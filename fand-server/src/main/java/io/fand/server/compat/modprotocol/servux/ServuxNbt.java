package io.fand.server.compat.modprotocol.servux;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

final class ServuxNbt {

    private ServuxNbt() {
    }

    static CompoundTag blockEntityFull(BlockEntity entity) {
        return entity.saveWithFullMetadata(entity.getLevel().registryAccess());
    }

    static CompoundTag blockEntityWithoutMetadata(BlockEntity entity) {
        return entity.saveWithoutMetadata(entity.getLevel().registryAccess());
    }

    static CompoundTag entity(Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        var tag = output.buildResult();
        var id = net.minecraft.world.entity.EntityType.getKey(entity.getType());
        if (id != null) {
            tag.putString("id", id.toString());
        }
        return tag;
    }
}
