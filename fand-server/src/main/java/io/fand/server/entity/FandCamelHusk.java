package io.fand.server.entity;

import io.fand.api.entity.CamelHusk;
import io.fand.server.world.WorldRegistry;

public final class FandCamelHusk extends FandHorse implements CamelHusk {

    public FandCamelHusk(net.minecraft.world.entity.animal.camel.CamelHusk handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.camel.CamelHusk handle() {
        return (net.minecraft.world.entity.animal.camel.CamelHusk) handle;
    }

    @Override
    public float chargeSpeedModifier() {
        return handle().chargeSpeedModifier();
    }
}
