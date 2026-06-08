package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:lodestone_tracker}. */
public final class ItemLodestoneTracker implements ItemComponentData {

    private final @Nullable ItemGlobalPosition target;
    private final boolean tracked;

    public ItemLodestoneTracker(@Nullable ItemGlobalPosition target, boolean tracked) {
        this.target = target;
        this.tracked = tracked;
    }

    public static ItemLodestoneTracker fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        return new ItemLodestoneTracker(
                object.has("target") ? ItemGlobalPosition.fromJson(object.get("target")) : null,
                ItemComponentJson.booleanOr(object, "tracked", true));
    }

    public Optional<ItemGlobalPosition> target() {
        return Optional.ofNullable(target);
    }

    public boolean tracked() {
        return tracked;
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (target != null) {
            json.add("target", target.toJson());
        }
        if (!tracked) {
            json.addProperty("tracked", false);
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemLodestoneTracker that)) {
            return false;
        }
        return tracked == that.tracked && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, tracked);
    }

    @Override
    public String toString() {
        return "ItemLodestoneTracker[target=" + target() + ", tracked=" + tracked + "]";
    }
}
