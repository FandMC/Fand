package io.fand.server.block;

import io.fand.api.block.SpawnerBlockEntity;
import io.fand.api.entity.EntityType;
import io.fand.server.entity.FandEntityType;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public final class FandSpawnerBlockEntity extends FandBlockEntity implements SpawnerBlockEntity {

    public FandSpawnerBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.SpawnerBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.SpawnerBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.SpawnerBlockEntity) handle;
    }

    @Override
    public Optional<EntityType> spawnedType() {
        return block.callOnServerThread(() -> spawnData().flatMap(tag -> tag.getString("id"))
                .flatMap(id -> BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)))
                .map(FandEntityType::of));
    }

    @Override
    public boolean setSpawnedType(EntityType type) {
        if (!(type instanceof FandEntityType fandType)) {
            return false;
        }
        block.runOnServerThread(() -> handle().setEntityId(fandType.handle(), block.worldHandle().getRandom()));
        return true;
    }

    @Override
    public int delay() {
        return block.callOnServerThread(() -> spawnerData().getIntOr("Delay", 20));
    }

    @Override
    public void setDelay(int ticks) {
        updateSpawner(() -> handle().getSpawner().fand$setSpawnDelay(ticks));
    }

    @Override
    public int minDelay() {
        return block.callOnServerThread(() -> spawnerData().getIntOr("MinSpawnDelay", 200));
    }

    @Override
    public void setMinDelay(int ticks) {
        updateSpawner(() -> handle().getSpawner().fand$setMinSpawnDelay(ticks));
    }

    @Override
    public int maxDelay() {
        return block.callOnServerThread(() -> spawnerData().getIntOr("MaxSpawnDelay", 800));
    }

    @Override
    public void setMaxDelay(int ticks) {
        updateSpawner(() -> handle().getSpawner().fand$setMaxSpawnDelay(ticks));
    }

    @Override
    public int spawnCount() {
        return block.callOnServerThread(() -> spawnerData().getIntOr("SpawnCount", 4));
    }

    @Override
    public void setSpawnCount(int count) {
        updateSpawner(() -> handle().getSpawner().fand$setSpawnCount(count));
    }

    @Override
    public int maxNearbyEntities() {
        return block.callOnServerThread(() -> spawnerData().getIntOr("MaxNearbyEntities", 6));
    }

    @Override
    public void setMaxNearbyEntities(int count) {
        updateSpawner(() -> handle().getSpawner().fand$setMaxNearbyEntities(count));
    }

    @Override
    public int requiredPlayerRange() {
        return block.callOnServerThread(() -> spawnerData().getIntOr("RequiredPlayerRange", 16));
    }

    @Override
    public void setRequiredPlayerRange(int blocks) {
        updateSpawner(() -> handle().getSpawner().fand$setRequiredPlayerRange(blocks));
    }

    @Override
    public int spawnRange() {
        return block.callOnServerThread(() -> spawnerData().getIntOr("SpawnRange", 4));
    }

    @Override
    public void setSpawnRange(int blocks) {
        updateSpawner(() -> handle().getSpawner().fand$setSpawnRange(blocks));
    }

    private Optional<CompoundTag> spawnData() {
        return spawnerData().getCompound("SpawnData").flatMap(tag -> tag.getCompound("entity"));
    }

    private CompoundTag spawnerData() {
        return handle().saveCustomOnly(block.worldHandle().registryAccess());
    }

    private void updateSpawner(Runnable update) {
        block.runOnServerThread(() -> {
            update.run();
            handle().setChanged();
            var state = block.worldHandle().getBlockState(block.position());
            block.worldHandle().sendBlockUpdated(block.position(), state, state, net.minecraft.world.level.block.Block.UPDATE_NONE);
        });
    }
}
