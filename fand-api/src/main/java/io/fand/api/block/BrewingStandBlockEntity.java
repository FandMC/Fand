package io.fand.api.block;

public interface BrewingStandBlockEntity extends ContainerBlockEntity {
    int brewTime();

    void setBrewTime(int ticks);

    int fuel();

    void setFuel(int fuel);
}
