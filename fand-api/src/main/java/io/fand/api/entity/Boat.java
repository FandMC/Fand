package io.fand.api.entity;

import net.kyori.adventure.key.Key;

public interface Boat extends Vehicle {

    enum Paddle {
        LEFT,
        RIGHT
    }

    Key woodType();

    boolean chestBoat();

    boolean raft();

    boolean paddling(Paddle paddle);

    void setPaddling(boolean left, boolean right);

    int bubbleTime();

    void setBubbleTime(int ticks);
}
