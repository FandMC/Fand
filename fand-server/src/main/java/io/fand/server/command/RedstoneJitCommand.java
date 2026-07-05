package io.fand.server.command;

import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.CommandSender;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;
import io.fand.api.command.Subcommand;
import io.fand.server.redstone.RedstoneClusterCandidateSnapshot;
import io.fand.server.redstone.RedstoneProbeSnapshot;
import io.fand.server.redstone.RedstoneRegionSnapshot;
import io.fand.server.redstone.RedstoneShadowCandidateSnapshot;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;

@Command(value = "redstonejit", namespace = "fand")
@Permission("fand.command.redstonejit")
public final class RedstoneJitCommand {

    private static final int TOP_LIMIT = 10;
    private final io.fand.server.FandServer server;

    public RedstoneJitCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Default
    public void execute(CommandContext context) {
        sendSnapshot(context.sender());
    }

    @Subcommand("status")
    public void status(CommandContext context) {
        sendSnapshot(context.sender());
    }

    private void sendSnapshot(CommandSender sender) {
        var runtime = server.redstoneRuntime();
        var snapshot = runtime.snapshot(TOP_LIMIT);
        runtime.refreshShadowCandidates(TOP_LIMIT);
        sendStatus(sender, snapshot);
        sendCandidates(sender, runtime.clusterCandidates(snapshot, TOP_LIMIT));
        sendShadowCandidates(sender, runtime.shadowCandidates(TOP_LIMIT));
        sendWireJit(sender, runtime.wireJitSnapshot());
        sendRegions(sender, runtime.regions().snapshot(TOP_LIMIT));
    }

    @Subcommand("clear")
    public void clear(CommandContext context) {
        server.redstoneRuntime().clear();
        context.sender().sendMessage(Component.text("Redstone JIT profiler cleared."));
    }

    private static void sendStatus(CommandSender sender, RedstoneProbeSnapshot snapshot) {
        sender.sendMessage(Component.text(String.format(
                Locale.ROOT,
                "Redstone JIT: mode=%s, events=%d, sampled=%d, estimatedTime=%.3fms, droppedPositions=%d",
                snapshot.mode().name().toLowerCase(Locale.ROOT),
                snapshot.observedEvents(),
                snapshot.totalCount(),
                nanosToMillis(snapshot.totalNanos()),
                snapshot.droppedPositionSamples())));
        if (snapshot.types().isEmpty()) {
            sender.sendMessage(Component.text("No redstone samples recorded."));
            return;
        }
        sender.sendMessage(Component.text("Type totals:"));
        for (var type : snapshot.types()) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s: %d calls, %.3fms",
                    type.type().id(),
                    type.count(),
                    nanosToMillis(type.totalNanos()))));
        }
        sender.sendMessage(Component.text("Top clusters:"));
        for (var cluster : snapshot.topClusters()) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s chunks [%d,%d -> %d,%d] (%d chunks): %d calls, %.3fms",
                    cluster.level(),
                    cluster.minChunkX(),
                    cluster.minChunkZ(),
                    cluster.maxChunkX(),
                    cluster.maxChunkZ(),
                    cluster.chunks(),
                    cluster.count(),
                    nanosToMillis(cluster.totalNanos()))));
        }
        sender.sendMessage(Component.text("Top regions:"));
        for (var region : snapshot.topRegions()) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s region %d,%d chunks [%d,%d -> %d,%d]: %d calls, %.3fms",
                    region.level(),
                    region.regionX(),
                    region.regionZ(),
                    region.minChunkX(),
                    region.minChunkZ(),
                    region.maxChunkX(),
                    region.maxChunkZ(),
                    region.count(),
                    nanosToMillis(region.totalNanos()))));
        }
        sender.sendMessage(Component.text("Top chunks:"));
        for (var chunk : snapshot.topChunks()) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s chunk %d,%d: %d calls, %.3fms",
                    chunk.level(),
                    chunk.chunkX(),
                    chunk.chunkZ(),
                    chunk.count(),
                    nanosToMillis(chunk.totalNanos()))));
        }
        sender.sendMessage(Component.text("Top positions:"));
        for (var position : snapshot.topPositions()) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s %s %s: %d calls, %.3fms",
                    position.type().id(),
                    position.level(),
                    formatPos(position.blockPos()),
                    position.count(),
                    nanosToMillis(position.totalNanos()))));
        }
    }

    private static void sendRegions(CommandSender sender, List<RedstoneRegionSnapshot> regions) {
        if (regions.isEmpty()) {
            return;
        }
        sender.sendMessage(Component.text("Tracked regions:"));
        for (var region : regions) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s region %d,%d chunks [%d,%d -> %d,%d]: hot=%s dirty=%s gen=%d samples=%d %.3fms activity=%d/%s invalidations=%d/%s",
                    region.level(),
                    region.regionX(),
                    region.regionZ(),
                    region.minChunkX(),
                    region.minChunkZ(),
                    region.maxChunkX(),
                    region.maxChunkZ(),
                    region.hot(),
                    region.dirty(),
                    region.generation(),
                    region.samples(),
                    nanosToMillis(region.totalNanos()),
                    region.activityCount(),
                    region.activityReason(),
                    region.invalidationCount(),
                    region.invalidationReason())));
        }
    }

    private static void sendCandidates(CommandSender sender, List<RedstoneClusterCandidateSnapshot> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        sender.sendMessage(Component.text("Candidate clusters:"));
        for (var candidate : candidates) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s chunks [%d,%d -> %d,%d] (%d chunks): readyForShadow=%s hot=%s regions=%d dirtyRegions=%d samples=%d %.3fms activity=%d invalidations=%d reason=%s",
                    candidate.level(),
                    candidate.minChunkX(),
                    candidate.minChunkZ(),
                    candidate.maxChunkX(),
                    candidate.maxChunkZ(),
                    candidate.chunks(),
                    candidate.readyForShadow(),
                    candidate.hot(),
                    candidate.coveredRegions(),
                    candidate.dirtyRegions(),
                    candidate.samples(),
                    nanosToMillis(candidate.totalNanos()),
                    candidate.activityCount(),
                    candidate.invalidationCount(),
                    candidate.blockedReason())));
        }
    }

    private static void sendShadowCandidates(CommandSender sender, List<RedstoneShadowCandidateSnapshot> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        sender.sendMessage(Component.text("Shadow candidates:"));
        for (var candidate : candidates) {
            sender.sendMessage(Component.text(String.format(
                    Locale.ROOT,
                    "  %s chunks [%d,%d -> %d,%d]: ready=%s stable=%d blocked=%d samples=%d %.3fms reason=%s",
                    candidate.level(),
                    candidate.minChunkX(),
                    candidate.minChunkZ(),
                    candidate.maxChunkX(),
                    candidate.maxChunkZ(),
                    candidate.ready(),
                    candidate.stableObservations(),
                    candidate.blockedObservations(),
                    candidate.lastSamples(),
                    nanosToMillis(candidate.lastNanos()),
                    candidate.lastReason())));
        }
    }

    private static void sendWireJit(CommandSender sender, io.fand.server.redstone.RedstoneWireJitSnapshot snapshot) {
        if (snapshot.attempts() == 0L && snapshot.plans() == 0) {
            return;
        }
        sender.sendMessage(Component.text(String.format(
                Locale.ROOT,
                "Wire JIT: plans=%d attempts=%d hits=%d compiled=%d guardMisses=%d capacityMisses=%d warmupMisses=%d cooldownMisses=%d invalidations=%d blockInvalidations=%d chunkInvalidations=%d",
                snapshot.plans(),
                snapshot.attempts(),
                snapshot.hits(),
                snapshot.compiled(),
                snapshot.guardMisses(),
                snapshot.capacityMisses(),
                snapshot.warmupMisses(),
                snapshot.cooldownMisses(),
                snapshot.invalidations(),
                snapshot.blockInvalidations(),
                snapshot.chunkInvalidations())));
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0D;
    }

    private static String formatPos(long packedPos) {
        return BlockPos.getX(packedPos) + "," + BlockPos.getY(packedPos) + "," + BlockPos.getZ(packedPos);
    }
}
