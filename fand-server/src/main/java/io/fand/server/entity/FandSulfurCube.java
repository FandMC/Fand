package io.fand.server.entity;

import io.fand.api.entity.SulfurCube;
import io.fand.server.world.WorldRegistry;

public final class FandSulfurCube extends FandAgeable implements SulfurCube {
    public FandSulfurCube(net.minecraft.world.entity.monster.cubemob.SulfurCube handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.monster.cubemob.SulfurCube handle() {
        return (net.minecraft.world.entity.monster.cubemob.SulfurCube) handle;
    }

    @Override public int size() { return handle().getSize(); }
    @Override public void setSize(int size, boolean updateHealth) { runOnServerThread(() -> handle().setSize(size, updateHealth)); }
    @Override public boolean fromBucket() { return handle().fromBucket(); }
    @Override public void setFromBucket(boolean fromBucket) { runOnServerThread(() -> handle().setFromBucket(fromBucket)); }
    @Override public int fuseTicks() { return handle().getFuse(); }
    @Override public boolean primed() { return handle().isPrimed(); }
    @Override public boolean canExplode() { return handle().canExplode(); }
    @Override public boolean prime(boolean imminent) { return runOnServerThreadFuture(() -> handle().primeTime(imminent)).join(); }
    @Override public boolean bodyItemEquipped() { return handle().hasBodyItem(); }
}
