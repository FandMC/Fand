package io.fand.server.player;

import io.fand.api.player.BanEntry;
import io.fand.api.player.OfflinePlayer;
import io.fand.api.player.OperatorEntry;
import io.fand.api.player.OperatorLevel;
import io.fand.api.player.PlayerAccessService;
import io.fand.api.player.PlayerProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.util.FileUtil;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

public final class FandPlayerAccessService implements PlayerAccessService {

    private final Supplier<MinecraftServer> server;

    public FandPlayerAccessService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> profile(String name) {
        var checked = requireName(name);
        return CompletableFuture.supplyAsync(() ->
                server().services().nameToIdCache().get(checked).map(PlayerProfiles::fromVanilla), Util.nonCriticalIoPool());
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> profile(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return CompletableFuture.supplyAsync(() ->
                server().services().nameToIdCache().get(uniqueId).map(PlayerProfiles::fromVanilla), Util.nonCriticalIoPool());
    }

    @Override
    public PlayerProfile offlineProfile(String name) {
        return PlayerProfiles.fromVanilla(NameAndId.createOffline(requireName(name)));
    }

    @Override
    public CompletableFuture<Optional<OfflinePlayer>> offlinePlayer(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        var current = server();
        return CompletableFuture.supplyAsync(() -> {
            var profile = current.services().nameToIdCache().get(uniqueId)
                    .orElseGet(() -> new NameAndId(uniqueId, uniqueId.toString()));
            return offlinePlayer(current, profile);
        }, Util.nonCriticalIoPool());
    }

    @Override
    public CompletableFuture<Optional<OfflinePlayer>> offlinePlayer(String name) {
        var checked = requireName(name);
        var current = server();
        return CompletableFuture.supplyAsync(() -> {
            var cached = current.services().nameToIdCache().get(checked)
                    .orElseGet(() -> NameAndId.createOffline(checked));
            return offlinePlayer(current, cached);
        }, Util.nonCriticalIoPool());
    }

    @Override
    public Collection<BanEntry> bans() {
        return callOnServerThread(() -> server().getPlayerList().getBans().getEntries().stream()
                .map(this::banEntry)
                .flatMap(Optional::stream)
                .toList());
    }

    @Override
    public Optional<BanEntry> ban(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> Optional.ofNullable(server().getPlayerList().getBans().get(vanilla))
                .flatMap(this::banEntry));
    }

    @Override
    public boolean banned(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> server().getPlayerList().getBans().isBanned(vanilla));
    }

    @Override
    public boolean ban(PlayerProfile profile, String source, String reason, @Nullable Instant expires) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(reason, "reason");
        var vanilla = PlayerProfiles.toVanilla(profile);
        var expiry = expires == null ? null : Date.from(expires);
        return callOnServerThread(() -> {
            var bans = server().getPlayerList().getBans();
            var entry = new UserBanListEntry(vanilla, new Date(), source, expiry, reason);
            if (!bans.add(entry)) {
                return false;
            }
            var online = server().getPlayerList().getPlayer(profile.uniqueId());
            if (online != null) {
                online.connection.disconnect(net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.banned"));
            }
            return true;
        });
    }

    @Override
    public boolean pardon(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> server().getPlayerList().getBans().remove(vanilla));
    }

    @Override
    public Collection<PlayerProfile> whitelist() {
        return callOnServerThread(() -> server().getPlayerList().getWhiteList().getEntries().stream()
                .map(UserWhiteListEntry::getUser)
                .filter(Objects::nonNull)
                .map(PlayerProfiles::fromVanilla)
                .toList());
    }

    @Override
    public boolean whitelisted(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> server().getPlayerList().getWhiteList().isWhiteListed(vanilla));
    }

    @Override
    public boolean addWhitelist(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> server().getPlayerList().getWhiteList().add(new UserWhiteListEntry(vanilla)));
    }

    @Override
    public boolean removeWhitelist(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> {
            boolean removed = server().getPlayerList().getWhiteList().remove(vanilla);
            if (removed) {
                server().kickUnlistedPlayers();
            }
            return removed;
        });
    }

    @Override
    public boolean whitelistEnabled() {
        return callOnServerThread(() -> server().isUsingWhitelist());
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        runOnServerThread(() -> {
            server().setUsingWhitelist(enabled);
            if (enabled) {
                server().kickUnlistedPlayers();
            }
        });
    }

    @Override
    public Collection<OperatorEntry> operators() {
        return callOnServerThread(() -> server().getPlayerList().getOps().getEntries().stream()
                .map(this::operatorEntry)
                .flatMap(Optional::stream)
                .toList());
    }

    @Override
    public Optional<OperatorEntry> operator(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> Optional.ofNullable(server().getPlayerList().getOps().get(vanilla))
                .flatMap(this::operatorEntry));
    }

    @Override
    public boolean isOperator(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> server().getPlayerList().isOp(vanilla));
    }

    @Override
    public boolean op(PlayerProfile profile, OperatorLevel level, boolean bypassesPlayerLimit) {
        Objects.requireNonNull(level, "level");
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> {
            var ops = server().getPlayerList().getOps();
            var previous = ops.get(vanilla);
            var permissions = OperatorLevels.toVanilla(level);
            if (previous != null
                    && previous.permissions().level() == permissions.level()
                    && previous.getBypassesPlayerLimit() == bypassesPlayerLimit) {
                return false;
            }
            server().getPlayerList().op(vanilla, Optional.of(permissions), Optional.of(bypassesPlayerLimit));
            return true;
        });
    }

    @Override
    public boolean deop(PlayerProfile profile) {
        var vanilla = PlayerProfiles.toVanilla(profile);
        return callOnServerThread(() -> {
            boolean removed = server().getPlayerList().getOps().get(vanilla) != null;
            server().getPlayerList().deop(vanilla);
            return removed;
        });
    }

    private Optional<BanEntry> banEntry(UserBanListEntry entry) {
        var user = entry.getUser();
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new BanEntry(
                PlayerProfiles.fromVanilla(user),
                entry.getCreated().toInstant(),
                entry.getSource(),
                entry.getExpires() == null ? null : entry.getExpires().toInstant(),
                entry.getReason()));
    }

    private Optional<OperatorEntry> operatorEntry(ServerOpListEntry entry) {
        var user = entry.getUser();
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new OperatorEntry(
                PlayerProfiles.fromVanilla(user),
                OperatorLevels.fromVanilla(entry.permissions().level()),
                entry.getBypassesPlayerLimit()));
    }

    private Optional<OfflinePlayer> offlinePlayer(MinecraftServer current, NameAndId profile) {
        return current.getPlayerList().loadPlayerData(profile)
                .map(data -> new FandOfflinePlayer(
                        profile,
                        PlayerProfiles.fromVanilla(profile),
                        data,
                        new ServerStatsCounter(current, statsFile(current, profile)),
                        current));
    }

    private static Path statsFile(MinecraftServer server, NameAndId profile) {
        var statFolder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        var uuidStatsFile = statFolder.resolve(profile.id() + ".json");
        if (Files.exists(uuidStatsFile)) {
            return uuidStatsFile;
        }
        var playerNameStatsFile = profile.name() + ".json";
        if (FileUtil.isValidPathSegment(playerNameStatsFile)) {
            var playerNameStatsPath = statFolder.resolve(playerNameStatsFile);
            if (Files.isRegularFile(playerNameStatsPath)) {
                return playerNameStatsPath;
            }
        }
        return uuidStatsFile;
    }

    private MinecraftServer server() {
        var current = server.get();
        if (current == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        return current;
    }

    private void runOnServerThread(Runnable task) {
        var current = server();
        if (current.isSameThread()) {
            task.run();
            return;
        }
        current.submit(() -> {
            task.run();
            return null;
        }).join();
    }

    private <T> T callOnServerThread(Supplier<T> task) {
        var current = server();
        if (current.isSameThread()) {
            return task.get();
        }
        return current.submit(task::get).join();
    }

    private static String requireName(String name) {
        var checked = Objects.requireNonNull(name, "name").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        return checked;
    }
}
