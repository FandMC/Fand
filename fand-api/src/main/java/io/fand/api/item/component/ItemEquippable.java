package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:equippable}. */
public record ItemEquippable(
        ItemEquipmentSlot slot,
        Key equipSound,
        Optional<Key> assetId,
        Optional<Key> cameraOverlay,
        Optional<ItemKeySet> allowedEntities,
        boolean dispensable,
        boolean swappable,
        boolean damageOnHurt,
        boolean equipOnInteract,
        boolean canBeSheared,
        Key shearingSound) implements ItemComponentData {

    public static final Key DEFAULT_EQUIP_SOUND = Key.key("minecraft:item.armor.equip_generic");
    public static final Key DEFAULT_SHEARING_SOUND = Key.key("minecraft:item.shears.snip");

    public ItemEquippable {
        slot = Objects.requireNonNull(slot, "slot");
        equipSound = Objects.requireNonNull(equipSound, "equipSound");
        assetId = Objects.requireNonNull(assetId, "assetId");
        cameraOverlay = Objects.requireNonNull(cameraOverlay, "cameraOverlay");
        allowedEntities = Objects.requireNonNull(allowedEntities, "allowedEntities");
        shearingSound = Objects.requireNonNull(shearingSound, "shearingSound");
    }

    public ItemEquippable(ItemEquipmentSlot slot) {
        this(slot, DEFAULT_EQUIP_SOUND, Optional.empty(), Optional.empty(), Optional.empty(), true, true, true, false, false, DEFAULT_SHEARING_SOUND);
    }

    public static ItemEquippable fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "equippable");
        return new ItemEquippable(
                ItemEquipmentSlot.fromSerializedName(object.get("slot").getAsString()),
                ItemComponentJson.optionalKey(object, "equip_sound").orElse(DEFAULT_EQUIP_SOUND),
                ItemComponentJson.optionalKey(object, "asset_id"),
                ItemComponentJson.optionalKey(object, "camera_overlay"),
                object.has("allowed_entities")
                        ? Optional.of(ItemKeySet.fromJson(object.get("allowed_entities")))
                        : Optional.empty(),
                ItemComponentJson.booleanOr(object, "dispensable", true),
                ItemComponentJson.booleanOr(object, "swappable", true),
                ItemComponentJson.booleanOr(object, "damage_on_hurt", true),
                ItemComponentJson.booleanOr(object, "equip_on_interact", false),
                ItemComponentJson.booleanOr(object, "can_be_sheared", false),
                ItemComponentJson.optionalKey(object, "shearing_sound").orElse(DEFAULT_SHEARING_SOUND));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("slot", slot.serializedName());
        if (!equipSound.equals(DEFAULT_EQUIP_SOUND)) {
            json.addProperty("equip_sound", equipSound.asString());
        }
        assetId.ifPresent(value -> json.addProperty("asset_id", value.asString()));
        cameraOverlay.ifPresent(value -> json.addProperty("camera_overlay", value.asString()));
        allowedEntities.ifPresent(value -> json.add("allowed_entities", value.toJson()));
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
}
