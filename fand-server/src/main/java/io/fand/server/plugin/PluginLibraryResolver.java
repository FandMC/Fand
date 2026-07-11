package io.fand.server.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PluginLibraryResolver implements AutoCloseable {

    static final String DESCRIPTOR_PATH = "libraries.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginLibraryResolver.class);
    private static final Gson GSON = new Gson();
    private static final long MAX_LIBRARY_BYTES = 256L * 1024L * 1024L;
    private static final Pattern GROUP = Pattern.compile("[A-Za-z0-9_]+(?:[.-][A-Za-z0-9_]+)*");
    private static final Pattern PATH_SEGMENT = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.+-]*");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-fA-F]{64}");

    private final Path repositoryDirectory;
    private final Map<ModuleId, Map<String, String>> versionsByPlugin = new HashMap<>();
    private final Map<ModuleId, Set<String>> reportedVersionSets = new HashMap<>();
    private RepositorySystem repositorySystem;

    PluginLibraryResolver(Path repositoryDirectory) {
        this.repositoryDirectory = Objects.requireNonNull(repositoryDirectory, "repositoryDirectory")
                .toAbsolutePath()
                .normalize();
    }

    synchronized List<Path> resolve(Path pluginJar, String pluginId) {
        var descriptor = readDescriptor(pluginJar);
        if (descriptor == null) {
            return List.of();
        }
        if (descriptor.libraries == null || descriptor.libraries.isEmpty()) {
            if (descriptor.sha256 != null && !descriptor.sha256.isEmpty()) {
                throw new PluginLoadException("Plugin jar " + pluginJar
                        + " declares a SHA-256 lock without any libraries");
            }
            return List.of();
        }

        var dependencies = validateDependencies(pluginJar, descriptor.libraries);
        var repositories = validateRepositories(pluginJar, descriptor.repositories);
        var sha256 = validateSha256(pluginJar, descriptor.sha256);
        var system = repositorySystem();
        var session = createSession(system, pluginId);
        var request = new CollectRequest((Dependency) null, dependencies, repositories);
        var filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);

        final List<ArtifactResult> results;
        try {
            results = system.resolveDependencies(session, new DependencyRequest(request, filter))
                    .getArtifactResults();
        } catch (DependencyResolutionException failure) {
            throw new PluginLoadException("Failed to resolve plugin libraries for '" + pluginId + "': "
                    + rootMessage(failure), failure);
        }

        var artifacts = collectArtifacts(pluginJar, results);
        verifyArtifacts(pluginJar, artifacts, sha256);
        recordVersions(pluginId, artifacts);
        return artifacts.stream().map(ResolvedArtifact::path).toList();
    }

    @Override
    public synchronized void close() {
        if (repositorySystem != null) {
            repositorySystem.shutdown();
            repositorySystem = null;
        }
    }

    private LibraryFile readDescriptor(Path pluginJar) {
        try (var jar = new JarFile(pluginJar.toFile())) {
            var entry = jar.getJarEntry(DESCRIPTOR_PATH);
            if (entry == null) {
                return null;
            }
            try (var reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                var descriptor = GSON.fromJson(reader, LibraryFile.class);
                if (descriptor == null) {
                    throw new PluginLoadException("Plugin jar " + pluginJar + " contains an empty " + DESCRIPTOR_PATH);
                }
                return descriptor;
            }
        } catch (IOException | JsonParseException failure) {
            throw new PluginLoadException("Failed to read " + DESCRIPTOR_PATH + " from " + pluginJar, failure);
        }
    }

    private static List<Dependency> validateDependencies(Path pluginJar, List<String> values) {
        var dependencies = new LinkedHashMap<String, Dependency>();
        for (var value : values) {
            var coordinate = parseCoordinate(pluginJar, value, "library");
            dependencies.putIfAbsent(coordinate, new Dependency(new DefaultArtifact(coordinate), JavaScopes.RUNTIME));
        }
        return List.copyOf(dependencies.values());
    }

    private static List<RemoteRepository> validateRepositories(Path pluginJar, List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new PluginLoadException("Plugin jar " + pluginJar + " declares libraries without repositories");
        }
        var policy = new RepositoryPolicy(
                true,
                RepositoryPolicy.UPDATE_POLICY_DAILY,
                RepositoryPolicy.CHECKSUM_POLICY_FAIL
        );
        var repositories = new ArrayList<RemoteRepository>(values.size());
        for (var index = 0; index < values.size(); index++) {
            var value = values.get(index);
            if (value == null || value.isBlank()) {
                throw new PluginLoadException("Plugin jar " + pluginJar + " declares a blank library repository");
            }
            final URI repository;
            try {
                var normalized = value.trim();
                repository = URI.create(normalized.endsWith("/") ? normalized : normalized + "/");
            } catch (IllegalArgumentException failure) {
                throw new PluginLoadException("Plugin jar " + pluginJar
                        + " declares invalid library repository '" + value + "'", failure);
            }
            var scheme = repository.getScheme() == null ? "" : repository.getScheme().toLowerCase(Locale.ROOT);
            if (!repository.isAbsolute() || (!scheme.equals("https") && !scheme.equals("http"))
                    || repository.getHost() == null || repository.getUserInfo() != null
                    || repository.getQuery() != null || repository.getFragment() != null) {
                throw new PluginLoadException("Plugin jar " + pluginJar
                        + " library repository must be an absolute HTTP(S) URL without credentials, query, or fragment: " + value);
            }
            repositories.add(new RemoteRepository.Builder("fand-plugin-" + index, "default", repository.toString())
                    .setReleasePolicy(policy)
                    .setSnapshotPolicy(policy)
                    .build());
        }
        return List.copyOf(repositories);
    }

    private static Map<String, String> validateSha256(Path pluginJar, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        var checksums = new LinkedHashMap<String, String>();
        for (var entry : values.entrySet()) {
            var coordinate = parseArtifactCoordinate(pluginJar, entry.getKey(), "sha256 key");
            var value = entry.getValue();
            if (value == null || !SHA_256.matcher(value.trim()).matches()) {
                throw new PluginLoadException("Plugin jar " + pluginJar
                        + " has invalid SHA-256 for '" + entry.getKey() + "'");
            }
            var previous = checksums.put(artifactKey(new DefaultArtifact(coordinate)), value.trim().toLowerCase(Locale.ROOT));
            if (previous != null) {
                throw new PluginLoadException("Plugin jar " + pluginJar
                        + " declares duplicate SHA-256 coordinate '" + entry.getKey() + "'");
            }
        }
        return Map.copyOf(checksums);
    }

    private static String parseCoordinate(Path pluginJar, String value, String field) {
        if (value == null) {
            throw new PluginLoadException("Plugin jar " + pluginJar + " declares a null " + field + " coordinate");
        }
        var coordinate = value.trim();
        var parts = coordinate.split(":", -1);
        if (parts.length != 3 || !GROUP.matcher(parts[0]).matches()
                || !PATH_SEGMENT.matcher(parts[1]).matches() || !PATH_SEGMENT.matcher(parts[2]).matches()) {
            throw new PluginLoadException("Plugin jar " + pluginJar + " has invalid " + field
                    + " coordinate '" + value + "' (expected group:artifact:version)");
        }
        return coordinate;
    }

    private static String parseArtifactCoordinate(Path pluginJar, String value, String field) {
        if (value == null) {
            throw new PluginLoadException("Plugin jar " + pluginJar + " declares a null " + field);
        }
        var coordinate = value.trim();
        var parts = coordinate.split(":", -1);
        var valid = (parts.length == 3 || parts.length == 4 || parts.length == 5)
                && GROUP.matcher(parts[0]).matches();
        for (var index = 1; valid && index < parts.length; index++) {
            valid = PATH_SEGMENT.matcher(parts[index]).matches();
        }
        if (!valid) {
            throw new PluginLoadException("Plugin jar " + pluginJar + " has invalid " + field
                    + " '" + value + "' (expected Maven artifact coordinates)");
        }
        try {
            new DefaultArtifact(coordinate);
        } catch (IllegalArgumentException failure) {
            throw new PluginLoadException("Plugin jar " + pluginJar + " has invalid " + field
                    + " '" + value + "'", failure);
        }
        return coordinate;
    }

    private DefaultRepositorySystemSession createSession(RepositorySystem system, String pluginId) {
        var session = MavenRepositorySystemUtils.newSession();
        session.setSystemProperties(System.getProperties());
        session.setIgnoreArtifactDescriptorRepositories(true);
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setConfigProperty(ConfigurationProperties.USER_AGENT, "Fand-Plugin-Library-Resolver");
        session.setConfigProperty(ConfigurationProperties.CONNECT_TIMEOUT, 15_000);
        session.setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT, 60_000);
        session.setConfigProperty(ConfigurationProperties.HTTP_RETRY_HANDLER_COUNT, 3);
        session.setConfigProperty(ConfigurationProperties.HTTP_RETRY_HANDLER_INTERVAL, 1_000L);
        session.setConfigProperty(ConfigurationProperties.HTTP_RETRY_HANDLER_INTERVAL_MAX, 10_000L);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(
                session,
                new LocalRepository(repositoryDirectory.toFile())
        ));
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferInitiated(TransferEvent event) {
                var resource = event.getResource();
                if (resource.getResourceName().endsWith(".jar")) {
                    LOGGER.info("Downloading plugin library for {} from {}{}",
                            pluginId, resource.getRepositoryUrl(), resource.getResourceName());
                }
            }

            @Override
            public void transferProgressed(TransferEvent event) throws TransferCancelledException {
                if (event.getResource().getResourceName().endsWith(".jar")
                        && event.getTransferredBytes() > MAX_LIBRARY_BYTES) {
                    throw new TransferCancelledException("Plugin library exceeds " + MAX_LIBRARY_BYTES + " bytes");
                }
            }
        });
        session.setReadOnly();
        return session;
    }

    private List<ResolvedArtifact> collectArtifacts(Path pluginJar, List<ArtifactResult> results) {
        var artifacts = new LinkedHashMap<Path, ResolvedArtifact>();
        for (var result : results) {
            var artifact = result.getArtifact();
            if (artifact == null || artifact.getFile() == null) {
                throw new PluginLoadException("Plugin jar " + pluginJar + " resolved a library without a file");
            }
            var path = artifact.getFile().toPath().toAbsolutePath().normalize();
            if (!path.startsWith(repositoryDirectory)) {
                throw new PluginLoadException("Resolved plugin library escapes repository directory: " + path);
            }
            try {
                if (Files.size(path) > MAX_LIBRARY_BYTES) {
                    discardInvalidArtifact(path);
                    throw new PluginLoadException("Resolved plugin library exceeds " + MAX_LIBRARY_BYTES + " bytes: " + path);
                }
            } catch (IOException failure) {
                throw new PluginLoadException("Failed to inspect resolved plugin library " + path, failure);
            }
            if (!validJar(path)) {
                discardInvalidArtifact(path);
                throw new PluginLoadException("Resolved plugin library is not a valid jar: " + path);
            }
            artifacts.putIfAbsent(path, new ResolvedArtifact(
                    artifactKey(artifact),
                    path,
                    new ModuleId(artifact.getGroupId(), artifact.getArtifactId()),
                    artifact.getVersion()
            ));
        }
        return List.copyOf(artifacts.values());
    }

    private void verifyArtifacts(Path pluginJar, List<ResolvedArtifact> artifacts, Map<String, String> checksums) {
        var remaining = new LinkedHashSet<>(checksums.keySet());
        for (var artifact : artifacts) {
            var actual = sha256(artifact.path);
            if (!checksums.isEmpty()) {
                var expected = checksums.get(artifact.key);
                if (expected == null) {
                    throw new PluginLoadException("Plugin jar " + pluginJar
                            + " SHA-256 lock is missing resolved artifact '" + artifact.key + "'");
                }
                if (!actual.equals(expected)) {
                    throw new PluginLoadException("Plugin jar " + pluginJar + " SHA-256 mismatch for '"
                            + artifact.key + "' (expected " + expected + ", got " + actual + ")");
                }
                remaining.remove(artifact.key);
            } else {
                verifyLocalSha256(artifact.path, actual);
            }
            writeLocalSha256(artifact.path, actual);
        }
        if (!remaining.isEmpty()) {
            throw new PluginLoadException("Plugin jar " + pluginJar
                    + " SHA-256 lock contains unresolved artifacts: " + String.join(", ", remaining));
        }
    }

    private static void verifyLocalSha256(Path artifact, String actual) {
        var sidecar = sha256Sidecar(artifact);
        if (!Files.isRegularFile(sidecar)) {
            return;
        }
        try {
            var expected = Files.readString(sidecar, StandardCharsets.US_ASCII).trim().split("\\s+", 2)[0];
            if (!SHA_256.matcher(expected).matches() || !actual.equalsIgnoreCase(expected)) {
                throw new PluginLoadException("Cached plugin library SHA-256 mismatch for " + artifact);
            }
        } catch (IOException failure) {
            throw new PluginLoadException("Failed to read plugin library SHA-256 for " + artifact, failure);
        }
    }

    private static void writeLocalSha256(Path artifact, String checksum) {
        var sidecar = sha256Sidecar(artifact);
        try {
            if (Files.isRegularFile(sidecar)) {
                var current = Files.readString(sidecar, StandardCharsets.US_ASCII).trim().split("\\s+", 2)[0];
                if (current.equalsIgnoreCase(checksum)) {
                    return;
                }
            }
            Files.createDirectories(sidecar.getParent());
            var temporary = Files.createTempFile(sidecar.getParent(), sidecar.getFileName().toString() + ".", ".part");
            try {
                Files.writeString(temporary, checksum + System.lineSeparator(), StandardCharsets.US_ASCII);
                try {
                    Files.move(temporary, sidecar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, sidecar, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException failure) {
            throw new PluginLoadException("Failed to write plugin library SHA-256 for " + artifact, failure);
        }
    }

    private void recordVersions(String pluginId, List<ResolvedArtifact> artifacts) {
        for (var artifact : artifacts) {
            var plugins = versionsByPlugin.computeIfAbsent(artifact.module, ignored -> new LinkedHashMap<>());
            plugins.put(pluginId, artifact.version);
            var versions = new TreeSet<>(plugins.values());
            if (versions.size() < 2) {
                continue;
            }
            var reported = reportedVersionSets.computeIfAbsent(artifact.module, ignored -> new HashSet<>());
            var signature = String.join(",", versions);
            if (reported.add(signature)) {
                LOGGER.warn("Plugin library {} uses isolated versions {} across plugins {}; "
                                + "Fand keeps them separate to preserve binary compatibility",
                        artifact.module, versions, plugins);
            }
        }
    }

    private static RepositorySystem createRepositorySystem() {
        return new RepositorySystemSupplier().get();
    }

    private RepositorySystem repositorySystem() {
        if (repositorySystem == null) {
            repositorySystem = createRepositorySystem();
        }
        return repositorySystem;
    }

    private static String artifactKey(Artifact artifact) {
        var base = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":";
        var classifier = artifact.getClassifier();
        if ("jar".equals(artifact.getExtension()) && (classifier == null || classifier.isEmpty())) {
            return base + artifact.getVersion();
        }
        if (classifier == null || classifier.isEmpty()) {
            return base + artifact.getExtension() + ":" + artifact.getVersion();
        }
        return base + artifact.getExtension() + ":" + classifier + ":" + artifact.getVersion();
    }

    private static String sha256(Path path) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
        try (var input = Files.newInputStream(path)) {
            var buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException failure) {
            throw new PluginLoadException("Failed to calculate SHA-256 for " + path, failure);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Path sha256Sidecar(Path artifact) {
        return artifact.resolveSibling(artifact.getFileName().toString() + ".sha256");
    }

    private static void discardInvalidArtifact(Path artifact) {
        try {
            Files.deleteIfExists(artifact);
            Files.deleteIfExists(artifact.resolveSibling(artifact.getFileName().toString() + ".sha1"));
            Files.deleteIfExists(artifact.resolveSibling(artifact.getFileName().toString() + ".md5"));
            Files.deleteIfExists(sha256Sidecar(artifact));
        } catch (IOException failure) {
            throw new PluginLoadException("Failed to remove invalid plugin library " + artifact, failure);
        }
    }

    private static boolean validJar(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try (var ignored = new JarFile(path.toFile())) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String rootMessage(Throwable failure) {
        var cursor = failure;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private record LibraryFile(List<String> repositories, List<String> libraries, Map<String, String> sha256) {
    }

    private record ResolvedArtifact(String key, Path path, ModuleId module, String version) {
    }

    private record ModuleId(String group, String artifact) {
        @Override
        public String toString() {
            return group + ":" + artifact;
        }
    }
}
