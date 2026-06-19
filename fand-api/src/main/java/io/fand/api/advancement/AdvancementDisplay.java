package io.fand.api.advancement;

import com.google.gson.JsonObject;
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jspecify.annotations.Nullable;

public record AdvancementDisplay(
        ItemStack icon,
        Component title,
        Component description,
        Optional<Key> background,
        AdvancementFrame frame,
        boolean showToast,
        boolean announceChat,
        boolean hidden,
        float x,
        float y
) {

    private static final ItemType DEFAULT_ICON = new ItemType() {
        @Override
        public Key key() {
            return ItemKey.PAPER.key();
        }

        @Override
        public int maxStackSize() {
            return 64;
        }
    };

    public AdvancementDisplay {
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        background = Objects.requireNonNull(background, "background");
        Objects.requireNonNull(frame, "frame");
    }

    public AdvancementDisplay(
            ItemStack icon,
            Component title,
            Component description,
            @Nullable Key background,
            AdvancementFrame frame,
            boolean showToast,
            boolean announceChat,
            boolean hidden
    ) {
        this(icon, title, description, Optional.ofNullable(background), frame, showToast, announceChat, hidden, 0.0F, 0.0F);
    }

    public static AdvancementDisplay task(Component title, Component description) {
        return new AdvancementDisplay(
                DEFAULT_ICON.one(),
                title,
                description,
                Optional.empty(),
                AdvancementFrame.TASK,
                true,
                true,
                false,
                0.0F,
                0.0F);
    }

    public JsonObject toVanillaJson() {
        var json = new JsonObject();
        json.add("icon", iconJson(icon));
        json.add("title", GsonComponentSerializer.gson().serializeToTree(title));
        json.add("description", GsonComponentSerializer.gson().serializeToTree(description));
        background.ifPresent(key -> json.addProperty("background", key.asString()));
        json.addProperty("frame", frame.serializedName());
        json.addProperty("show_toast", showToast);
        json.addProperty("announce_to_chat", announceChat);
        json.addProperty("hidden", hidden);
        return json;
    }

    private static JsonObject iconJson(ItemStack icon) {
        if (icon.isEmpty()) {
            throw new IllegalArgumentException("advancement display icon must not be empty");
        }
        var json = new JsonObject();
        json.addProperty("id", icon.type().key().asString());
        if (icon.amount() != 1) {
            json.addProperty("count", icon.amount());
        }
        if (!icon.components().isEmpty()) {
            json.add("components", icon.components().toJsonPatch());
        }
        return json;
    }
}
