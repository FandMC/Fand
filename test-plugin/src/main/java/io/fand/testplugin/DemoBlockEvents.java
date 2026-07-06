package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.block.Block;
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.block.BlockBreakEvent;
import io.fand.api.event.block.BlockBurnEvent;
import io.fand.api.event.block.BlockCanBuildEvent;
import io.fand.api.event.block.BlockChangeEvent;
import io.fand.api.event.block.BlockDispenseEvent;
import io.fand.api.event.block.BlockExplodeEvent;
import io.fand.api.event.block.BlockFadeEvent;
import io.fand.api.event.block.BlockFertilizeEvent;
import io.fand.api.event.block.BlockFormEvent;
import io.fand.api.event.block.BlockFromToEvent;
import io.fand.api.event.block.BlockGrowEvent;
import io.fand.api.event.block.BlockIgniteEvent;
import io.fand.api.event.block.BlockMultiPlaceEvent;
import io.fand.api.event.block.BlockPhysicsEvent;
import io.fand.api.event.block.BlockPistonExtendEvent;
import io.fand.api.event.block.BlockPistonPushEvent;
import io.fand.api.event.block.BlockPistonRetractEvent;
import io.fand.api.event.block.BlockPlaceEvent;
import io.fand.api.event.block.BlockRedstoneEvent;
import io.fand.api.event.block.BlockSpreadEvent;
import io.fand.api.event.block.CauldronLevelChangeEvent;
import io.fand.api.event.block.FluidFlowEvent;
import io.fand.api.event.block.LeavesDecayEvent;
import io.fand.api.event.block.PortalCreateEvent;
import io.fand.api.event.block.SignChangeEvent;
import io.fand.api.event.block.SpongeAbsorbEvent;
import io.fand.api.plugin.PluginContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

final class DemoBlockEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;

    DemoBlockEvents(PluginContext context) {
        this.context = context;
        this.logger = context.logger();
    }

    @Subscribe
    public void onBlockBreak(BlockBreakEvent event) {
        if (blockName(event.blockType()).contains("diamond_ore")) {
            event.player().sendMessage(Component.text("Diamond ore break observed by the API.", NamedTextColor.AQUA));
        }
    }

    @Subscribe
    public void onBlockPlace(BlockPlaceEvent event) {
        if (context.config().booleanValue("protections.block-tnt-placement", true)
                && blockName(event.placedType()).equals("minecraft:tnt")) {
            event.setCancelled(true);
            event.player().sendMessage(Component.text("test-plugin cancelled TNT placement.", NamedTextColor.RED));
        }
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("{} placed block {} with hand={} item={} at {},{},{}",
                    event.player().name(),
                    blockName(event.placedType()),
                    event.hand(),
                    stackName(event.item()),
                    event.block().x(),
                    event.block().y(),
                    event.block().z());
        }
    }

    @Subscribe
    public void onBlockCanBuild(BlockCanBuildEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block can-build: {},{},{} type={} player={} item={} buildable={}",
                    event.block().x(),
                    event.block().y(),
                    event.block().z(),
                    blockName(event.blockType()),
                    event.player().map(player -> player.name()).orElse("none"),
                    stackName(event.item()),
                    event.buildable());
        }
    }

    @Subscribe
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("{} placed multi-block {} with hand={} item={} at {},{},{} blocks={}",
                    event.player().name(),
                    blockName(event.placedType()),
                    event.hand(),
                    stackName(event.item()),
                    event.block().x(),
                    event.block().y(),
                    event.block().z(),
                    event.blocks().size());
        }
    }

    @Subscribe
    public void onBlockChange(BlockChangeEvent event) {
        if (context.config().booleanValue("features.log-block-change-events", false)) {
            logger.info("Block changed: {} {},{},{} {} -> {}",
                    event.block().world().key().asString(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.oldType()), blockName(event.newType()));
        }
    }

    @Subscribe
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (context.config().booleanValue("features.log-block-physics-events", false)) {
            logger.info("Block physics: {} {},{},{} source={}",
                    event.block().world().key().asString(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.sourceType()));
        }
    }

    @Subscribe
    public void onBlockBurn(BlockBurnEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block burn: {} at {},{},{} source={},{},{}",
                    blockName(event.blockType()),
                    event.block().x(), event.block().y(), event.block().z(),
                    event.sourceBlock().x(), event.sourceBlock().y(), event.sourceBlock().z());
        }
    }

    @Subscribe
    public void onBlockDispense(BlockDispenseEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block dispense: {},{},{} direction={} item={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    event.direction(), stackName(event.item()));
        }
    }

    @Subscribe
    public void onBlockExplode(BlockExplodeEvent event) {
        if (context.config().booleanValue("protections.limit-explosion-block-damage", false)
                && event.affectedBlocks().size() > 512) {
            event.affectedBlocks().subList(512, event.affectedBlocks().size()).clear();
        }
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block explosion: {},{},{} affected={}",
                    event.block().x(), event.block().y(), event.block().z(), event.affectedBlocks().size());
        }
    }

    @Subscribe
    public void onBlockFade(BlockFadeEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block fade: {} {},{},{} {} -> {} cause={}",
                    event.block().world().key().asString(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.oldType()), blockName(event.newType()), event.cause());
        }
    }

    @Subscribe
    public void onBlockGrow(BlockGrowEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block grow: {} {},{},{} {} -> {} cause={}",
                    event.block().world().key().asString(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.oldType()), blockName(event.newType()), event.cause());
        }
    }

    @Subscribe
    public void onBlockForm(BlockFormEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block form: {} {},{},{} {} -> {} cause={}",
                    event.block().world().key().asString(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.oldType()), blockName(event.newType()), event.cause());
        }
    }

    @Subscribe
    public void onBlockFromTo(BlockFromToEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block from-to: {} {},{},{} -> {},{},{} new={} cause={}",
                    blockName(event.sourceType()),
                    event.sourceBlock().x(), event.sourceBlock().y(), event.sourceBlock().z(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.newType()), event.cause());
        }
    }

    @Subscribe
    public void onBlockFertilize(BlockFertilizeEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block fertilize: {},{},{} item={} player={} cause={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    stackName(event.item()),
                    event.player().map(player -> player.name()).orElse("none"),
                    event.cause());
        }
    }

    @Subscribe
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block ignite: {} {},{},{} type={} cause={} source={}",
                    event.block().world().key().asString(),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.ignitedType()), event.cause(),
                    event.sourceBlock()
                            .map(block -> block.x() + "," + block.y() + "," + block.z())
                            .orElse("none"));
        }
    }

    @Subscribe
    public void onPortalCreate(PortalCreateEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Portal create: world={} type={} cause={} blocks={}",
                    event.world().key().asString(), event.type(), event.cause(), event.blocks().size());
        }
    }

    @Subscribe
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Sponge absorb: {},{},{} blocks={}",
                    event.sponge().x(), event.sponge().y(), event.sponge().z(), event.absorbedBlocks().size());
        }
    }

    @Subscribe
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Cauldron level: {},{},{} {}({}) -> {}({}) cause={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.oldType()), event.oldLevel(),
                    blockName(event.newType()), event.newLevel(),
                    event.cause());
        }
    }

    @Subscribe
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (context.config().booleanValue("features.log-redstone-events", false)) {
            logger.info("Redstone update: {},{},{} {} -> {}",
                    event.block().x(), event.block().y(), event.block().z(),
                    event.oldCurrent(), event.newCurrent());
        }
    }

    @Subscribe
    public void onSignChange(SignChangeEvent event) {
        if (!event.lines().isEmpty() && event.lines().getFirst().equalsIgnoreCase("[fand]")) {
            event.setLine(0, "Fand API");
            if (event.lines().size() > 1) {
                event.setLine(1, event.player().name());
            }
        }
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("{} edited sign at {},{},{} front={} lines={}",
                    event.player().name(),
                    event.block().x(), event.block().y(), event.block().z(),
                    event.frontText(), event.lines());
        }
    }

    @Subscribe
    public void onBlockSpread(BlockSpreadEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Block spread: {} -> {},{},{} new={} cause={}",
                    blockName(event.sourceType()),
                    event.block().x(), event.block().y(), event.block().z(),
                    blockName(event.newType()), event.cause());
        }
    }

    @Subscribe
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Leaves decay: {} at {},{},{}",
                    blockName(event.blockType()), event.block().x(), event.block().y(), event.block().z());
        }
    }

    @Subscribe
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Piston extend: {},{},{} direction={} affected={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    event.direction(), event.affectedBlocks().size());
        }
    }

    @Subscribe
    public void onPistonPush(BlockPistonPushEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Piston push: {},{},{} direction={} affected={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    event.direction(), event.affectedBlocks().size());
        }
    }

    @Subscribe
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Piston retract: {},{},{} direction={} affected={}",
                    event.block().x(), event.block().y(), event.block().z(),
                    event.direction(), event.affectedBlocks().size());
        }
    }

    @Subscribe
    public void onFluidFlow(FluidFlowEvent event) {
        if (context.config().booleanValue("features.log-block-detail-events", false)) {
            logger.info("Fluid flow: {} {},{},{} -> {},{},{} direction={}",
                    event.fluid().asString(),
                    event.sourceBlock().x(), event.sourceBlock().y(), event.sourceBlock().z(),
                    event.block().x(), event.block().y(), event.block().z(),
                    event.direction());
        }
    }
}
