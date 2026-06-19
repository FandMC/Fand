package io.fand.api.block;

import io.fand.api.item.ItemStack;

public interface JukeboxBlockEntity extends BlockEntity {
    ItemStack record();

    void setRecord(ItemStack record);

    boolean playing();

    long playTicks();

    void play();

    void stop();

    void eject();
}
