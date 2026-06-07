package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Typed value for the vanilla {@code minecraft:tooltip_display} component.
 */
public record ItemTooltipDisplay(boolean hideTooltip, Set<Key> hiddenComponents) {

    public static final ItemTooltipDisplay DEFAULT = new ItemTooltipDisplay(false, Set.of());

    public ItemTooltipDisplay {
        var copied = new LinkedHashSet<Key>();
        for (var key : Objects.requireNonNull(hiddenComponents, "hiddenComponents")) {
            copied.add(Objects.requireNonNull(key, "hidden component key"));
        }
        hiddenComponents = Collections.unmodifiableSet(copied);
    }

    public static ItemTooltipDisplay showing() {
        return DEFAULT;
    }

    public static ItemTooltipDisplay hidden() {
        return new ItemTooltipDisplay(true, Set.of());
    }

    public boolean hides(Key component) {
        return hideTooltip || hiddenComponents.contains(component);
    }

    public ItemTooltipDisplay withHiddenTooltip(boolean hidden) {
        return hidden == hideTooltip ? this : new ItemTooltipDisplay(hidden, hiddenComponents);
    }

    public ItemTooltipDisplay withHiddenComponent(Key component, boolean hidden) {
        Objects.requireNonNull(component, "component");
        if (hiddenComponents.contains(component) == hidden) {
            return this;
        }
        var next = new LinkedHashSet<>(hiddenComponents);
        if (hidden) {
            next.add(component);
        } else {
            next.remove(component);
        }
        return new ItemTooltipDisplay(hideTooltip, next);
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        if (hideTooltip) {
            json.addProperty("hide_tooltip", true);
        }
        if (!hiddenComponents.isEmpty()) {
            var hidden = new JsonArray();
            hiddenComponents.forEach(key -> hidden.add(key.asString()));
            json.add("hidden_components", hidden);
        }
        return json;
    }

    public static ItemTooltipDisplay fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return DEFAULT;
        }
        var object = value.getAsJsonObject();
        boolean hideTooltip = object.has("hide_tooltip") && object.get("hide_tooltip").getAsBoolean();
        var hiddenComponents = new LinkedHashSet<Key>();
        var hidden = object.get("hidden_components");
        if (hidden != null && hidden.isJsonArray()) {
            for (var element : hidden.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    hiddenComponents.add(Key.key(element.getAsString()));
                }
            }
        }
        return new ItemTooltipDisplay(hideTooltip, hiddenComponents);
    }
}
