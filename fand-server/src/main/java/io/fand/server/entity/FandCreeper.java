package io.fand.server.entity;

import io.fand.api.entity.Creeper;
import io.fand.server.world.WorldRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.LoggerFactory;

public final class FandCreeper extends FandMob implements Creeper {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FandCreeper.class);

    public FandCreeper(net.minecraft.world.entity.monster.Creeper handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.monster.Creeper handle() {
        return (net.minecraft.world.entity.monster.Creeper) handle;
    }

    @Override
    public boolean charged() {
        return handle().isPowered();
    }

    @Override
    public void setCharged(boolean charged) {
        runOnServerThread(() -> updateData("powered", charged));
    }

    @Override
    public boolean ignited() {
        return handle().isIgnited();
    }

    @Override
    public void ignite() {
        runOnServerThread(handle()::ignite);
    }

    @Override
    public int swellDirection() {
        return handle().getSwellDir();
    }

    @Override
    public void setSwellDirection(int direction) {
        runOnServerThread(() -> handle().setSwellDir(Integer.compare(direction, 0)));
    }

    @Override
    public double swelling() {
        return handle().getSwelling(0.0F);
    }

    @Override
    public int fuseTicks() {
        return data().getShortOr("Fuse", (short) 30);
    }

    @Override
    public void setFuseTicks(int ticks) {
        runOnServerThread(() -> updateData("Fuse", (short) Math.max(1, ticks)));
    }

    @Override
    public int explosionRadius() {
        return data().getByteOr("ExplosionRadius", (byte) 3);
    }

    @Override
    public void setExplosionRadius(int radius) {
        runOnServerThread(() -> updateData("ExplosionRadius", (byte) Math.max(0, Math.min(Byte.MAX_VALUE, radius))));
    }

    private CompoundTag data() {
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                handle().registryAccess());
        handle().saveWithoutId(output);
        return output.buildResult();
    }

    private void updateData(String key, boolean value) {
        var tag = data();
        tag.putBoolean(key, value);
        load(tag);
    }

    private void updateData(String key, short value) {
        var tag = data();
        tag.putShort(key, value);
        load(tag);
    }

    private void updateData(String key, byte value) {
        var tag = data();
        tag.putByte(key, value);
        load(tag);
    }

    private void load(CompoundTag tag) {
        try (var reporter = new ProblemReporter.ScopedCollector(handle().problemPath(), LOGGER)) {
            handle().load(TagValueInput.create(reporter, handle().registryAccess(), tag));
        }
    }
}
