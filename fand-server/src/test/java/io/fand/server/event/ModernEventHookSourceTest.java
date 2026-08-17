package io.fand.server.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ModernEventHookSourceTest {

    @Test
    void lowLevelStateAndPlayerPacketHooksStayWired() throws IOException {
        var blocks = read("src/main/java/io/fand/server/event/BlockEvents.java");
        var packets = read("src/minecraft/java/net/minecraft/server/network/ServerGamePacketListenerImpl.java");

        assertThat(blocks).contains("new BlockStateChangeEvent(block, snapshot(oldState), snapshot(newState), updateFlags)");
        assertThat(packets).contains("PlayerEvents.fireInput(");
        assertThat(packets).contains("if (this.fand$clientLoadEventPending)");
        assertThat(packets).contains("PlayerEvents.fireClientLoaded(this.player, !this.fand$completedInitialClientLoad)");
        assertThat(packets).contains("itemStack, location)");
    }

    @Test
    void inventoryAndKineticHooksPrecedeTheirMutations() throws IOException {
        var shelf = read("src/minecraft/java/net/minecraft/world/level/block/ShelfBlock.java");
        var crafter = read("src/minecraft/java/net/minecraft/world/level/block/CrafterBlock.java");
        var transport = read("src/minecraft/java/net/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers.java");
        var kinetic = read("src/minecraft/java/net/minecraft/world/item/component/KineticWeapon.java");
        var livingEntity = read("src/minecraft/java/net/minecraft/world/entity/LivingEntity.java");
        var serverPlayer = read("src/minecraft/java/net/minecraft/server/level/ServerPlayer.java");

        assertThat(shelf.indexOf("BlockEvents.fireShelfSwap(")).isLessThan(shelf.indexOf("swapSingleItem("));
        assertThat(crafter.indexOf("InventoryEvents.fireCrafterCraft(")).isLessThan(crafter.indexOf("this.dispenseItem(level"));
        assertThat(transport.indexOf("InventoryEvents.fireEntityMoveItem(")).isLessThan(transport.indexOf("container.removeItem(slot, movedCount)"));
        assertThat(transport).contains("container.removeItem(slot, movedCount)");
        assertThat(kinetic.indexOf("EntityEvents.fireKineticWeaponHit(")).isLessThan(kinetic.indexOf("livingEntity.rememberStabbedEntity(otherEntity)"));
        assertThat(livingEntity).contains("protected boolean fand$beforeCompleteUsingItem");
        assertThat(serverPlayer.indexOf("super.fand$beforeCompleteUsingItem(hand)"))
                .isLessThan(serverPlayer.indexOf("PlayerEvents.fireItemConsume"));
    }

    @Test
    void modernEntityAndBlockEntityWrappersStayAheadOfGenericFallbacks() throws IOException {
        var entities = read("src/main/java/io/fand/server/entity/EntityRegistry.java");
        var blocks = read("src/main/java/io/fand/server/block/FandBlock.java");

        assertThat(entities.indexOf("new FandCopperGolem")).isLessThan(entities.indexOf("new FandMob"));
        assertThat(entities.indexOf("new FandNautilus")).isLessThan(entities.indexOf("new FandTameable"));
        assertThat(entities.indexOf("new FandHappyGhast")).isLessThan(entities.indexOf("new FandAnimal"));
        assertThat(blocks.indexOf("new FandShelfBlockEntity")).isLessThan(blocks.indexOf("new FandContainerBlockEntity"));
        assertThat(blocks).contains("new FandCrafterBlockEntity", "new FandVaultBlockEntity", "new FandCreakingHeartBlockEntity");

        var crafter = read("src/main/java/io/fand/server/block/FandCrafterBlockEntity.java");
        var vault = read("src/main/java/io/fand/server/block/FandVaultBlockEntity.java");
        assertThat(crafter).contains("block.callOnServerThread", "block.runOnServerThread");
        assertThat(vault).contains("block.callOnServerThread", "block.runOnServerThread");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
