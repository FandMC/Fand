package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.entity.EntityKey;
import io.fand.api.world.sound.SoundKey;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:equippable}. */
public final class ItemEquippable implements ItemComponentData {

    public static final Key DEFAULT_EQUIP_SOUND = Key.key("minecraft:item.armor.equip_generic");
    public static final Key DEFAULT_SHEARING_SOUND = Key.key("minecraft:item.shears.snip");

    private final ItemEquipmentSlot slot;
    private final Key equipSound;
    private final @Nullable Key assetId;
    private final @Nullable Key cameraOverlay;
    private final @Nullable ItemKeySet allowedEntities;
    private final boolean dispensable;
    private final boolean swappable;
    private final boolean damageOnHurt;
    private final boolean equipOnInteract;
    private final boolean canBeSheared;
    private final Key shearingSound;

    public ItemEquippable(
            ItemEquipmentSlot slot,
            Key equipSound,
            @Nullable Key assetId,
            @Nullable Key cameraOverlay,
            @Nullable ItemKeySet allowedEntities,
            boolean dispensable,
            boolean swappable,
            boolean damageOnHurt,
            boolean equipOnInteract,
            boolean canBeSheared,
            Key shearingSound) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.equipSound = Objects.requireNonNull(equipSound, "equipSound");
        this.assetId = assetId;
        this.cameraOverlay = cameraOverlay;
        this.allowedEntities = allowedEntities;
        this.dispensable = dispensable;
        this.swappable = swappable;
        this.damageOnHurt = damageOnHurt;
        this.equipOnInteract = equipOnInteract;
        this.canBeSheared = canBeSheared;
        this.shearingSound = Objects.requireNonNull(shearingSound, "shearingSound");
    }

    public ItemEquippable(ItemEquipmentSlot slot) {
        this(slot, DEFAULT_EQUIP_SOUND, null, null, null, true, true, true, false, false, DEFAULT_SHEARING_SOUND);
    }

    public ItemEquippable(ItemEquipmentSlot slot, SoundKey equipSound) {
        this(
                slot,
                Objects.requireNonNull(equipSound, "equipSound").key(),
                null,
                null,
                null,
                true,
                true,
                true,
                false,
                false,
                DEFAULT_SHEARING_SOUND);
    }

    public ItemEquippable withEquipSound(SoundKey equipSound) {
        return new ItemEquippable(
                slot,
                Objects.requireNonNull(equipSound, "equipSound").key(),
                assetId,
                cameraOverlay,
                allowedEntities,
                dispensable,
                swappable,
                damageOnHurt,
                equipOnInteract,
                canBeSheared,
                shearingSound);
    }

    public ItemEquippable withAssetId(EquipmentAssetKey assetId) {
        return new ItemEquippable(
                slot,
                equipSound,
                Objects.requireNonNull(assetId, "assetId").key(),
                cameraOverlay,
                allowedEntities,
                dispensable,
                swappable,
                damageOnHurt,
                equipOnInteract,
                canBeSheared,
                shearingSound);
    }

    public ItemEquippable withoutAssetId() {
        return new ItemEquippable(
                slot,
                equipSound,
                null,
                cameraOverlay,
                allowedEntities,
                dispensable,
                swappable,
                damageOnHurt,
                equipOnInteract,
                canBeSheared,
                shearingSound);
    }

    public ItemEquippable withAllowedEntities(EntityKey first, EntityKey... rest) {
        return new ItemEquippable(
                slot,
                equipSound,
                assetId,
                cameraOverlay,
                ItemKeySet.of(first, rest),
                dispensable,
                swappable,
                damageOnHurt,
                equipOnInteract,
                canBeSheared,
                shearingSound);
    }

    public ItemEquippable withoutAllowedEntities() {
        return new ItemEquippable(
                slot,
                equipSound,
                assetId,
                cameraOverlay,
                null,
                dispensable,
                swappable,
                damageOnHurt,
                equipOnInteract,
                canBeSheared,
                shearingSound);
    }

    public ItemEquippable withShearingSound(SoundKey shearingSound) {
        return new ItemEquippable(
                slot,
                equipSound,
                assetId,
                cameraOverlay,
                allowedEntities,
                dispensable,
                swappable,
                damageOnHurt,
                equipOnInteract,
                canBeSheared,
                Objects.requireNonNull(shearingSound, "shearingSound").key());
    }

    public static ItemEquippable fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "equippable");
        return new ItemEquippable(
                ItemEquipmentSlot.fromSerializedName(object.get("slot").getAsString()),
                ItemComponentJson.optionalKey(object, "equip_sound").orElse(DEFAULT_EQUIP_SOUND),
                ItemComponentJson.optionalKey(object, "asset_id").orElse(null),
                ItemComponentJson.optionalKey(object, "camera_overlay").orElse(null),
                object.has("allowed_entities") ? ItemKeySet.fromJson(object.get("allowed_entities")) : null,
                ItemComponentJson.booleanOr(object, "dispensable", true),
                ItemComponentJson.booleanOr(object, "swappable", true),
                ItemComponentJson.booleanOr(object, "damage_on_hurt", true),
                ItemComponentJson.booleanOr(object, "equip_on_interact", false),
                ItemComponentJson.booleanOr(object, "can_be_sheared", false),
                ItemComponentJson.optionalKey(object, "shearing_sound").orElse(DEFAULT_SHEARING_SOUND));
    }

    public ItemEquipmentSlot slot() {
        return slot;
    }

    public Key equipSound() {
        return equipSound;
    }

    public Optional<Key> assetId() {
        return Optional.ofNullable(assetId);
    }

    public Optional<Key> cameraOverlay() {
        return Optional.ofNullable(cameraOverlay);
    }

    public Optional<ItemKeySet> allowedEntities() {
        return Optional.ofNullable(allowedEntities);
    }

    public boolean dispensable() {
        return dispensable;
    }

    public boolean swappable() {
        return swappable;
    }

    public boolean damageOnHurt() {
        return damageOnHurt;
    }

    public boolean equipOnInteract() {
        return equipOnInteract;
    }

    public boolean canBeSheared() {
        return canBeSheared;
    }

    public Key shearingSound() {
        return shearingSound;
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("slot", slot.serializedName());
        if (!equipSound.equals(DEFAULT_EQUIP_SOUND)) {
            json.addProperty("equip_sound", equipSound.asString());
        }
        if (assetId != null) {
            json.addProperty("asset_id", assetId.asString());
        }
        if (cameraOverlay != null) {
            json.addProperty("camera_overlay", cameraOverlay.asString());
        }
        if (allowedEntities != null) {
            json.add("allowed_entities", allowedEntities.toJson());
        }
        if (!dispensable) {
            json.addProperty("dispensable", false);
        }
        if (!swappable) {
            json.addProperty("swappable", false);
        }
        if (!damageOnHurt) {
            json.addProperty("damage_on_hurt", false);
        }
        if (equipOnInteract) {
            json.addProperty("equip_on_interact", true);
        }
        if (canBeSheared) {
            json.addProperty("can_be_sheared", true);
        }
        if (!shearingSound.equals(DEFAULT_SHEARING_SOUND)) {
            json.addProperty("shearing_sound", shearingSound.asString());
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemEquippable that)) {
            return false;
        }
        return dispensable == that.dispensable
                && swappable == that.swappable
                && damageOnHurt == that.damageOnHurt
                && equipOnInteract == that.equipOnInteract
                && canBeSheared == that.canBeSheared
                && slot == that.slot
                && equipSound.equals(that.equipSound)
                && Objects.equals(assetId, that.assetId)
                && Objects.equals(cameraOverlay, that.cameraOverlay)
                && Objects.equals(allowedEntities, that.allowedEntities)
                && shearingSound.equals(that.shearingSound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                slot,
                equipSound,
                assetId,
                cameraOverlay,
                allowedEntities,
                dispensable,
                swappable,
                damageOnHurt,
                equipOnInteract,
                canBeSheared,
                shearingSound);
    }

    @Override
    public String toString() {
        return "ItemEquippable[slot=" + slot
                + ", equipSound=" + equipSound
                + ", assetId=" + assetId()
                + ", cameraOverlay=" + cameraOverlay()
                + ", allowedEntities=" + allowedEntities()
                + ", dispensable=" + dispensable
                + ", swappable=" + swappable
                + ", damageOnHurt=" + damageOnHurt
                + ", equipOnInteract=" + equipOnInteract
                + ", canBeSheared=" + canBeSheared
                + ", shearingSound=" + shearingSound
                + "]";
    }
}
