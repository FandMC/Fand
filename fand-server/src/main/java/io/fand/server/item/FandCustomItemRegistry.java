package io.fand.server.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fand.api.customitem.CustomItemRegistration;
import io.fand.api.customitem.CustomItemRegistry;
import io.fand.api.customitem.CustomItemType;
import io.fand.api.item.ItemStack;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;

public final class FandCustomItemRegistry implements CustomItemRegistry {

    public static final String CUSTOM_ITEM_KEY = "fand:custom_item";

    private final ConcurrentHashMap<Key, RegisteredCustomItem> types = new ConcurrentHashMap<>();

    @Override
    public CustomItemRegistration register(CustomItemType type) {
        Objects.requireNonNull(type, "type");
        var registered = new RegisteredCustomItem(this, type);
        var previous = types.putIfAbsent(type.id(), registered);
        if (previous != null) {
            throw new IllegalArgumentException("Custom item already registered: " + type.id().asString());
        }
        return registered;
    }

    @Override
    public Optional<CustomItemType> type(Key id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(types.get(id)).filter(RegisteredCustomItem::active).map(RegisteredCustomItem::type);
    }

    @Override
    public Collection<CustomItemType> types() {
        return types.values().stream()
                .filter(RegisteredCustomItem::active)
                .map(RegisteredCustomItem::type)
                .sorted(java.util.Comparator.comparing(type -> type.id().asString()))
                .toList();
    }

    @Override
    public Optional<CustomItemType> customItem(ItemStack stack) {
        return customId(stack).flatMap(this::type);
    }

    @Override
    public Optional<Key> customId(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty()) {
            return Optional.empty();
        }
        return stack.customData()
                .filter(data -> data.has(CUSTOM_ITEM_KEY) && data.get(CUSTOM_ITEM_KEY).isJsonPrimitive())
                .map(data -> Key.key(data.get(CUSTOM_ITEM_KEY).getAsString()));
    }

    @Override
    public ItemStack create(Key id, int amount) {
        var type = type(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom item: " + id.asString()));
        return tag(type.template().withAmount(amount), type.id());
    }

    @Override
    public ItemStack tag(ItemStack stack, Key id) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(id, "id");
        if (stack.empty()) {
            return ItemStack.EMPTY;
        }
        JsonObject data = stack.customData().orElseGet(JsonObject::new);
        data.add(CUSTOM_ITEM_KEY, new JsonPrimitive(id.asString()));
        return stack.withCustomData(data);
    }

    @Override
    public ItemStack untag(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty()) {
            return ItemStack.EMPTY;
        }
        var data = stack.customData();
        if (data.isEmpty() || !data.get().has(CUSTOM_ITEM_KEY)) {
            return stack;
        }
        JsonObject next = data.get();
        next.remove(CUSTOM_ITEM_KEY);
        return next.isEmpty() ? stack.withoutCustomData() : stack.withCustomData(next);
    }

    private void unregister(RegisteredCustomItem registration) {
        types.remove(registration.id(), registration);
    }

    private static final class RegisteredCustomItem implements CustomItemRegistration {

        private final FandCustomItemRegistry owner;
        private final CustomItemType type;
        private volatile boolean active = true;

        private RegisteredCustomItem(FandCustomItemRegistry owner, CustomItemType type) {
            this.owner = owner;
            this.type = type;
        }

        @Override
        public Key id() {
            return type.id();
        }

        private CustomItemType type() {
            return type;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.unregister(this);
            }
        }
    }
}
