package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Typed block predicate entry used by adventure-mode item components. */
public final class ItemBlockPredicate implements ItemComponentData {

    private final @Nullable ItemKeySet blocks;
    private final @Nullable JsonObject state;
    private final @Nullable JsonObject nbt;
    private final @Nullable JsonObject components;

    public ItemBlockPredicate(
            @Nullable ItemKeySet blocks,
            @Nullable JsonObject state,
            @Nullable JsonObject nbt,
            @Nullable JsonObject components) {
        this.blocks = blocks;
        this.state = copy(state);
        this.nbt = copy(nbt);
        this.components = copy(components);
    }

    public static ItemBlockPredicate blocks(ItemKeySet blocks) {
        return new ItemBlockPredicate(Objects.requireNonNull(blocks, "blocks"), null, null, null);
    }

    public static ItemBlockPredicate fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "block predicate");
        return new ItemBlockPredicate(
                object.has("blocks") ? ItemKeySet.fromJson(object.get("blocks")) : null,
                ItemComponentJson.optionalObject(object, "state").orElse(null),
                ItemComponentJson.optionalObject(object, "nbt").orElse(null),
                ItemComponentJson.optionalObject(object, "components").orElse(null));
    }

    public Optional<ItemKeySet> blocks() {
        return Optional.ofNullable(blocks);
    }

    public Optional<JsonObject> state() {
        return Optional.ofNullable(copy(state));
    }

    public Optional<JsonObject> nbt() {
        return Optional.ofNullable(copy(nbt));
    }

    public Optional<JsonObject> components() {
        return Optional.ofNullable(copy(components));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (blocks != null) {
            json.add("blocks", blocks.toJson());
        }
        if (state != null) {
            json.add("state", state.deepCopy());
        }
        if (nbt != null) {
            json.add("nbt", nbt.deepCopy());
        }
        if (components != null) {
            json.add("components", components.deepCopy());
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemBlockPredicate that)) {
            return false;
        }
        return Objects.equals(blocks, that.blocks)
                && Objects.equals(state, that.state)
                && Objects.equals(nbt, that.nbt)
                && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks, state, nbt, components);
    }

    @Override
    public String toString() {
        return "ItemBlockPredicate[blocks=" + blocks()
                + ", state=" + state()
                + ", nbt=" + nbt()
                + ", components=" + components()
                + "]";
    }

    private static @Nullable JsonObject copy(@Nullable JsonObject value) {
        return value == null ? null : value.deepCopy();
    }
}
