package io.fand.api.block;

import io.fand.api.item.ItemStack;
import java.util.Set;
import java.util.UUID;

/** Vault reward and display state. */
public interface VaultBlockEntity extends BlockEntity {
    ItemStack displayItem();
    void setDisplayItem(ItemStack item);
    Set<UUID> rewardedPlayers();
    Set<UUID> connectedPlayers();
    float ejectionProgress();
}
