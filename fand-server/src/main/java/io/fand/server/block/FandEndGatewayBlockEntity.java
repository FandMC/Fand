package io.fand.server.block;

import io.fand.api.block.EndGatewayBlockEntity;
import io.fand.api.world.Location;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public final class FandEndGatewayBlockEntity extends FandBlockEntity implements EndGatewayBlockEntity {

    public FandEndGatewayBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity) handle;
    }

    @Override
    public boolean spawning() {
        return handle().isSpawning();
    }

    @Override
    public boolean coolingDown() {
        return handle().isCoolingDown();
    }

    @Override
    public float spawnPercent() {
        return handle().getSpawnPercent(0.0F);
    }

    @Override
    public float cooldownPercent() {
        return handle().getCooldownPercent(0.0F);
    }

    @Override
    public Optional<Location> exitPosition() {
        return block.callOnServerThread(() -> handle()
                .saveCustomOnly(block.worldHandle().registryAccess())
                .read("exit_portal", BlockPos.CODEC)
                .map(pos -> new Location(block.world(), pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F)));
    }

    @Override
    public void setExitPosition(Location position, boolean exact) {
        var pos = BlockPos.containing(position.x(), position.y(), position.z());
        handle().setExitPosition(pos, exact);
    }

    @Override
    public void triggerCooldown() {
        net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity.triggerCooldown(
                block.worldHandle(), block.position(), block.worldHandle().getBlockState(block.position()), handle());
    }

    @Override
    public int particleAmount() {
        return handle().getParticleAmount();
    }
}
