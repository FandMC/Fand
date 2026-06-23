package io.fand.server.compat.modprotocol.servux;

import com.mojang.serialization.DataResult;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

final class ServuxHudProvider {

    static final int PROTOCOL_VERSION = 2;

    private final ServuxProtocol protocol;
    private final FandConfigView config;

    ServuxHudProvider(ServuxProtocol protocol, FandConfigView config) {
        this.protocol = protocol;
        this.config = config;
    }

    void handle(ServerPlayer player, ServuxPacketCodec.Incoming packet) {
        switch (packet.type()) {
            case 2 -> sendMetadata(player);
            case 4 -> sendSpawn(player);
            case 5 -> sendWeather(player);
            case 6 -> sendRecipes(player, packet.buffer().isReadable() ? ServuxPacketCodec.readNbt(packet.buffer()) : null);
            case 8 -> updateLoggers(player, ServuxPacketCodec.readNbt(packet.buffer()));
            default -> {
            }
        }
    }

    void tick(MinecraftServer server, int tick) {
        if (!config.hudEnabled()) {
            return;
        }
        if (tick % Math.max(1, config.hudUpdateInterval()) == 0) {
            for (var player : server.getPlayerList().getPlayers()) {
                if (config.shareWeather() && ServuxPermissions.has(player, config.weatherPermissionLevel())) {
                    sendWeather(player);
                }
                if (protocol.hasLogger(player)) {
                    sendLoggerTick(server, player);
                }
            }
        }
    }

    void sendMetadata(ServerPlayer player) {
        if (!config.hudEnabled() || !ServuxPermissions.has(player, config.hudPermissionLevel())) {
            return;
        }
        protocol.send(player, ServuxChannels.HUD, ServuxPacketCodec.metadata(ServuxPacketType.S2C_METADATA.id(), metadata(player)));
    }

    private void sendSpawn(ServerPlayer player) {
        if (!config.hudEnabled() || !ServuxPermissions.has(player, config.hudPermissionLevel())) {
            return;
        }
        protocol.send(player, ServuxChannels.HUD, ServuxPacketCodec.metadata(ServuxPacketType.S2C_SPAWN_DATA.id(), spawnMetadata(player)));
    }

    private void sendWeather(ServerPlayer player) {
        if (!config.hudEnabled() || !config.shareWeather() || !ServuxPermissions.has(player, config.weatherPermissionLevel())) {
            return;
        }
        protocol.send(player, ServuxChannels.HUD, ServuxPacketCodec.metadata(ServuxPacketType.S2C_WEATHER_TICK.id(), weatherMetadata(player.level())));
    }

    private void sendRecipes(ServerPlayer player, CompoundTag ignored) {
        if (!config.hudEnabled() || !ServuxPermissions.has(player, config.hudPermissionLevel())) {
            return;
        }
        var tag = new CompoundTag();
        var list = new ListTag();
        player.level().getServer().getRecipeManager().getRecipes().stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .forEach(recipe -> addRecipe(player, list, recipe));
        tag.put("RecipeManager", list);
        protocol.sendSplit(player, ServuxChannels.HUD, -1, tag);
    }

    private void updateLoggers(ServerPlayer player, CompoundTag request) {
        if (!config.hudLoggersEnabled() || request == null || !ServuxPermissions.has(player, config.hudLoggerPermissionLevel())) {
            protocol.setLogger(player, List.of());
            return;
        }
        var enabled = request.keySet().stream()
                .filter(key -> request.getBooleanOr(key, false))
                .filter(key -> key.equals("tps") || key.equals("mob_caps"))
                .toList();
        protocol.setLogger(player, enabled);
    }

    private void sendLoggerTick(MinecraftServer server, ServerPlayer player) {
        var tag = new CompoundTag();
        for (var logger : protocol.loggers(player)) {
            if (logger.equals("tps")) {
                tag.put("tps", tps(server));
            } else if (logger.equals("mob_caps")) {
                tag.put("mob_caps", mobCaps(server));
            }
        }
        if (!tag.isEmpty()) {
            protocol.send(player, ServuxChannels.HUD, ServuxPacketCodec.metadata(ServuxPacketType.S2C_DATA_LOGGER_TICK.id(), tag));
        }
    }

    private CompoundTag metadata(ServerPlayer player) {
        var tag = spawnMetadata(player);
        tag.putString("name", "hud_data");
        tag.putString("id", ServuxChannels.HUD.asString());
        tag.putInt("version", PROTOCOL_VERSION);
        tag.putString("servux", ServuxProtocol.VERSION_STRING);
        if (config.hudLoggersEnabled()) {
            var loggers = new CompoundTag();
            loggers.putBoolean("tps", true);
            loggers.putBoolean("mob_caps", true);
            tag.put("Loggers", loggers);
        }
        return tag;
    }

    private CompoundTag spawnMetadata(ServerPlayer player) {
        var server = player.level().getServer();
        var spawn = server.getRespawnData().pos();
        var tag = new CompoundTag();
        tag.putString("id", ServuxChannels.HUD.asString());
        tag.putString("servux", ServuxProtocol.VERSION_STRING);
        tag.putInt("version", PROTOCOL_VERSION);
        tag.putString("spawnDimension", Level.OVERWORLD.identifier().toString());
        tag.putInt("spawnPosX", spawn.getX());
        tag.putInt("spawnPosY", spawn.getY());
        tag.putInt("spawnPosZ", spawn.getZ());
        if (config.shareSeed() && ServuxPermissions.has(player, config.seedPermissionLevel())) {
            tag.putLong("worldSeed", server.overworld().getSeed());
        }
        return tag;
    }

    private static void addRecipe(ServerPlayer player, ListTag list, RecipeHolder<?> holder) {
        DataResult<Tag> result = Recipe.CODEC.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), holder.value());
        result.result().ifPresent(recipeTag -> {
            var entry = new CompoundTag();
            entry.putString("id_reg", holder.id().registry().toString());
            entry.putString("id_value", holder.id().identifier().toString());
            entry.put("recipe", recipeTag);
            list.add(entry);
        });
    }

    private static CompoundTag tps(MinecraftServer server) {
        var tag = new CompoundTag();
        double mspt = (double) server.getAverageTickTimeNanos() / TimeUnit.MILLISECONDS.toNanos(1L);
        boolean sprinting = server.tickRateManager().isSprinting();
        double tps = 1000.0D / Math.max(sprinting ? 0.0D : server.tickRateManager().millisecondsPerTick(), mspt);
        if (server.tickRateManager().isFrozen()) {
            tps = 0.0D;
        }
        tag.putDouble("mspt", mspt);
        tag.putDouble("tps", tps);
        tag.putLong("sprintTicks", 0L);
        tag.putBoolean("frozen", server.tickRateManager().isFrozen());
        tag.putBoolean("sprinting", sprinting);
        tag.putBoolean("stepping", server.tickRateManager().isSteppingForward());
        return tag;
    }

    private static CompoundTag weatherMetadata(ServerLevel level) {
        var weather = level.getWeatherData();
        var tag = new CompoundTag();
        tag.putString("id", ServuxChannels.HUD.asString());
        tag.putString("servux", ServuxProtocol.VERSION_STRING);
        tag.putBoolean("isRaining", weather.isRaining());
        tag.putBoolean("isThundering", weather.isThundering());
        if (weather.getClearWeatherTime() > -1) {
            tag.putInt("SetClear", weather.getClearWeatherTime());
        }
        if (weather.isRaining() && weather.getRainTime() > -1) {
            tag.putInt("SetRaining", weather.getRainTime());
        }
        if (weather.isThundering() && weather.getThunderTime() > -1) {
            tag.putInt("SetThundering", weather.getThunderTime());
        }
        return tag;
    }

    private static CompoundTag mobCaps(MinecraftServer server) {
        var root = new CompoundTag();
        for (var level : server.getAllLevels()) {
            var state = level.getChunkSource().getLastSpawnState();
            if (state == null || state.getSpawnableChunkCount() <= 0) {
                continue;
            }
            var world = new CompoundTag();
            var caps = new ListTag();
            for (MobCategory category : MobCategory.values()) {
                caps.add(mobCap(category, state));
            }
            world.putInt("cap_count", caps.size());
            world.put("cap_data", caps);
            world.putLong("WorldTick", level.getGameTime());
            root.put(level.dimension().identifier().toString(), world);
        }
        return root;
    }

    private static CompoundTag mobCap(MobCategory category, net.minecraft.world.level.NaturalSpawner.SpawnState state) {
        int spawnableChunks = state.getSpawnableChunkCount();
        int vanillaCap = category.getMaxInstancesPerChunk();
        int current = state.getMobCategoryCounts().getInt(category);
        int cap = Mth.clamp(vanillaCap * (spawnableChunks / 289), 0, vanillaCap);
        var tag = new CompoundTag();
        tag.putInt("current", current);
        tag.putInt("cap", cap);
        return tag;
    }

    interface FandConfigView {

        boolean hudEnabled();

        int hudPermissionLevel();

        int hudUpdateInterval();

        boolean hudLoggersEnabled();

        int hudLoggerPermissionLevel();

        boolean shareWeather();

        int weatherPermissionLevel();

        boolean shareSeed();

        int seedPermissionLevel();
    }
}
