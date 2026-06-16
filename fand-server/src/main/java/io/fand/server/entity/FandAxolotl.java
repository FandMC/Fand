package io.fand.server.entity;

import io.fand.api.entity.Axolotl;
import io.fand.server.world.WorldRegistry;

public final class FandAxolotl extends FandAnimal implements Axolotl {

    public FandAxolotl(net.minecraft.world.entity.animal.axolotl.Axolotl handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.animal.axolotl.Axolotl handle() {
        return (net.minecraft.world.entity.animal.axolotl.Axolotl) handle;
    }
}
