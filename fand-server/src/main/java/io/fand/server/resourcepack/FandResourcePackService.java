package io.fand.server.resourcepack;

import com.google.gson.JsonObject;
import io.fand.api.resourcepack.ResourcePack;
import io.fand.api.resourcepack.ResourcePackBuild;
import io.fand.api.resourcepack.ResourcePackFile;
import io.fand.api.resourcepack.ResourcePackRegistration;
import io.fand.api.resourcepack.ResourcePackService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

public final class FandResourcePackService implements ResourcePackService {

    private final Path rootDirectory;
    private final Path buildDirectory;
    private final ConcurrentHashMap<String, ReentrantLock> buildLocks = new ConcurrentHashMap<>();

    public FandResourcePackService(Path rootDirectory) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory").toAbsolutePath().normalize();
        this.buildDirectory = this.rootDirectory.resolve("builds").normalize();
    }

    @Override
    public Path rootDirectory() {
        ensureRoot();
        return rootDirectory;
    }

    @Override
    public Path buildDirectory() {
        ensureBuildRoot();
        return buildDirectory;
    }

    @Override
    public Collection<ResourcePack> packs() {
        ensureRoot();
        try (var stream = Files.list(rootDirectory)) {
            return stream
                    .filter(path -> Files.isDirectory(path) && !path.equals(buildDirectory))
                    .map(path -> pack(path.getFileName().toString()))
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(ResourcePack::id))
                    .toList();
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to list managed resource packs", failure);
        }
    }

    @Override
    public Optional<ResourcePack> pack(String id) {
        var normalized = normalizeId(id);
        var directory = packDirectory(normalized);
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        return Optional.of(new ResourcePack(
                normalized,
                readDescription(directory).orElse("Fand resource pack " + normalized),
                readPackFormat(directory).orElse(currentResourcePackFormat())));
    }

    @Override
    public ResourcePackRegistration create(String id, String description) {
        return create(id, description, currentResourcePackFormat());
    }

    @Override
    public ResourcePackRegistration create(String id, String description, int packFormat) {
        return create(new ResourcePack(id, description, packFormat));
    }

    @Override
    public ResourcePackRegistration create(ResourcePack pack) {
        Objects.requireNonNull(pack, "pack");
        var directory = packDirectory(pack.id());
        try {
            Files.createDirectories(directory.resolve("assets"));
            writePackMeta(directory, pack.description(), pack.packFormat());
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to create resource pack " + pack.id(), failure);
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
            throw new UncheckedIOException("Failed to write resource pack file " + path + " in " + packId, failure);
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
            throw new UncheckedIOException("Failed to read resource pack file " + path + " in " + packId, failure);
        }
    }

    @Override
    public Collection<ResourcePackFile> files(String packId) {
        var id = normalizeId(packId);
        var directory = packDirectory(id);
        if (!Files.isDirectory(directory)) {
            return java.util.List.of();
        }
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> new ResourcePackFile(id, directory.relativize(path).toString().replace('\\', '/'), size(path)))
                    .sorted(Comparator.comparing(ResourcePackFile::path))
                    .toList();
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to list files for resource pack " + id, failure);
        }
    }

    @Override
    public boolean deleteFile(String packId, String path) {
        var file = resolvePackFile(packId, path);
        try {
            return Files.deleteIfExists(file);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to delete resource pack file " + path + " in " + packId, failure);
        }
    }

    @Override
    public boolean delete(String packId) {
        var id = normalizeId(packId);
        var deletedPack = deleteDirectory(packDirectory(id));
        var deletedBuild = deleteFileIfExists(buildDirectory.resolve(id + ".zip").normalize());
        return deletedPack || deletedBuild;
    }

    @Override
    public ResourcePackBuild build(String packId) {
        var id = normalizeId(packId);
        var lock = buildLocks.computeIfAbsent(id, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return buildLocked(id);
        } finally {
            lock.unlock();
        }
    }

    private ResourcePackBuild buildLocked(String id) {
        var directory = packDirectory(id);
        ensurePackExists(id);
        ensureBuildRoot();
        var output = buildDirectory.resolve(id + ".zip").normalize();
        try {
            var temporary = Files.createTempFile(buildDirectory, id + "-", ".zip");
            try {
                try (var zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                    for (var file : sortedPackFiles(directory)) {
                        var relative = directory.relativize(file).toString().replace('\\', '/');
                        var entry = new ZipEntry(relative);
                        zip.putNextEntry(entry);
                        Files.copy(file, zip);
                        zip.closeEntry();
                    }
                }
                publish(temporary, output);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return new ResourcePackBuild(id, output, sha1(output), Files.size(output));
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to build resource pack " + id, failure);
        }
    }

    private void ensureRoot() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to create managed resource pack root " + rootDirectory, failure);
        }
    }

    private void ensureBuildRoot() {
        ensureRoot();
        try {
            Files.createDirectories(buildDirectory);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to create managed resource pack build root " + buildDirectory, failure);
        }
    }

    private void ensurePackExists(String id) {
        if (!Files.isDirectory(packDirectory(id))) {
            throw new IllegalArgumentException("Unknown managed resource pack: " + id);
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
            throw new IllegalArgumentException("Resource pack path escapes the pack root: " + path);
        }
        return resolved;
    }

    private static java.util.List<Path> sortedPackFiles(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> directory.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }
    }

    private static void publish(Path temporary, Path output) throws IOException {
        try {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
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

    private static boolean deleteFileIfExists(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to delete file " + file, failure);
        }
    }

    private static String normalizeId(String id) {
        return new ResourcePack(id, "", 1).id();
    }

    private static String normalizeRelativePath(String path) {
        return ResourcePack.normalizeRelativePath(path);
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to read file size for " + path, failure);
        }
    }

    private static void writePackMeta(Path directory, String description, int packFormat) throws IOException {
        var pack = new JsonObject();
        var meta = new JsonObject();
        meta.addProperty("description", description);
        meta.addProperty("pack_format", packFormat);
        pack.add("pack", meta);
        Files.writeString(directory.resolve("pack.mcmeta"), ResourcePackService.PRETTY_GSON.toJson(pack) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static int currentResourcePackFormat() {
        try {
            return SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).major();
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

    private static Optional<Integer> readPackFormat(Path directory) {
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
            if (pack == null || !pack.has("pack_format")) {
                return Optional.empty();
            }
            return Optional.of(pack.get("pack_format").getAsInt()).filter(value -> value > 0);
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static String sha1(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-1 digest is not available", failure);
        }
        try (var input = Files.newInputStream(file)) {
            var buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static final class Registration implements ResourcePackRegistration {
        private final FandResourcePackService owner;
        private final String id;
        private volatile boolean active = true;

        private Registration(FandResourcePackService owner, String id) {
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
        public void delete() {
            if (active) {
                active = false;
                owner.delete(id);
            }
        }
    }
}
