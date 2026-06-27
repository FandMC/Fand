package io.fand.server.compat.modprotocol.servux;

import io.fand.api.structure.StructureFormat;
import io.fand.api.structure.StructureMirror;
import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureProjection;
import io.fand.api.structure.StructureRotation;
import io.fand.api.world.Location;
import io.fand.server.structure.FandStructureService;
import io.fand.server.world.FandWorld;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class ServuxLitematicaProvider {

    static final int PROTOCOL_VERSION = 1;

    private static final int MAX_TRANSMIT_SLICES = 262_144;
    private static final long MAX_TRANSMIT_BYTES = 512L * 1024L * 1024L;

    private final ServuxProtocol protocol;
    private final ServuxProtocol.ConfigView config;
    private final FandStructureService structures;
    private final ServuxSplitter uploads = new ServuxSplitter();
    private final Map<UUID, Map<Long, TransmitSession>> fileUploads = new HashMap<>();

    ServuxLitematicaProvider(
            ServuxProtocol protocol,
            ServuxProtocol.ConfigView config,
            FandStructureService structures
    ) {
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.config = Objects.requireNonNull(config, "config");
        this.structures = Objects.requireNonNull(structures, "structures");
    }

    void handle(ServerPlayer player, ServuxPacketCodec.Incoming packet) {
        switch (packet.type()) {
            case 2 -> sendMetadata(player);
            case 3 -> sendBlockEntity(player, packet.transactionalBlockPos().pos());
            case 4 -> sendEntity(player, packet.transactionalEntityId().entityId());
            case 7 -> sendBulk(player, -1, packet.chunkPos(), ServuxPacketCodec.readNbt(packet.buffer()));
            case 13 -> receiveUpload(player, ServuxPacketCodec.remaining(packet.buffer()));
            default -> {
            }
        }
    }

    void forget(UUID player) {
        uploads.forget(player);
        fileUploads.remove(player);
    }

    void close() {
        uploads.clear();
        fileUploads.clear();
    }

    void sendMetadata(ServerPlayer player) {
        if (!config.litematicaEnabled() || !ServuxPermissions.has(player, config.litematicaPermissionLevel())) {
            return;
        }
        var tag = new CompoundTag();
        tag.putString("name", "litematic_data");
        tag.putString("id", ServuxChannels.LITEMATICA.asString());
        tag.putInt("version", PROTOCOL_VERSION);
        tag.putString("servux", ServuxProtocol.VERSION_STRING);
        protocol.send(player, ServuxChannels.LITEMATICA, ServuxPacketCodec.metadata(ServuxPacketType.S2C_METADATA.id(), tag));
    }

    private void sendBlockEntity(ServerPlayer player, BlockPos pos) {
        if (!config.litematicaEnabled() || !ServuxPermissions.has(player, config.litematicaPermissionLevel())) {
            return;
        }
        var blockEntity = player.level().getBlockEntity(pos);
        var tag = blockEntity == null ? new CompoundTag() : ServuxNbt.blockEntityFull(blockEntity);
        protocol.send(player, ServuxChannels.LITEMATICA, ServuxPacketCodec.blockEntitySimple(pos, tag));
    }

    private void sendEntity(ServerPlayer player, int entityId) {
        if (!config.litematicaEnabled() || !ServuxPermissions.has(player, config.litematicaPermissionLevel())) {
            return;
        }
        var entity = player.level().getEntity(entityId);
        if (entity == null) {
            return;
        }
        protocol.send(player, ServuxChannels.LITEMATICA, ServuxPacketCodec.entitySimple(entityId, ServuxNbt.entity(entity)));
    }

    private void sendBulk(ServerPlayer player, int transactionId, ChunkPos chunkPos, CompoundTag request) {
        if (!config.litematicaEnabled() || !ServuxPermissions.has(player, config.litematicaPermissionLevel())) {
            return;
        }
        if (request == null || request.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        var chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
        if (chunk == null) {
            return;
        }
        int minY = request.getIntOr("minY", level.getMinY());
        int maxY = request.getIntOr("maxY", level.getMaxY());
        var tileList = new ListTag();
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (pos.getY() < minY || pos.getY() > maxY) {
                continue;
            }
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                tileList.add(ServuxNbt.blockEntityFull(blockEntity));
            }
        }
        var entityList = new ListTag();
        var min = new BlockPos(chunkPos.getMinBlockX(), minY, chunkPos.getMinBlockZ());
        var box = new AABB(
                chunkPos.getMinBlockX(),
                minY,
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1.0,
                maxY + 1.0,
                chunkPos.getMaxBlockZ() + 1.0);
        for (Entity entity : level.getEntities((Entity) null, box, entity -> !(entity instanceof ServerPlayer))) {
            var tag = ServuxNbt.entity(entity);
            var id = EntityType.getKey(entity.getType());
            if (id == null) {
                continue;
            }
            writeEntityPosition(new Vec3(entity.getX() - min.getX(), entity.getY() - min.getY(), entity.getZ() - min.getZ()), tag);
            tag.putInt("entityId", entity.getId());
            entityList.add(tag);
        }
        var response = new CompoundTag();
        response.putString("Task", "BulkEntityReply");
        response.put("TileEntities", tileList);
        response.put("Entities", entityList);
        response.putInt("chunkX", chunkPos.x());
        response.putInt("chunkZ", chunkPos.z());
        protocol.sendSplit(player, ServuxChannels.LITEMATICA, Math.max(0, transactionId), response);
    }

    private void receiveUpload(ServerPlayer player, byte[] slice) {
        var complete = uploads.receive(player.getUUID(), slice);
        if (complete == null) {
            return;
        }
        var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(complete));
        int transactionId = buffer.readVarInt();
        var tag = ServuxPacketCodec.readNbt(buffer);
        if (tag == null) {
            return;
        }
        if (!canPaste(player)) {
            respond(player, transactionId, tag.getStringOr("Task", "LitematicaPaste"), "Denied",
                    "Fand Servux paste is disabled or permission/creative checks failed");
            return;
        }
        handlePasteTask(player, transactionId, tag);
    }

    private boolean canPaste(ServerPlayer player) {
        return config.litematicaPasteEnabled()
                && ServuxPermissions.has(player, config.litematicaPermissionLevel())
                && ServuxPermissions.has(player, config.litematicaPastePermissionLevel())
                && player.isCreative();
    }

    private void handlePasteTask(ServerPlayer player, int transactionId, CompoundTag tag) {
        var task = tag.getStringOr("Task", "LitematicaPaste");
        switch (task) {
            case "Litematic-TransmitStart" -> startTransmit(player, transactionId, tag);
            case "Litematic-TransmitData" -> receiveTransmitData(player, transactionId, tag);
            case "Litematic-TransmitCancel" -> cancelTransmit(player, transactionId, tag);
            case "Litematic-TransmitEnd" -> finishTransmit(player, transactionId, tag);
            case "LitematicaPaste" -> pasteInline(player, transactionId, tag);
            default -> respond(player, transactionId, task, "Unsupported", "Unsupported Servux litematica task: " + task);
        }
    }

    private void pasteInline(ServerPlayer player, int transactionId, CompoundTag tag) {
        var schematic = tag.getCompound("Schematics").orElse(null);
        if (schematic == null || schematic.isEmpty()) {
            respond(player, transactionId, "LitematicaPaste", "Unsupported",
                    "Inline LitematicaPaste did not include a Schematics compound");
            return;
        }
        paste(player, transactionId, "LitematicaPaste", tag, nbtBytes(schematic), StructureFormat.LITEMATIC);
    }

    private void startTransmit(ServerPlayer player, int transactionId, CompoundTag tag) {
        long key = tag.getLongOr("SliceKey", -1L);
        int totalSlices = tag.getIntOr("TotalSlices", -1);
        long totalSize = tag.getLongOr("TotalSize", -1L);
        if (key == -1L || totalSlices < 1 || totalSlices > MAX_TRANSMIT_SLICES || totalSize < 0L || totalSize > MAX_TRANSMIT_BYTES) {
            respond(player, transactionId, "Litematic-TransmitStart", "Denied", "Invalid Servux file transfer header");
            return;
        }
        var placementData = tag.getCompoundOrEmpty("PlacementData").copy();
        var session = new TransmitSession(
                tag.getStringOr("FileName", "servux_upload.litematic"),
                fileFormat(tag),
                totalSlices,
                totalSize,
                placementData);
        fileUploads.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(key, session);
    }

    private void receiveTransmitData(ServerPlayer player, int transactionId, CompoundTag tag) {
        long key = tag.getLongOr("SliceKey", -1L);
        var session = session(player, key);
        if (session == null) {
            respond(player, transactionId, "Litematic-TransmitData", "Denied", "Unknown Servux file transfer session");
            return;
        }
        int slice = tag.getIntOr("Slice", -1);
        int size = tag.getIntOr("Size", -1);
        byte[] data = tag.getByteArray("Data").orElse(null);
        if (!session.receive(slice, data, size)) {
            respond(player, transactionId, "Litematic-TransmitData", "Denied", "Invalid Servux file transfer slice");
        }
    }

    private void cancelTransmit(ServerPlayer player, int transactionId, CompoundTag tag) {
        long key = tag.getLongOr("SliceKey", -1L);
        var sessions = fileUploads.get(player.getUUID());
        if (sessions != null) {
            sessions.remove(key);
            if (sessions.isEmpty()) {
                fileUploads.remove(player.getUUID());
            }
        }
        respond(player, transactionId, "Litematic-TransmitCancel", "Cancelled", "Servux file transfer was cancelled");
    }

    private void finishTransmit(ServerPlayer player, int transactionId, CompoundTag tag) {
        long key = tag.getLongOr("SliceKey", -1L);
        var sessions = fileUploads.get(player.getUUID());
        var session = sessions == null ? null : sessions.remove(key);
        if (sessions != null && sessions.isEmpty()) {
            fileUploads.remove(player.getUUID());
        }
        if (session == null) {
            respond(player, transactionId, "Litematic-TransmitEnd", "Denied", "Unknown Servux file transfer session");
            return;
        }
        var data = session.finish();
        if (data == null) {
            respond(player, transactionId, "Litematic-TransmitEnd", "Denied", "Incomplete Servux file transfer");
            return;
        }
        paste(player, transactionId, "Litematic-TransmitEnd", session.placementData(), data, session.format());
    }

    private TransmitSession session(ServerPlayer player, long key) {
        var sessions = fileUploads.get(player.getUUID());
        return sessions == null ? null : sessions.get(key);
    }

    private void paste(
            ServerPlayer player,
            int transactionId,
            String task,
            CompoundTag placementData,
            byte[] data,
            StructureFormat format
    ) {
        var origin = origin(player, placementData);
        var placement = placement(placementData);
        var key = Key.key("fand", "servux/" + player.getUUID() + "/" + Integer.toHexString(java.util.Arrays.hashCode(data)));
        var projection = StructureProjection.of(format, data)
                .withSourceKey(key)
                .withName(placementData.getStringOr("Name", task));
        try {
            structures.placeEphemeral(projection, origin, placement).whenComplete((placed, failure) -> {
                if (failure != null) {
                    respond(player, transactionId, task, "Error", failure.getMessage());
                    return;
                }
                if (Boolean.TRUE.equals(placed)) {
                    respond(player, transactionId, task, "Done", "Servux litematic paste completed");
                    player.sendSystemMessage(Component.literal("Servux litematic pasted: " + projection.name().orElse(task)));
                } else {
                    respond(player, transactionId, task, "Error", "Structure placement returned false");
                }
            });
        } catch (RuntimeException failure) {
            respond(player, transactionId, task, "Error", failure.getMessage());
        }
    }

    private void respond(ServerPlayer player, int transactionId, String task, String status, String reason) {
        var response = new CompoundTag();
        response.putString("Task", task);
        response.putString("Status", status);
        response.putString("Reason", reason == null ? "" : reason);
        protocol.sendSplit(player, ServuxChannels.LITEMATICA, Math.max(0, transactionId), response);
        if (!"Done".equals(status)) {
            player.sendSystemMessage(Component.literal("Servux litematica " + status + ": " + response.getStringOr("Reason", "")));
        }
    }

    private static Location origin(ServerPlayer player, CompoundTag tag) {
        int[] origin = intArray(tag, "Origin");
        if (origin == null) {
            origin = intArray(tag, "origin");
        }
        if (origin == null) {
            return new Location(new FandWorld(player.level()), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
        return new Location(new FandWorld(player.level()), origin[0], origin[1], origin[2], player.getYRot(), player.getXRot());
    }

    private static int[] intArray(CompoundTag tag, String key) {
        var array = tag.getIntArray(key).orElse(null);
        if (array != null && array.length >= 3) {
            return new int[] { array[0], array[1], array[2] };
        }
        var list = tag.getList(key).orElse(null);
        if (list != null && list.size() >= 3) {
            return new int[] { list.getIntOr(0, 0), list.getIntOr(1, 0), list.getIntOr(2, 0) };
        }
        var compound = tag.getCompound(key).orElse(null);
        if (compound != null && !compound.isEmpty()) {
            return new int[] {
                    compound.getIntOr("x", compound.getIntOr("X", 0)),
                    compound.getIntOr("y", compound.getIntOr("Y", 0)),
                    compound.getIntOr("z", compound.getIntOr("Z", 0))
            };
        }
        return null;
    }

    private static StructurePlacement placement(CompoundTag tag) {
        return StructurePlacement.defaults()
                .withRotation(rotation(tag.getIntOr("Rotation", 0)))
                .withMirror(mirror(tag.getIntOr("Mirror", 0)))
                .withIncludeEntities(!tag.getBooleanOr("IgnoreEntities", false));
    }

    private static StructureRotation rotation(int ordinal) {
        return switch (Math.floorMod(ordinal, 4)) {
            case 1 -> StructureRotation.CLOCKWISE_90;
            case 2 -> StructureRotation.CLOCKWISE_180;
            case 3 -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };
    }

    private static StructureMirror mirror(int ordinal) {
        return switch (ordinal) {
            case 1 -> StructureMirror.LEFT_RIGHT;
            case 2 -> StructureMirror.FRONT_BACK;
            default -> StructureMirror.NONE;
        };
    }

    private static StructureFormat fileFormat(CompoundTag tag) {
        var fileName = tag.getStringOr("FileName", "").toLowerCase(Locale.ROOT);
        var type = tag.getStringOr("FileType", "").toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".schem") || type.contains("sponge")) {
            return StructureFormat.SPONGE_SCHEMATIC;
        }
        if (fileName.endsWith(".schematic") || type.equals("schematic")) {
            return StructureFormat.WORLDEDIT_SCHEMATIC;
        }
        if (fileName.endsWith(".nbt") || type.contains("vanilla")) {
            return StructureFormat.VANILLA_NBT;
        }
        if (fileName.endsWith(".snbt")) {
            return StructureFormat.VANILLA_SNBT;
        }
        return StructureFormat.LITEMATIC;
    }

    private static byte[] nbtBytes(CompoundTag tag) {
        try {
            var out = new ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(tag, out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException("Could not encode litematic NBT", failure);
        }
    }

    private static void writeEntityPosition(Vec3 pos, CompoundTag tag) {
        var list = new ListTag();
        list.add(DoubleTag.valueOf(pos.x()));
        list.add(DoubleTag.valueOf(pos.y()));
        list.add(DoubleTag.valueOf(pos.z()));
        tag.put("Pos", list);
    }

    private record TransmitSession(
            String fileName,
            StructureFormat format,
            int totalSlices,
            long totalSize,
            CompoundTag placementData,
            byte[][] slices,
            int[] sizes
    ) {

        private TransmitSession(
                String fileName,
                StructureFormat format,
                int totalSlices,
                long totalSize,
                CompoundTag placementData
        ) {
            this(fileName, format, totalSlices, totalSize, placementData, new byte[totalSlices][], new int[totalSlices]);
        }

        boolean receive(int slice, byte[] data, int size) {
            if (data == null || size < 0 || size > data.length) {
                return false;
            }
            if (slice < 0 || slice >= totalSlices || slices[slice] != null) {
                slice = firstEmptySlice();
            }
            if (slice < 0) {
                return false;
            }
            if (slices[slice] != null) {
                return true;
            }
            var copy = java.util.Arrays.copyOf(data, size);
            slices[slice] = copy;
            sizes[slice] = size;
            return true;
        }

        private int firstEmptySlice() {
            for (int i = 0; i < totalSlices; i++) {
                if (slices[i] == null) {
                    return i;
                }
            }
            return -1;
        }

        byte[] finish() {
            long actual = 0L;
            for (int i = 0; i < totalSlices; i++) {
                if (slices[i] == null) {
                    return null;
                }
                actual += sizes[i];
            }
            if (actual != totalSize) {
                return null;
            }
            var out = new byte[Math.toIntExact(actual)];
            int offset = 0;
            for (int i = 0; i < totalSlices; i++) {
                System.arraycopy(slices[i], 0, out, offset, sizes[i]);
                offset += sizes[i];
            }
            return out;
        }
    }
}
