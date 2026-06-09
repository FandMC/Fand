package io.fand.api.block;

import net.kyori.adventure.text.Component;

/**
 * Sign or hanging-sign block entity.
 */
public interface SignBlockEntity extends BlockEntity {

    Component line(int index, boolean front);

    void setLine(int index, boolean front, Component line);

    default Component frontLine(int index) {
        return line(index, true);
    }

    default void setFrontLine(int index, Component line) {
        setLine(index, true, line);
    }

    default Component backLine(int index) {
        return line(index, false);
    }

    default void setBackLine(int index, Component line) {
        setLine(index, false, line);
    }

    boolean waxed();

    void setWaxed(boolean waxed);

    boolean glowingText(boolean front);

    void setGlowingText(boolean front, boolean glowing);
}
