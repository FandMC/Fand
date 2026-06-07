package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.world.sound.SoundKey;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed consume-effect value used by consumable and death-protection components. */
public sealed interface ItemConsumeEffect extends ItemComponentData permits
        ItemConsumeEffect.ApplyEffects,
        ItemConsumeEffect.RemoveEffects,
        ItemConsumeEffect.ClearAllEffects,
        ItemConsumeEffect.TeleportRandomly,
        ItemConsumeEffect.PlaySound {

    Key APPLY_EFFECTS = Key.key("minecraft:apply_effects");
    Key REMOVE_EFFECTS = Key.key("minecraft:remove_effects");
    Key CLEAR_ALL_EFFECTS = Key.key("minecraft:clear_all_effects");
    Key TELEPORT_RANDOMLY = Key.key("minecraft:teleport_randomly");
    Key PLAY_SOUND = Key.key("minecraft:play_sound");

    Key type();

    static ItemConsumeEffect fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "consume effect");
        var type = keyOrMinecraft(object.get("type").getAsString());
        if (type.equals(APPLY_EFFECTS)) {
            return ApplyEffects.fromObject(object);
        }
        if (type.equals(REMOVE_EFFECTS)) {
            return RemoveEffects.fromObject(object);
        }
        if (type.equals(CLEAR_ALL_EFFECTS)) {
            return new ClearAllEffects();
        }
        if (type.equals(TELEPORT_RANDOMLY)) {
            return TeleportRandomly.fromObject(object);
        }
        if (type.equals(PLAY_SOUND)) {
            return PlaySound.fromObject(object);
        }
        throw new IllegalArgumentException("Unsupported consume effect type: " + type.asString());
    }

    private static Key keyOrMinecraft(String value) {
        return Key.key(value.contains(":") ? value : "minecraft:" + value);
    }

    record ApplyEffects(List<ItemEffectInstance> effects, float probability) implements ItemConsumeEffect {

        public ApplyEffects {
            effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
            if (probability < 0.0F || probability > 1.0F) {
                throw new IllegalArgumentException("probability must be in 0.0..1.0");
            }
        }

        public ApplyEffects(List<ItemEffectInstance> effects) {
            this(effects, 1.0F);
        }

        private static ApplyEffects fromObject(JsonObject object) {
            var effects = new java.util.ArrayList<ItemEffectInstance>();
            var rawEffects = object.get("effects");
            if (rawEffects != null && rawEffects.isJsonArray()) {
                for (var effect : rawEffects.getAsJsonArray()) {
                    effects.add(ItemEffectInstance.fromJson(effect));
                }
            }
            return new ApplyEffects(effects, ItemComponentJson.floatOr(object, "probability", 1.0F));
        }

        @Override
        public Key type() {
            return APPLY_EFFECTS;
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", type().asString());
            var effectArray = new JsonArray();
            effects.forEach(effect -> effectArray.add(effect.toJson()));
            json.add("effects", effectArray);
            if (probability != 1.0F) {
                json.addProperty("probability", probability);
            }
            return json;
        }
    }

    record RemoveEffects(ItemKeySet effects) implements ItemConsumeEffect {

        public RemoveEffects {
            effects = Objects.requireNonNull(effects, "effects");
        }

        private static RemoveEffects fromObject(JsonObject object) {
            return new RemoveEffects(ItemKeySet.fromJson(object.get("effects")));
        }

        @Override
        public Key type() {
            return REMOVE_EFFECTS;
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", type().asString());
            json.add("effects", effects.toJson());
            return json;
        }
    }

    record ClearAllEffects() implements ItemConsumeEffect {

        @Override
        public Key type() {
            return CLEAR_ALL_EFFECTS;
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", type().asString());
            return json;
        }
    }

    record TeleportRandomly(float diameter) implements ItemConsumeEffect {

        public TeleportRandomly {
            if (diameter <= 0.0F) {
                throw new IllegalArgumentException("diameter must be > 0");
            }
        }

        public TeleportRandomly() {
            this(16.0F);
        }

        private static TeleportRandomly fromObject(JsonObject object) {
            return new TeleportRandomly(ItemComponentJson.floatOr(object, "diameter", 16.0F));
        }

        @Override
        public Key type() {
            return TELEPORT_RANDOMLY;
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", type().asString());
            if (diameter != 16.0F) {
                json.addProperty("diameter", diameter);
            }
            return json;
        }
    }

    record PlaySound(Key sound) implements ItemConsumeEffect {

        public PlaySound {
            sound = Objects.requireNonNull(sound, "sound");
        }

        public PlaySound(SoundKey sound) {
            this(Objects.requireNonNull(sound, "sound").key());
        }

        private static PlaySound fromObject(JsonObject object) {
            return new PlaySound(ItemComponentJson.key(object, "sound"));
        }

        @Override
        public Key type() {
            return PLAY_SOUND;
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", type().asString());
            json.addProperty("sound", sound.asString());
            return json;
        }
    }
}
