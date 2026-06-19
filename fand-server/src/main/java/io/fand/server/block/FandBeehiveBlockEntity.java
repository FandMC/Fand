package io.fand.server.block;

import io.fand.api.block.BeeReleaseMode;
import io.fand.api.block.BeehiveBlockEntity;
import io.fand.api.world.Location;
import io.fand.server.util.ReflectionFields;
import java.lang.reflect.Field;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public final class FandBeehiveBlockEntity extends FandBlockEntity implements BeehiveBlockEntity {

    private static final Field SAVED_FLOWER_POS = ReflectionFields.field(
            net.minecraft.world.level.block.entity.BeehiveBlockEntity.class, "savedFlowerPos");

    public FandBeehiveBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.BeehiveBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.BeehiveBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.BeehiveBlockEntity) handle;
    }

    @Override
    public int beeCount() {
        return handle().getOccupantCount();
    }

    @Override
    public boolean empty() {
        return handle().isEmpty();
    }

    @Override
    public boolean full() {
        return handle().isFull();
    }

    @Override
    public boolean sedated() {
        return handle().isSedated();
    }

    @Override
    public void releaseBees() {
        releaseBees(BeeReleaseMode.NORMAL);
    }

    @Override
    public void releaseBees(BeeReleaseMode mode) {
        java.util.Objects.requireNonNull(mode, "mode");
        block.runOnServerThread(() -> {
            var state = block.worldHandle().getBlockState(block.position());
            handle().emptyAllLivingFromHive(
                    null,
                    state,
                    releaseStatus(mode));
        });
    }

    @Override
    public Optional<Location> flowerPosition() {
        var pos = (BlockPos) ReflectionFields.value(SAVED_FLOWER_POS, handle());
        return pos == null
                ? Optional.empty()
                : Optional.of(new Location(block.world(), pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F));
    }

    @Override
    public void setFlowerPosition(Location location) {
        java.util.Objects.requireNonNull(location, "location");
        if (!location.world().key().equals(block.world().key())) {
            throw new IllegalArgumentException("Flower position must be in the same world as the beehive");
        }
        setFlowerPos(new BlockPos(location.blockX(), location.blockY(), location.blockZ()));
    }

    @Override
    public void clearFlowerPosition() {
        setFlowerPos(null);
    }

    private void setFlowerPos(BlockPos pos) {
        block.runOnServerThread(() -> {
            ReflectionFields.set(SAVED_FLOWER_POS, handle(), pos);
            syncBlockEntity();
        });
    }

    private static net.minecraft.world.level.block.entity.BeehiveBlockEntity.BeeReleaseStatus releaseStatus(BeeReleaseMode mode) {
        return switch (mode) {
            case NORMAL -> net.minecraft.world.level.block.entity.BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED;
            case HONEY_DELIVERED -> net.minecraft.world.level.block.entity.BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED;
            case EMERGENCY -> net.minecraft.world.level.block.entity.BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY;
        };
    }
}
