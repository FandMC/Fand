package io.fand.api.block;

/** Crafter slot and trigger state. */
public interface CrafterBlockEntity extends ContainerBlockEntity {
    /** Returns whether a crafting-grid slot in the range {@code 0..8} is enabled. */
    boolean slotEnabled(int slot);
    /** Enables or disables a crafting-grid slot in the range {@code 0..8}. */
    void setSlotEnabled(int slot, boolean enabled);
    boolean triggered();
    void setTriggered(boolean triggered);
    int redstoneSignal();
}
