package io.fand.server.block;

import io.fand.api.block.VaultBlockEntity;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FandVaultBlockEntity extends FandBlockEntity implements VaultBlockEntity {
    public FandVaultBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.vault.VaultBlockEntity handle) { super(block, handle); }
    @Override public net.minecraft.world.level.block.entity.vault.VaultBlockEntity handle() { return (net.minecraft.world.level.block.entity.vault.VaultBlockEntity) super.handle(); }
    @Override
    public ItemStack displayItem() {
        return block.callOnServerThread(() -> FandItemStacks.fromVanilla(handle().getSharedData().getDisplayItem()));
    }

    @Override
    public void setDisplayItem(ItemStack item) {
        Objects.requireNonNull(item, "item");
        block.runOnServerThread(() -> {
            handle().getSharedData().setDisplayItem(FandItemStacks.toVanilla(item));
            syncBlockEntity();
        });
    }

    @Override public Set<UUID> rewardedPlayers() { return block.callOnServerThread(() -> Set.copyOf(handle().fand$rewardedPlayers())); }
    @Override public Set<UUID> connectedPlayers() { return block.callOnServerThread(() -> Set.copyOf(handle().fand$connectedPlayers())); }
    @Override public float ejectionProgress() { return block.callOnServerThread(() -> handle().fand$ejectionProgress()); }
}
