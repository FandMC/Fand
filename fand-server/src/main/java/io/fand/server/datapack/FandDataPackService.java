package io.fand.server.datapack;

import com.google.gson.JsonObject;
import io.fand.api.datapack.DataPack;
import io.fand.api.datapack.DataPackFile;
import io.fand.api.datapack.DataPackRegistration;
import io.fand.api.datapack.DataPackService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

public final class FandDataPackService implements DataPackService {

    private static final String WORLD_PACK_PREFIX = "fand-";

    private final Path rootDirectory;
    private final Supplier<@Nullable MinecraftServer> server;
    private final Set<String> requestedEnabled = ConcurrentHashMap.newKeySet();

    public FandDataPackService(Path rootDirectory, Supplier<@Nullable MinecraftServer> server) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory").toAbsolutePath().normalize();
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Path rootDirectory() {
        ensureRoot();
        return rootDirectory;
    }

    @Override
    public Collection<DataPack> packs() {
        ensureRoot();
        try (var stream = Files.list(rootDirectory)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(path -> pack(path.getFileName().toString()))
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(DataPack::id))
                    .toList();
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to list managed data packs", failure);
        }
    }

    @Override
    public Optional<DataPack> pack(String id) {
        var normalized = normalizeId(id);
        var directory = packDirectory(normalized);
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        return Optional.of(new DataPack(
                normalized,
                readDescription(directory).orElse("Fand data pack " + normalized),
                requestedEnabled.contains(normalized) || selectedIds().contains(packRepositoryId(normalized))));
    }

    @Override
    public DataPackRegistration create(String id, String description) {
        return create(DataPack.of(id, description));
    }

    @Override
    public DataPackRegistration create(DataPack pack) {
        Objects.requireNonNull(pack, "pack");
        var directory = packDirectory(pack.id());
        try {
            Files.createDirectories(directory.resolve("data"));
            writePackMeta(directory, pack.description());
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to create data pack " + pack.id(), failure);
        }
        if (pack.enabled()) {
            enable(pack.id());
        } else {
            requestedEnabled.remove(pack.id());
        }
        return new Registration(this, pack.id());
    }

    @Override
    public void writeText(String packId, String path, String content) {
        Objects.requireNonNull(content, "content");
        write(packId, path, content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void write(String packId, String path, byte[] content) {
        Objects.requireNonNull(content, "content");
        var file = resolvePackFile(packId, path);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, content);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to write data pack file " + path + " in " + packId, failure);
        }
    }

    @Override
    public Optional<byte[]> read(String packId, String path) {
        var file = resolvePackFile(packId, path);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to read data pack file " + path + " in " + packId, failure);
        }
    }

    @Override
    public Collection<DataPackFile> files(String packId) {
        var id = normalizeId(packId);
        var directory = packDirectory(id);
        if (!Files.isDirectory(directory)) {
            return java.util.List.of();
        }
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> new DataPackFile(id, directory.relativize(path).toString().replace('\\', '/'), size(path)))
                    .sorted(Comparator.comparing(DataPackFile::path))
                    .toList();
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to list files for data pack " + id, failure);
        }
    }

    @Override
    public boolean deleteFile(String packId, String path) {
        var file = resolvePackFile(packId, path);
        try {
            return Files.deleteIfExists(file);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to delete data pack file " + path + " in " + packId, failure);
        }
    }

    @Override
    public boolean delete(String packId) {
        var id = normalizeId(packId);
        disable(id);
        var directory = packDirectory(id);
        var worldMirror = worldPackDirectory(id);
        if (!Files.exists(directory)) {
            return deleteDirectory(worldMirror);
        }
        var deleted = deleteDirectory(directory);
        deleteDirectory(worldMirror);
        return deleted;
    }

    @Override
    public boolean enable(String packId) {
        var id = normalizeId(packId);
        ensurePackExists(id);
        var wasRequested = requestedEnabled.add(id);
        syncPackToWorld(id);
        var current = new LinkedHashSet<>(selectedIds());
        var repositoryId = packRepositoryId(id);
        if (!current.add(repositoryId)) {
            return wasRequested;
        }
        applySelection(current);
        return true;
    }

    @Override
    public boolean disable(String packId) {
        var id = normalizeId(packId);
        var wasRequested = requestedEnabled.remove(id);
        var current = new LinkedHashSet<>(selectedIds());
        if (!current.remove(packRepositoryId(id))) {
            return wasRequested;
        }
        applySelection(current);
        return true;
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        var current = server.get();
        if (current == null) {
            return CompletableFuture.completedFuture(false);
        }
        syncRequestedPacksToWorld();
        current.getPackRepository().reload();
        var selected = new LinkedHashSet<>(current.getPackRepository().getSelectedIds());
        requestedEnabled.stream()
                .filter(id -> Files.isDirectory(packDirectory(id)))
                .map(FandDataPackService::packRepositoryId)
                .forEach(selected::add);
        return current.reloadResources(selected).thenApply(ignored -> true);
    }

    private void ensureRoot() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to create managed data pack root " + rootDirectory, failure);
        }
    }

    private void ensurePackExists(String id) {
        if (!Files.isDirectory(packDirectory(id))) {
            throw new IllegalArgumentException("Unknown managed data pack: " + id);
        }
    }

    private Path packDirectory(String id) {
        ensureRoot();
        return rootDirectory.resolve(normalizeId(id)).normalize();
    }

    private Path resolvePackFile(String packId, String path) {
        var directory = packDirectory(packId);
        ensurePackExists(packId);
        var resolved = directory.resolve(normalizeRelativePath(path)).normalize();
        if (!resolved.startsWith(directory)) {
            throw new IllegalArgumentException("Data pack path escapes the pack root: " + path);
        }
        return resolved;
    }

    private Collection<String> selectedIds() {
        var current = server.get();
        if (current == null) {
            return requestedEnabled.stream().map(FandDataPackService::packRepositoryId).toList();
        }
        current.getPackRepository().reload();
        return current.getPackRepository().getSelectedIds();
    }

    private void applySelection(Collection<String> selectedIds) {
        var current = server.get();
        if (current == null) {
            return;
        }
        current.getPackRepository().reload();
        current.getPackRepository().setSelected(selectedIds);
    }

    private void syncRequestedPacksToWorld() {
        requestedEnabled.stream()
                .filter(id -> Files.isDirectory(packDirectory(id)))
                .forEach(this::syncPackToWorld);
    }

    private void syncPackToWorld(String id) {
        var target = worldPackDirectory(id);
        if (target == null) {
            return;
        }
        var source = packDirectory(id);
        try {
            deleteDirectory(target);
            copyDirectory(source, target);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to sync managed data pack " + id + " to world data packs", failure);
        }
    }

    private @Nullable Path worldPackDirectory(String id) {
        var current = server.get();
        if (current == null) {
            return null;
        }
        return current.getWorldPath(LevelResource.DATAPACK_DIR).resolve(worldPackDirectoryName(id)).normalize();
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (var stream = Files.walk(source)) {
            for (var path : stream.toList()) {
                var relative = source.relativize(path);
                var destination = target.resolve(relative).normalize();
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static boolean deleteDirectory(@Nullable Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return false;
        }
        try (var stream = Files.walk(directory)) {
            var paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (var path : paths) {
                Files.deleteIfExists(path);
            }
            return true;
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to delete directory " + directory, failure);
        }
    }

    private static String normalizeId(String id) {
        return new DataPack(id, "", false).id();
    }

    private static String normalizeRelativePath(String path) {
        Objects.requireNonNull(path, "path");
        var normalized = path.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("://")) {
            throw new IllegalArgumentException("Data pack path must be relative: " + path);
        }
        var result = Path.of(normalized).normalize();
        if (result.isAbsolute() || result.startsWith("..") || result.toString().equals("..")) {
            throw new IllegalArgumentException("Data pack path escapes the pack root: " + path);
        }
        return result.toString().replace('\\', '/');
    }

    private static String packRepositoryId(String id) {
        return "file/" + worldPackDirectoryName(id);
    }

    private static String worldPackDirectoryName(String id) {
        return WORLD_PACK_PREFIX + id;
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to read file size for " + path, failure);
        }
    }

    private static void writePackMeta(Path directory, String description) throws IOException {
        var pack = new JsonObject();
        var meta = new JsonObject();
        meta.addProperty("description", description);
        meta.addProperty("pack_format", currentDataPackFormat());
        pack.add("pack", meta);
        Files.writeString(directory.resolve("pack.mcmeta"), DataPackService.PRETTY_GSON.toJson(pack) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static int currentDataPackFormat() {
        try {
            return SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major();
        } catch (IllegalStateException ignored) {
            return 1;
        }
    }

    private static Optional<String> readDescription(Path directory) {
        var file = directory.resolve("pack.mcmeta");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            var json = com.google.gson.JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!json.isJsonObject()) {
                return Optional.empty();
            }
            var pack = json.getAsJsonObject().getAsJsonObject("pack");
            if (pack == null || !pack.has("description")) {
                return Optional.empty();
            }
            return Optional.of(pack.get("description").getAsString());
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static final class Registration implements DataPackRegistration {
        private final FandDataPackService owner;
        private final String id;
        private volatile boolean active = true;

        private Registration(FandDataPackService owner, String id) {
            this.owner = owner;
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean active() {
            return active && owner.pack(id).isPresent();
        }

        @Override
        public void enable() {
            ensureOpen();
            owner.enable(id);
        }

        @Override
        public void disable() {
            if (active) {
                owner.disable(id);
            }
        }

        @Override
        public void delete() {
            if (active) {
                active = false;
                owner.delete(id);
            }
        }

        private void ensureOpen() {
            if (!active) {
                throw new IllegalStateException("Data pack registration is closed");
            }
        }
    }
}
