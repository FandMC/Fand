package io.fand.server.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EventHookSourceTest {

    @Test
    void damageAttackerUsesGenericWrapperThenChecksLivingType() throws IOException {
        var source = read("src/main/java/io/fand/server/event/EntityEvents.java");

        assertThat(source).contains("var attacker = wrapLivingDamageSource(source.getEntity(), \"EntityDamageEvent attacker\")");
        assertThat(source).contains("var wrapped = FandHooks.wrapEntity(entity);");
        assertThat(source).contains("if (wrapped instanceof io.fand.api.entity.LivingEntity living)");
    }

    @Test
    void projectileEventsPreserveShooterPayload() throws IOException {
        var source = read("src/main/java/io/fand/server/event/EntityEvents.java");

        assertThat(source).contains("projectile.getOwner() == null ? null : FandHooks.wrapEntity(projectile.getOwner())");
        assertThat(source).contains("potion.getOwner() == null ? null : FandHooks.wrapEntity(potion.getOwner())");
    }

    @Test
    void entitySpawnDropCancellationAndRemovePayloadPathsStayWired() throws IOException {
        var source = read("src/main/java/io/fand/server/event/EntityEvents.java");

        assertThat(source).contains("public static boolean fireSpawn");
        assertThat(source).contains("if (event.cancelled()) {\n                return false;");
        assertThat(source).contains("if (event.cancelled() || event.item().isEmpty()) {\n                return false;");
        assertThat(source).contains("public static net.minecraft.world.item.@Nullable ItemStack fireDropItem");
        assertThat(source).contains("return null;");
        assertThat(source).contains("public static void fireRemove");
        assertThat(source).contains("new EntityRemoveEvent(fandEntity, removeCause(entity.getRemovalReason()))");
    }

    @Test
    void blockCancellationHasRollbackHooksForGeneratedChanges() throws IOException {
        var source = read("src/main/java/io/fand/server/event/BlockEvents.java");

        assertThat(source).contains("restoreStructureGrow(level, snapshot)");
        assertThat(source).contains("level.setBlock(pos, entry.getValue(), Block.UPDATE_ALL)");
        assertThat(source).contains("return !event.cancelled();");
    }

    @Test
    void blockItemPlaceCancellationRestoresNeighbourChangesAndComponents() throws IOException {
        var source = read("src/minecraft/java/net/minecraft/world/item/BlockItem.java");

        assertThat(source).contains("snapshotPlacementStates(level, pos)");
        assertThat(source).contains("snapshotPlacementComponents(serverLevel, beforeStates.keySet())");
        assertThat(source).contains("changedPlacementPositions(level, beforeStates)");
        assertThat(source).contains("restorePlacement(serverLevel, beforeStates, beforeComponents, serverPlayer)");
        assertThat(source).contains("level.getBlockTicks().clearArea(restoreArea(beforeStates))");
        assertThat(source).contains("level.setBlock(pos, entry.getValue(), Block.UPDATE_ALL)");
    }

    @Test
    void tntPathCancellationIsCoveredByBlockItemPlacementRollbackWindow() throws IOException {
        var source = read("src/minecraft/java/net/minecraft/world/item/BlockItem.java");

        assertThat(source).contains("for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 2, 1)))");
        assertThat(source).contains("BlockState replacedState = level.getBlockState(pos)");
        assertThat(source).contains("placedState.getBlock().setPlacedBy(level, pos, placedState, player, itemStack)");
        assertThat(source).contains("if (!allowed) {");
        assertThat(source).contains("return InteractionResult.FAIL;");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
