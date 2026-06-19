package io.fand.api.block;

public interface FurnaceBlockEntity extends ContainerBlockEntity {
    int cookTime();

    void setCookTime(int ticks);

    int cookTimeTotal();

    void setCookTimeTotal(int ticks);

    int burnTime();

    void setBurnTime(int ticks);

    int burnTimeTotal();

    void setBurnTimeTotal(int ticks);
}
