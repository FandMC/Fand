package io.fand.server.entity;

import io.fand.api.entity.EntityEffect;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

final class EntityEffects {

    private EntityEffects() {
    }

    static Optional<Holder.Reference<MobEffect>> holder(Key key) {
        var id = Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        return BuiltInRegistries.MOB_EFFECT.get(id);
    }

    static EntityEffect toApi(MobEffectInstance effect) {
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        var key = id == null ? Key.key("minecraft:unknown") : Key.key(id.getNamespace(), id.getPath());
        return new EntityEffect(
                key,
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon());
    }

    static MobEffectInstance toVanilla(EntityEffect effect) {
        var holder = holder(effect.effect())
                .orElseThrow(() -> new IllegalArgumentException("Unknown mob effect: " + effect.effect().asString()));
        return new MobEffectInstance(
                holder,
                effect.duration(),
                effect.amplifier(),
                effect.ambient(),
                effect.showParticles(),
                effect.showIcon());
    }
}
