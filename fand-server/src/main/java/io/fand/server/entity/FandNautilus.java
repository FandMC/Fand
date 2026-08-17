package io.fand.server.entity;

import io.fand.api.entity.Nautilus;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.server.inventory.FandContainerInventory;
import io.fand.server.world.WorldRegistry;

public class FandNautilus extends FandTameable implements Nautilus {
    private final Inventory inventory;

    public FandNautilus(net.minecraft.world.entity.animal.nautilus.AbstractNautilus handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
        this.inventory = new FandContainerInventory(handle.fand$getInventory(), InventoryType.HORSE);
    }

    @Override public net.minecraft.world.entity.animal.nautilus.AbstractNautilus handle() { return (net.minecraft.world.entity.animal.nautilus.AbstractNautilus) handle; }
    @Override public boolean dashing() { return handle().isDashing(); }
    @Override public void setDashing(boolean dashing) { runOnServerThread(() -> handle().setDashing(dashing)); }
    @Override public int dashCooldownTicks() { return handle().getJumpCooldown(); }

    @Override
    public void setDashCooldownTicks(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("ticks must be non-negative");
        runOnServerThread(() -> handle().fand$setDashCooldown(ticks));
    }

    @Override public Inventory inventory() { return inventory; }
}
