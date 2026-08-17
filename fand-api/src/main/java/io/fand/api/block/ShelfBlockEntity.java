package io.fand.api.block;

/** Three-slot Shelf container controls. */
public interface ShelfBlockEntity extends ContainerBlockEntity {
    boolean alignItemsToBottom();
    void setAlignItemsToBottom(boolean align);
}
