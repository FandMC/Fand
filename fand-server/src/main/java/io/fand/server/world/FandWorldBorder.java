package io.fand.server.world;

import io.fand.api.world.WorldBorder;
import java.time.Duration;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;

public final class FandWorldBorder implements WorldBorder {

    private final ServerLevel level;

    public FandWorldBorder(ServerLevel level) {
        this.level = Objects.requireNonNull(level, "level");
    }

    @Override
    public double centerX() {
        return handle().getCenterX();
    }

    @Override
    public double centerZ() {
        return handle().getCenterZ();
    }

    @Override
    public void setCenter(double x, double z) {
        runOnServerThread(() -> handle().setCenter(x, z));
    }

    @Override
    public double size() {
        return handle().getSize();
    }

    @Override
    public double targetSize() {
        return handle().getLerpTarget();
    }

    @Override
    public long remainingTransitionTicks() {
        return handle().getLerpTime();
    }

    @Override
    public void setSize(double size) {
        runOnServerThread(() -> handle().setSize(size));
    }

    @Override
    public void setSize(double size, Duration transition) {
        Objects.requireNonNull(transition, "transition");
        long ticks = Math.max(0L, transition.toMillis() / 50L);
        runOnServerThread(() -> {
            var border = handle();
            if (ticks == 0L) {
                border.setSize(size);
            } else {
                border.lerpSizeBetween(border.getSize(), size, ticks, level.getGameTime());
            }
        });
    }

    @Override
    public int warningDistance() {
        return handle().getWarningBlocks();
    }

    @Override
    public void setWarningDistance(int blocks) {
        runOnServerThread(() -> handle().setWarningBlocks(Math.max(0, blocks)));
    }

    @Override
    public int warningTime() {
        return handle().getWarningTime();
    }

    @Override
    public void setWarningTime(int seconds) {
        runOnServerThread(() -> handle().setWarningTime(Math.max(0, seconds)));
    }

    @Override
    public double damageBuffer() {
        return handle().getSafeZone();
    }

    @Override
    public void setDamageBuffer(double blocks) {
        runOnServerThread(() -> handle().setSafeZone(Math.max(0.0, blocks)));
    }

    @Override
    public double damageAmount() {
        return handle().getDamagePerBlock();
    }

    @Override
    public void setDamageAmount(double damagePerBlock) {
        runOnServerThread(() -> handle().setDamagePerBlock(Math.max(0.0, damagePerBlock)));
    }

    @Override
    public boolean contains(double x, double z) {
        return handle().isWithinBounds(x, z);
    }

    private net.minecraft.world.level.border.WorldBorder handle() {
        return level.getWorldBorder();
    }

    private void runOnServerThread(Runnable task) {
        var server = level.getServer();
        if (server == null || server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }
}
