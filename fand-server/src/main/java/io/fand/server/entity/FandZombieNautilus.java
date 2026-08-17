package io.fand.server.entity;

import io.fand.api.entity.ZombieNautilus;
import io.fand.server.world.WorldRegistry;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class FandZombieNautilus extends FandNautilus implements ZombieNautilus {
    public FandZombieNautilus(net.minecraft.world.entity.animal.nautilus.ZombieNautilus handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override public net.minecraft.world.entity.animal.nautilus.ZombieNautilus handle() { return (net.minecraft.world.entity.animal.nautilus.ZombieNautilus) handle; }

    @Override
    public Key variant() {
        var id = handle().getVariant().unwrapKey().orElseThrow().identifier();
        return Key.key(id.getNamespace(), id.getPath());
    }

    @Override
    public void setVariant(Key variant) {
        runOnServerThread(() -> {
            var id = Identifier.fromNamespaceAndPath(variant.namespace(), variant.value());
            var key = ResourceKey.create(Registries.ZOMBIE_NAUTILUS_VARIANT, id);
            var holder = handle().registryAccess().lookupOrThrow(Registries.ZOMBIE_NAUTILUS_VARIANT).getOrThrow(key);
            handle().setVariant(holder);
        });
    }
}
