package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:blocks_attacks}. */
public final class ItemBlocksAttacks implements ItemComponentData {

    public static final ItemDamageFunction DEFAULT_ITEM_DAMAGE = new ItemDamageFunction(1.0F, 0.0F, 1.0F);
    public static final DamageReduction DEFAULT_DAMAGE_REDUCTION =
            new DamageReduction(90.0F, null, 0.0F, 1.0F);

    private final float blockDelaySeconds;
    private final float disableCooldownScale;
    private final List<ItemBlocksAttacks.DamageReduction> damageReductions;
    private final ItemDamageFunction itemDamage;
    private final @Nullable ItemKeySet bypassedBy;
    private final @Nullable Key blockSound;
    private final @Nullable Key disableSound;

    public ItemBlocksAttacks(
            float blockDelaySeconds,
            float disableCooldownScale,
            List<ItemBlocksAttacks.DamageReduction> damageReductions,
            ItemDamageFunction itemDamage,
            @Nullable ItemKeySet bypassedBy,
            @Nullable Key blockSound,
            @Nullable Key disableSound) {
        if (blockDelaySeconds < 0.0F) {
            throw new IllegalArgumentException("blockDelaySeconds must be >= 0");
        }
        if (disableCooldownScale < 0.0F) {
            throw new IllegalArgumentException("disableCooldownScale must be >= 0");
        }
        this.blockDelaySeconds = blockDelaySeconds;
        this.disableCooldownScale = disableCooldownScale;
        this.damageReductions = List.copyOf(Objects.requireNonNull(damageReductions, "damageReductions"));
        this.itemDamage = Objects.requireNonNull(itemDamage, "itemDamage");
        this.bypassedBy = bypassedBy;
        this.blockSound = blockSound;
        this.disableSound = disableSound;
    }

    public static ItemBlocksAttacks fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        var reductions = new java.util.ArrayList<DamageReduction>();
        var rawReductions = object.get("damage_reductions");
        if (rawReductions != null && rawReductions.isJsonArray()) {
            for (var reduction : rawReductions.getAsJsonArray()) {
                reductions.add(DamageReduction.fromJson(reduction));
            }
        }
        if (reductions.isEmpty()) {
            reductions.add(DEFAULT_DAMAGE_REDUCTION);
        }
        return new ItemBlocksAttacks(
                ItemComponentJson.floatOr(object, "block_delay_seconds", 0.0F),
                ItemComponentJson.floatOr(object, "disable_cooldown_scale", 1.0F),
                reductions,
                object.has("item_damage") ? ItemDamageFunction.fromJson(object.get("item_damage")) : DEFAULT_ITEM_DAMAGE,
                object.has("bypassed_by") ? ItemKeySet.fromJson(object.get("bypassed_by")) : null,
                ItemComponentJson.optionalKey(object, "block_sound").orElse(null),
                ItemComponentJson.optionalKey(object, "disabled_sound").orElse(null));
    }

    public float blockDelaySeconds() {
        return blockDelaySeconds;
    }

    public float disableCooldownScale() {
        return disableCooldownScale;
    }

    public List<DamageReduction> damageReductions() {
        return damageReductions;
    }

    public ItemDamageFunction itemDamage() {
        return itemDamage;
    }

    public Optional<ItemKeySet> bypassedBy() {
        return Optional.ofNullable(bypassedBy);
    }

    public Optional<Key> blockSound() {
        return Optional.ofNullable(blockSound);
    }

    public Optional<Key> disableSound() {
        return Optional.ofNullable(disableSound);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (blockDelaySeconds != 0.0F) {
            json.addProperty("block_delay_seconds", blockDelaySeconds);
        }
        if (disableCooldownScale != 1.0F) {
            json.addProperty("disable_cooldown_scale", disableCooldownScale);
        }
        if (!damageReductions.equals(List.of(DEFAULT_DAMAGE_REDUCTION))) {
            var reductions = new JsonArray();
            damageReductions.forEach(reduction -> reductions.add(reduction.toJson()));
            json.add("damage_reductions", reductions);
        }
        if (!itemDamage.equals(DEFAULT_ITEM_DAMAGE)) {
            json.add("item_damage", itemDamage.toJson());
        }
        if (bypassedBy != null) {
            json.add("bypassed_by", bypassedBy.toJson());
        }
        if (blockSound != null) {
            json.addProperty("block_sound", blockSound.asString());
        }
        if (disableSound != null) {
            json.addProperty("disabled_sound", disableSound.asString());
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemBlocksAttacks that)) {
            return false;
        }
        return Float.compare(blockDelaySeconds, that.blockDelaySeconds) == 0
                && Float.compare(disableCooldownScale, that.disableCooldownScale) == 0
                && damageReductions.equals(that.damageReductions)
                && itemDamage.equals(that.itemDamage)
                && Objects.equals(bypassedBy, that.bypassedBy)
                && Objects.equals(blockSound, that.blockSound)
                && Objects.equals(disableSound, that.disableSound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                blockDelaySeconds,
                disableCooldownScale,
                damageReductions,
                itemDamage,
                bypassedBy,
                blockSound,
                disableSound);
    }

    @Override
    public String toString() {
        return "ItemBlocksAttacks[blockDelaySeconds=" + blockDelaySeconds
                + ", disableCooldownScale=" + disableCooldownScale
                + ", damageReductions=" + damageReductions
                + ", itemDamage=" + itemDamage
                + ", bypassedBy=" + bypassedBy()
                + ", blockSound=" + blockSound()
                + ", disableSound=" + disableSound()
                + "]";
    }

    public static final class DamageReduction implements ItemComponentData {

        private final float horizontalBlockingAngle;
        private final @Nullable ItemKeySet type;
        private final float base;
        private final float factor;

        public DamageReduction(float horizontalBlockingAngle, @Nullable ItemKeySet type, float base, float factor) {
            if (horizontalBlockingAngle <= 0.0F) {
                throw new IllegalArgumentException("horizontalBlockingAngle must be > 0");
            }
            this.horizontalBlockingAngle = horizontalBlockingAngle;
            this.type = type;
            this.base = base;
            this.factor = factor;
        }

        public static DamageReduction fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "damage reduction");
            return new DamageReduction(
                    ItemComponentJson.floatOr(object, "horizontal_blocking_angle", 90.0F),
                    object.has("type") ? ItemKeySet.fromJson(object.get("type")) : null,
                    object.get("base").getAsFloat(),
                    object.get("factor").getAsFloat());
        }

        public float horizontalBlockingAngle() {
            return horizontalBlockingAngle;
        }

        public Optional<ItemKeySet> type() {
            return Optional.ofNullable(type);
        }

        public float base() {
            return base;
        }

        public float factor() {
            return factor;
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            if (horizontalBlockingAngle != 90.0F) {
                json.addProperty("horizontal_blocking_angle", horizontalBlockingAngle);
            }
            if (type != null) {
                json.add("type", type.toJson());
            }
            json.addProperty("base", base);
            json.addProperty("factor", factor);
            return json;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DamageReduction that)) {
                return false;
            }
            return Float.compare(horizontalBlockingAngle, that.horizontalBlockingAngle) == 0
                    && Float.compare(base, that.base) == 0
                    && Float.compare(factor, that.factor) == 0
                    && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(horizontalBlockingAngle, type, base, factor);
        }

        @Override
        public String toString() {
            return "DamageReduction[horizontalBlockingAngle=" + horizontalBlockingAngle
                    + ", type=" + type()
                    + ", base=" + base
                    + ", factor=" + factor
                    + "]";
        }
    }

    public record ItemDamageFunction(float threshold, float base, float factor) implements ItemComponentData {

        public ItemDamageFunction {
            if (threshold < 0.0F) {
                throw new IllegalArgumentException("threshold must be >= 0");
            }
        }

        public static ItemDamageFunction fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "item damage function");
            return new ItemDamageFunction(
                    object.get("threshold").getAsFloat(),
                    object.get("base").getAsFloat(),
                    object.get("factor").getAsFloat());
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("threshold", threshold);
            json.addProperty("base", base);
            json.addProperty("factor", factor);
            return json;
        }
    }
}
