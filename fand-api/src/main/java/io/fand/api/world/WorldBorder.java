package io.fand.api.world;

import java.time.Duration;

/**
 * Live world border controls for a loaded world.
 *
 * <p>Methods that mutate the border marshal to the server thread.
 */
public interface WorldBorder {

    double centerX();

    double centerZ();

    void setCenter(double x, double z);

    double size();

    double targetSize();

    long remainingTransitionTicks();

    void setSize(double size);

    void setSize(double size, Duration transition);

    int warningDistance();

    void setWarningDistance(int blocks);

    int warningTime();

    void setWarningTime(int seconds);

    double damageBuffer();

    void setDamageBuffer(double blocks);

    double damageAmount();

    void setDamageAmount(double damagePerBlock);

    boolean contains(double x, double z);
}
