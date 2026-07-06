package io.fand.api.block;

import io.fand.api.item.ItemStack;

/** Lectern block entity. */
public interface LecternBlockEntity extends BlockEntity {

    ItemStack book();

    void setBook(ItemStack book);

    boolean bookPresent();

    int page();

    void setPage(int page);

    int redstoneSignal();
}
