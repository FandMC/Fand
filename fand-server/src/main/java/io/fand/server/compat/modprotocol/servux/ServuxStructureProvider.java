package io.fand.server.compat.modprotocol.servux;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

final class ServuxStructureProvider {

    static final int PROTOCOL_VERSION = 2;

    private final ServuxProtocol protocol;
    private final FandConfigView config;
    private final Map<UUID, TrackedPlayer> tracked = new HashMap<>();

    ServuxStructureProvider(ServuxProtocol protocol, FandConfigView config) {
        this.protocol = protocol;
        this.config = config;
    }

    void handle(ServerPlayer player, ServuxPacketCodec.Incoming packet) {
        switch (packet.type()) {
            case 3 -> register(player);
            case 4 -> unregister(player.getUUID());
            case 11 -> protocol.hud().handle(player, new ServuxPacketCodec.Incoming(4, packet.buffer()));
            case 12 -> protocol.hud().handle(player, new ServuxPacketCodec.Incoming(5, packet.buffer()));
            default -> {
            }
        }
    }

    void tick(MinecraftServer server, int tick) {
        if (!config.structuresEnabled() || tick % Math.max(1, config.structuresUpdateInterval()) != 0) {
            return;
        }
        if (tracked.isEmpty()) {
            return;
        }
        var iter = tracked.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iter.remove();
                continue;
            }
            if (!ServuxPermissions.has(player, config.structuresPermissionLevel())) {
                iter.remove();
                continue;
            }
            refresh(player, entry.getValue(), tick);
        }
    }

    void forget(UUID player) {
        tracked.remove(player);
    }

    void clear() {
        tracked.clear();
    }

    void sendMetadata(ServerPlayer player) {
        if (!config.structuresEnabled() || !ServuxPermissions.has(player, config.structuresPermissionLevel())) {
            return;
        }
        protocol.send(player, ServuxChannels.STRUCTURES, ServuxPacketCodec.metadata(ServuxPacketType.S2C_METADATA.id(), metadata()));
    }

    private void register(ServerPlayer player) {
        if (!config.structuresEnabled() || !ServuxPermissions.has(player, config.structuresPermissionLevel())) {
            return;
        }
        var trackedPlayer = tracked.computeIfAbsent(player.getUUID(), ignored -> new TrackedPlayer());
        trackedPlayer.dimension = player.level().dimension().identifier().toString();
        protocol.send(player, ServuxChannels.STRUCTURES, ServuxPacketCodec.metadata(ServuxPacketType.S2C_METADATA.id(), metadata()));
        initialSync(player, trackedPlayer);
    }

    private void unregister(UUID player) {
        tracked.remove(player);
    }

    private void initialSync(ServerPlayer player, TrackedPlayer trackedPlayer) {
        trackedPlayer.timeoutChunks.clear();
        var server = player.level().getServer();
        var references = referencesWithinRange(player.level(), player.chunkPosition(), server.getPlayerList().getViewDistance() + 2);
        sendStructures(player, trackedPlayer, references, server.getTickCount());
    }

    private void refresh(ServerPlayer player, TrackedPlayer trackedPlayer, int tick) {
        var dimension = player.level().dimension().identifier().toString();
        if (!dimension.equals(trackedPlayer.dimension)) {
            trackedPlayer.dimension = dimension;
            initialSync(player, trackedPlayer);
            return;
        }
        var center = player.chunkPosition();
        var expired = new HashSet<ChunkPos>();
        Iterator<Map.Entry<ChunkPos, Integer>> iter = trackedPlayer.timeoutChunks.entrySet().iterator();
        int retain = player.level().getServer().getPlayerList().getViewDistance() + 2;
        while (iter.hasNext()) {
            var entry = iter.next();
            var pos = entry.getKey();
            if (Math.abs(pos.x() - center.x()) > retain || Math.abs(pos.z() - center.z()) > retain) {
                iter.remove();
                continue;
            }
            if (tick - entry.getValue() >= config.structuresTimeoutTicks()) {
                expired.add(pos);
            }
        }
        if (expired.isEmpty()) {
            return;
        }
        var references = new HashMap<Structure, LongSet>();
        for (var pos : expired) {
            collectReferences(pos.x(), pos.z(), player.level(), references);
            trackedPlayer.timeoutChunks.put(pos, tick);
        }
        sendStructures(player, trackedPlayer, references, tick);
    }

    private void sendStructures(ServerPlayer player, TrackedPlayer trackedPlayer, Map<Structure, LongSet> references, int tick) {
        var starts = starts(player.level(), references);
        if (starts.isEmpty()) {
            return;
        }
        for (LongSet chunks : references.values()) {
            chunks.forEach(chunk -> trackedPlayer.timeoutChunks.put(ChunkPos.unpack(chunk), tick));
        }
        var list = structureList(player.level(), starts);
        if (list.isEmpty()) {
            return;
        }
        var tag = new CompoundTag();
        tag.put("Structures", list);
        protocol.sendSplit(player, ServuxChannels.STRUCTURES, -1, tag, ServuxPacketType.S2C_STRUCTURE_DATA.id());
    }

    private Map<Structure, LongSet> referencesWithinRange(ServerLevel level, ChunkPos center, int radius) {
        var references = new HashMap<Structure, LongSet>();
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                collectReferences(x, z, level, references);
            }
        }
        return references;
    }

    private static void collectReferences(int chunkX, int chunkZ, Level level, Map<Structure, LongSet> references) {
        if (!level.hasChunk(chunkX, chunkZ)) {
            return;
        }
        ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);
        if (chunk == null) {
            return;
        }
        for (var entry : chunk.getAllReferences().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                references.computeIfAbsent(entry.getKey(), ignored -> new LongOpenHashSet()).addAll(entry.getValue());
            }
        }
    }

    private static Map<ChunkPos, StructureStart> starts(ServerLevel level, Map<Structure, LongSet> references) {
        var starts = new HashMap<ChunkPos, StructureStart>();
        for (var entry : references.entrySet()) {
            var iter = entry.getValue().iterator();
            while (iter.hasNext()) {
                var pos = ChunkPos.unpack(iter.nextLong());
                if (!level.hasChunk(pos.x(), pos.z())) {
                    continue;
                }
                ChunkAccess chunk = level.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_REFERENCES, false);
                if (chunk == null) {
                    continue;
                }
                var start = chunk.getStartForStructure(entry.getKey());
                if (start != null && start.isValid()) {
                    starts.put(pos, start);
                }
            }
        }
        return starts;
    }

    private ListTag structureList(ServerLevel level, Map<ChunkPos, StructureStart> starts) {
        var list = new ListTag();
        var context = StructurePieceSerializationContext.fromLevel(level);
        for (var entry : starts.entrySet()) {
            var start = entry.getValue();
            var structure = start.getStructure();
            if (structure == null) {
                continue;
            }
            var type = BuiltInRegistries.STRUCTURE_TYPE.getKey(structure.type());
            var id = level.registryAccess()
                    .lookupOrThrow(Registries.STRUCTURE)
                    .getResourceKey(structure)
                    .map(ResourceKey::identifier);
            if ((type == null && id.isEmpty()) || !allowed(type == null ? "" : type.toString(), id.map(Object::toString).orElse(""))) {
                continue;
            }
            var tag = start.createTag(context, entry.getKey());
            tag.putBoolean("ExpandBox", structure.terrainAdaptation() != TerrainAdjustment.NONE);
            list.add(tag);
        }
        return list;
    }

    private boolean allowed(String structureType, String structureId) {
        Set<String> whitelist = config.structureWhitelistEnabled() ? config.structureWhitelist() : Set.of();
        if (config.structureWhitelistEnabled() && !whitelist.isEmpty()) {
            return whitelist.contains(structureType) || whitelist.contains(structureId);
        }
        Set<String> blacklist = config.structureBlacklistEnabled() ? config.structureBlacklist() : Set.of();
        return !blacklist.contains(structureType) && !blacklist.contains(structureId);
    }

    private CompoundTag metadata() {
        var tag = new CompoundTag();
        tag.putString("name", "structure_bounding_boxes");
        tag.putString("id", ServuxChannels.STRUCTURES.asString());
        tag.putInt("version", PROTOCOL_VERSION);
        tag.putString("servux", ServuxProtocol.VERSION_STRING);
        tag.putInt("timeout", config.structuresTimeoutTicks());
        return tag;
    }

    private static final class TrackedPlayer {

        private final Map<ChunkPos, Integer> timeoutChunks = new HashMap<>();
        private String dimension = "";
    }

    interface FandConfigView {

        boolean structuresEnabled();

        int structuresPermissionLevel();

        int structuresUpdateInterval();

        int structuresTimeoutTicks();

        boolean structureWhitelistEnabled();

        Set<String> structureWhitelist();

        boolean structureBlacklistEnabled();

        Set<String> structureBlacklist();
    }
}
