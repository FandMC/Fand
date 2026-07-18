package io.fand.server.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fand.api.item.custom.CustomItemRegistration;
import io.fand.api.item.custom.CustomItemRegistry;
import io.fand.api.item.custom.CustomItemType;
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
        if (stack.type() instanceof CustomItemType customType) {
            return Optional.of(customType.id());
        }
        return taggedId(stack);
    }

    public ItemStack encode(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty() || !(stack.type() instanceof CustomItemType customType)) {
            return stack;
        }
        var registered = type(customType.id())
                .filter(customType::equals)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Custom item is not registered: " + customType.id().asString()));
        var physical = new ItemStack(registered.baseType(), stack.amount(), stack.components());
        return addMarker(physical, registered.id());
    }

    public ItemStack decode(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty() || stack.type() instanceof CustomItemType) {
            return stack;
        }
        var customType = taggedId(stack)
                .flatMap(this::type)
                .filter(type -> type.baseType().equals(stack.type()))
                .orElse(null);
        if (customType == null) {
            return stack;
        }
        var unmarked = removeMarker(stack);
        return new ItemStack(customType, unmarked.amount(), unmarked.componentPatch());
    }

    private static Optional<Key> taggedId(ItemStack stack) {
        return stack.customData()
                .filter(data -> data.has(CUSTOM_ITEM_KEY) && data.get(CUSTOM_ITEM_KEY).isJsonPrimitive())
                .map(data -> Key.key(data.get(CUSTOM_ITEM_KEY).getAsString()));
    }

    @Override
    public ItemStack create(Key id, int amount) {
        var type = type(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom item: " + id.asString()));
        return type.stack(amount);
    }

    @Override
    public ItemStack tag(ItemStack stack, Key id) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(id, "id");
        if (stack.empty()) {
            return ItemStack.EMPTY;
        }
        var type = type(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom item: " + id.asString()));
        if (!type.baseType().equals(stack.type())) {
            throw new IllegalArgumentException("Stack base type must be " + type.baseType().key().asString());
        }
        return new ItemStack(type, stack.amount(), stack.componentPatch());
    }

    @Override
    public ItemStack untag(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty()) {
            return ItemStack.EMPTY;
        }
        if (stack.type() instanceof CustomItemType customType) {
            var registered = type(customType.id()).filter(customType::equals).orElse(null);
            if (registered == null) {
                return stack;
            }
            return removeMarker(new ItemStack(registered.baseType(), stack.amount(), stack.components()));
        }
        return removeMarker(stack);
    }

    private static ItemStack addMarker(ItemStack stack, Key id) {
        JsonObject data = stack.customData().orElseGet(JsonObject::new);
        data.add(CUSTOM_ITEM_KEY, new JsonPrimitive(id.asString()));
        return stack.withCustomData(data);
    }

    private static ItemStack removeMarker(ItemStack stack) {
        var data = stack.customData();
        if (data.isEmpty() || !data.get().has(CUSTOM_ITEM_KEY)) {
            return stack;
        }
        JsonObject next = data.get();
        next.remove(CUSTOM_ITEM_KEY);
        return next.isEmpty() ? stack.removeComponent(io.fand.api.item.component.ItemComponentKeys.CUSTOM_DATA)
                : stack.withCustomData(next);
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
        public CustomItemType type() {
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
