package io.fand.api.enchantment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

public sealed interface EnchantmentEntityEffect extends EnchantmentLocationEffect
        permits EnchantmentEntityEffect.AllOf,
        EnchantmentEntityEffect.ApplyExhaustion,
        EnchantmentEntityEffect.ApplyImpulse,
        EnchantmentEntityEffect.ApplyMobEffect,
        EnchantmentEntityEffect.ChangeItemDamage,
        EnchantmentEntityEffect.DamageEntity,
        EnchantmentEntityEffect.Explode,
        EnchantmentEntityEffect.Ignite,
        EnchantmentEntityEffect.PlaySound,
        EnchantmentEntityEffect.Raw,
        EnchantmentEntityEffect.ReplaceBlock,
        EnchantmentEntityEffect.ReplaceDisk,
        EnchantmentEntityEffect.RunFunction,
        EnchantmentEntityEffect.SetBlockProperties,
        EnchantmentEntityEffect.SpawnParticles,
        EnchantmentEntityEffect.SummonEntity {

    static EnchantmentEntityEffect raw(JsonObject value) {
        return new Raw(value);
    }

    static EnchantmentEntityEffect applyExhaustion(EnchantmentLevelValue amount) {
        return new ApplyExhaustion(amount);
    }

    static EnchantmentEntityEffect allOf(List<EnchantmentEntityEffect> effects) {
        return new AllOf(effects);
    }

    static EnchantmentEntityEffect applyImpulse(Vec3d direction, Vec3d coordinateScale, EnchantmentLevelValue magnitude) {
        return new ApplyImpulse(direction, coordinateScale, magnitude);
    }

    static EnchantmentEntityEffect changeItemDamage(EnchantmentLevelValue amount) {
        return new ChangeItemDamage(amount);
    }

    static EnchantmentEntityEffect damageEntity(EnchantmentLevelValue minDamage, EnchantmentLevelValue maxDamage, Key damageType) {
        return new DamageEntity(minDamage, maxDamage, damageType);
    }

    static EnchantmentEntityEffect ignite(EnchantmentLevelValue duration) {
        return new Ignite(duration);
    }

    static EnchantmentEntityEffect runFunction(Key function) {
        return new RunFunction(function);
    }

    static EnchantmentEntityEffect summonEntity(List<io.fand.api.registry.RegistryReference> entityTypes, boolean joinTeam) {
        return new SummonEntity(entityTypes, joinTeam);
    }

    record AllOf(List<EnchantmentEntityEffect> effects) implements EnchantmentEntityEffect {
        public AllOf {
            effects = List.copyOf(effects);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:all_of");
            json.add("effects", EnchantmentJson.array(effects));
            return json;
        }
    }

    record ApplyExhaustion(EnchantmentLevelValue amount) implements EnchantmentEntityEffect {
        public ApplyExhaustion {
            amount = Objects.requireNonNull(amount, "amount");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:apply_exhaustion");
            json.add("amount", amount.toJson());
            return json;
        }
    }

    record ApplyImpulse(Vec3d direction, Vec3d coordinateScale, EnchantmentLevelValue magnitude) implements EnchantmentEntityEffect {
        public ApplyImpulse {
            direction = Objects.requireNonNull(direction, "direction");
            coordinateScale = Objects.requireNonNull(coordinateScale, "coordinateScale");
            magnitude = Objects.requireNonNull(magnitude, "magnitude");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:apply_impulse");
            json.add("direction", direction.toJson());
            json.add("coordinate_scale", coordinateScale.toJson());
            json.add("magnitude", magnitude.toJson());
            return json;
        }
    }

    record ApplyMobEffect(
            List<io.fand.api.registry.RegistryReference> toApply,
            EnchantmentLevelValue minDuration,
            EnchantmentLevelValue maxDuration,
            EnchantmentLevelValue minAmplifier,
            EnchantmentLevelValue maxAmplifier
    ) implements EnchantmentEntityEffect {

        public ApplyMobEffect {
            toApply = List.copyOf(toApply);
            minDuration = Objects.requireNonNull(minDuration, "minDuration");
            maxDuration = Objects.requireNonNull(maxDuration, "maxDuration");
            minAmplifier = Objects.requireNonNull(minAmplifier, "minAmplifier");
            maxAmplifier = Objects.requireNonNull(maxAmplifier, "maxAmplifier");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:apply_mob_effect");
            json.add("to_apply", registryReferences(toApply));
            json.add("min_duration", minDuration.toJson());
            json.add("max_duration", maxDuration.toJson());
            json.add("min_amplifier", minAmplifier.toJson());
            json.add("max_amplifier", maxAmplifier.toJson());
            return json;
        }
    }

    record ChangeItemDamage(EnchantmentLevelValue amount) implements EnchantmentEntityEffect {
        public ChangeItemDamage {
            amount = Objects.requireNonNull(amount, "amount");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:change_item_damage");
            json.add("amount", amount.toJson());
            return json;
        }
    }

    record DamageEntity(EnchantmentLevelValue minDamage, EnchantmentLevelValue maxDamage, Key damageType) implements EnchantmentEntityEffect {
        public DamageEntity {
            minDamage = Objects.requireNonNull(minDamage, "minDamage");
            maxDamage = Objects.requireNonNull(maxDamage, "maxDamage");
            damageType = Objects.requireNonNull(damageType, "damageType");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:damage_entity");
            json.add("min_damage", minDamage.toJson());
            json.add("max_damage", maxDamage.toJson());
            json.addProperty("damage_type", damageType.asString());
            return json;
        }
    }

    record Explode(JsonObject fields) implements EnchantmentEntityEffect {
        public Explode {
            fields = EnchantmentJson.rawObject(fields);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:explode");
            fields.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue().deepCopy()));
            return json;
        }
    }

    record Ignite(EnchantmentLevelValue duration) implements EnchantmentEntityEffect {
        public Ignite {
            duration = Objects.requireNonNull(duration, "duration");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:ignite");
            json.add("duration", duration.toJson());
            return json;
        }
    }

    record PlaySound(List<Key> sound, JsonObject volume, JsonObject pitch) implements EnchantmentEntityEffect {
        public PlaySound {
            sound = List.copyOf(sound);
            volume = EnchantmentJson.rawObject(volume);
            pitch = EnchantmentJson.rawObject(pitch);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:play_sound");
            json.add("sound", EnchantmentJson.keys(sound));
            json.add("volume", volume.deepCopy());
            json.add("pitch", pitch.deepCopy());
            return json;
        }
    }

    record ReplaceBlock(Vec3i offset, @Nullable JsonObject predicate, JsonObject blockState, @Nullable Key triggerGameEvent)
            implements EnchantmentEntityEffect {
        public ReplaceBlock {
            offset = Objects.requireNonNull(offset, "offset");
            predicate = predicate == null ? null : predicate.deepCopy();
            blockState = EnchantmentJson.rawObject(blockState);
        }

        public Optional<JsonObject> optionalPredicate() {
            return Optional.ofNullable(predicate == null ? null : predicate.deepCopy());
        }

        public Optional<Key> optionalTriggerGameEvent() {
            return Optional.ofNullable(triggerGameEvent);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:replace_block");
            json.add("offset", offset.toJson());
            if (predicate != null) {
                json.add("predicate", predicate.deepCopy());
            }
            json.add("block_state", blockState.deepCopy());
            if (triggerGameEvent != null) {
                json.addProperty("trigger_game_event", triggerGameEvent.asString());
            }
            return json;
        }
    }

    record ReplaceDisk(JsonObject fields) implements EnchantmentEntityEffect {
        public ReplaceDisk {
            fields = EnchantmentJson.rawObject(fields);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:replace_disk");
            fields.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue().deepCopy()));
            return json;
        }
    }

    record RunFunction(Key function) implements EnchantmentEntityEffect {
        public RunFunction {
            function = Objects.requireNonNull(function, "function");
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:run_function");
            json.addProperty("function", function.asString());
            return json;
        }
    }

    record SetBlockProperties(JsonObject properties, Vec3i offset, @Nullable Key triggerGameEvent) implements EnchantmentEntityEffect {
        public SetBlockProperties {
            properties = EnchantmentJson.rawObject(properties);
            offset = Objects.requireNonNull(offset, "offset");
        }

        public Optional<Key> optionalTriggerGameEvent() {
            return Optional.ofNullable(triggerGameEvent);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:set_block_properties");
            json.add("properties", properties.deepCopy());
            json.add("offset", offset.toJson());
            if (triggerGameEvent != null) {
                json.addProperty("trigger_game_event", triggerGameEvent.asString());
            }
            return json;
        }
    }

    record SpawnParticles(JsonObject fields) implements EnchantmentEntityEffect {
        public SpawnParticles {
            fields = EnchantmentJson.rawObject(fields);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:spawn_particles");
            fields.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue().deepCopy()));
            return json;
        }
    }

    record SummonEntity(List<io.fand.api.registry.RegistryReference> entity, boolean joinTeam) implements EnchantmentEntityEffect {
        public SummonEntity {
            entity = List.copyOf(entity);
        }

        @Override
        public JsonObject toJson() {
            var json = EnchantmentJson.object("minecraft:summon_entity");
            json.add("entity", registryReferences(entity));
            if (joinTeam) {
                json.addProperty("join_team", true);
            }
            return json;
        }
    }

    record Raw(JsonObject value) implements EnchantmentEntityEffect {
        public Raw {
            value = EnchantmentJson.rawObject(value);
        }

        @Override
        public JsonObject toJson() {
            return value.deepCopy();
        }
    }

    private static JsonElement registryReferences(List<io.fand.api.registry.RegistryReference> references) {
        if (references.size() == 1 && references.getFirst().tag()) {
            return new com.google.gson.JsonPrimitive(references.getFirst().asString());
        }
        var array = new com.google.gson.JsonArray();
        for (var reference : references) {
            array.add(reference.asString());
        }
        return array;
    }
}
