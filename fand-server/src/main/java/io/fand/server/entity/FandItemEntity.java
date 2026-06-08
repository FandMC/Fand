package io.fand.server.entity;

import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;

public final class FandItemEntity extends FandEntity implements io.fand.api.entity.ItemEntity {

    public FandItemEntity(net.minecraft.world.entity.item.ItemEntity handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.item.ItemEntity handle() {
        return (net.minecraft.world.entity.item.ItemEntity) handle;
    }

    @Override
    public ItemStack item() {
        return FandItemStacks.fromVanilla(handle().getItem());
    }

    @Override
    public void setItem(ItemStack item) {
        Objects.requireNonNull(item, "item");
        runOnServerThread(() -> handle().setItem(FandItemStacks.toVanilla(item)));
    }

    @Override
    public int age() {
        return handle().getAge();
    }

    @Override
    public boolean hasPickupDelay() {
        return handle().hasPickUpDelay();
    }

    @Override
    public void setPickupDelay(int ticks) {
        runOnServerThread(() -> handle().setPickUpDelay(Math.max(0, ticks)));
    }

    @Override
    public void setNoPickupDelay() {
        runOnServerThread(() -> handle().setNoPickUpDelay());
    }

    @Override
    public void setNeverPickup() {
        runOnServerThread(() -> handle().setNeverPickUp());
    }

    @Override
    public void setUnlimitedLifetime() {
        runOnServerThread(() -> handle().setUnlimitedLifetime());
    }
}
