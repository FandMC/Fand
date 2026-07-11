package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.fand.server.command.CommandManager;
import io.fand.server.event.EventDispatcher;
import io.fand.server.permission.PermissionManager;
import io.fand.server.scheduler.TaskScheduler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PluginLibraryResolverTest {

    private static final String ROOT_COORDINATE = "com.example:example-library:1.2.3";
    private static final String TRANSITIVE_COORDINATE = "com.example:transitive-library:2.0.0";
    private static final String ROOT_JAR_PATH = artifactPath(ROOT_COORDINATE, "jar");
    private static final String TRANSITIVE_JAR_PATH = artifactPath(TRANSITIVE_COORDINATE, "jar");

    @TempDir
    Path tempDirectory;

    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        servers.forEach(server -> server.stop(0));
    }

    @Test
    void resolvesTransitiveLibrariesVerifiesLockAndReusesRootCache() throws Exception {
        var transitiveJar = PluginRuntimeTestSupport.createJavaJar(
                tempDirectory,
                tempDirectory.resolve("transitive-library.jar"),
                Map.of("com/example/library/TransitiveLibrary.java", """
                        package com.example.library;

                        public final class TransitiveLibrary {
                            public static String value() {
                                return "transitive";
                            }
                        }
                        """),
                List.of()
        );
        var rootJar = PluginRuntimeTestSupport.createJavaJar(
                tempDirectory,
                tempDirectory.resolve("example-library.jar"),
                Map.of("com/example/library/ExampleLibrary.java", """
                        package com.example.library;

                        public final class ExampleLibrary {
                            public static String value() {
                                return "from-" + TransitiveLibrary.value();
                            }
                        }
                        """),
                List.of(transitiveJar)
        );
        var repository = new TestRepository()
                .addArtifact(TRANSITIVE_COORDINATE, transitiveJar, List.of())
                .addArtifact(ROOT_COORDINATE, rootJar, List.of(TRANSITIVE_COORDINATE));
        var repositoryUrl = startRepository(repository);
        var pluginsDirectory = Files.createDirectories(tempDirectory.resolve("plugins"));
        PluginRuntimeTestSupport.createPluginJar(
                tempDirectory,
                pluginsDirectory.resolve("example-plugin.jar"),
                PluginRuntimeTestSupport.descriptorJson(
                        "example-plugin",
                        "testplugins.library.LibraryPlugin",
                        List.of()
                ),
                Map.of("testplugins/library/LibraryPlugin.java", """
                        package testplugins.library;

                        import com.example.library.ExampleLibrary;
                        import io.fand.api.plugin.Plugin;
                        import io.fand.api.plugin.PluginContext;

                        public final class LibraryPlugin implements Plugin {
                            @Override
                            public void onLoad(PluginContext context) {
                                System.setProperty("fand.test.library.value", ExampleLibrary.value());
                            }

                            @Override
                            public void onEnable(PluginContext context) {
                            }
                        }
                        """),
                List.of(rootJar, transitiveJar),
                Map.of(
                        PluginLibraryResolver.DESCRIPTOR_PATH,
                        librariesJson(
                                List.of(repositoryUrl),
                                List.of(ROOT_COORDINATE),
                                Map.of(
                                        ROOT_COORDINATE, sha256(rootJar),
                                        TRANSITIVE_COORDINATE, sha256(transitiveJar)
                                )
                        )
                )
        );

        var previousValue = System.getProperty("fand.test.library.value");
        try {
            loadPlugins(pluginsDirectory);
            assertThat(System.getProperty("fand.test.library.value")).isEqualTo("from-transitive");
            assertThat(repository.requests(ROOT_JAR_PATH)).hasValue(1);
            assertThat(repository.requests(TRANSITIVE_JAR_PATH)).hasValue(1);
            assertThat(tempDirectory.resolve("libraries").resolve(ROOT_JAR_PATH.substring(1))).isRegularFile();
            assertThat(tempDirectory.resolve("libraries").resolve(TRANSITIVE_JAR_PATH.substring(1))).isRegularFile();

            servers.removeFirst().stop(0);
            System.clearProperty("fand.test.library.value");
            loadPlugins(pluginsDirectory);
            assertThat(System.getProperty("fand.test.library.value")).isEqualTo("from-transitive");
            assertThat(repository.requests(ROOT_JAR_PATH)).hasValue(1);
            assertThat(repository.requests(TRANSITIVE_JAR_PATH)).hasValue(1);
        } finally {
            PluginRuntimeTestSupport.restoreProperty("fand.test.library.value", previousValue);
        }
    }

    @Test
    void fallsBackToTheNextRepository() throws IOException {
        var libraryJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("fallback-library.jar"),
                Map.of("marker.txt", "valid")
        );
        var firstRepository = new TestRepository();
        var firstUrl = startRepository(firstRepository);
        var secondRepository = new TestRepository().addArtifact(ROOT_COORDINATE, libraryJar, List.of());
        var secondUrl = startRepository(secondRepository);
        var pluginJar = pluginJar(librariesJson(
                List.of(firstUrl, secondUrl),
                List.of(ROOT_COORDINATE),
                Map.of()
        ));

        var resolved = resolve(tempDirectory.resolve("cache"), pluginJar);

        assertThat(resolved).hasSize(1);
        assertThat(firstRepository.totalRequests()).isPositive();
        assertThat(secondRepository.requests(ROOT_JAR_PATH)).hasValue(1);
    }

    @Test
    void ignoresRepositoriesDeclaredByDependencyPom() throws IOException {
        var transitiveJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("injected-transitive.jar"),
                Map.of("transitive.txt", "valid")
        );
        var injectedRepository = new TestRepository()
                .addArtifact(TRANSITIVE_COORDINATE, transitiveJar, List.of());
        var injectedUrl = startRepository(injectedRepository);
        var rootJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("injected-root.jar"),
                Map.of("root.txt", "valid")
        );
        var rootPom = pom(ROOT_COORDINATE, List.of(TRANSITIVE_COORDINATE)).replace(
                "  <dependencies>",
                """
                  <repositories>
                    <repository>
                      <id>injected</id>
                      <url>%s</url>
                    </repository>
                  </repositories>
                  <dependencies>""".formatted(injectedUrl)
        );
        var declaredRepository = new TestRepository().addArtifact(ROOT_COORDINATE, rootJar, rootPom);
        var declaredUrl = startRepository(declaredRepository);
        var pluginJar = pluginJar(librariesJson(
                List.of(declaredUrl),
                List.of(ROOT_COORDINATE),
                Map.of()
        ));

        assertThatThrownBy(() -> resolve(tempDirectory.resolve("repository-policy-cache"), pluginJar))
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("Failed to resolve plugin libraries");
        assertThat(injectedRepository.totalRequests()).isZero();
    }

    @Test
    void retriesTransientRepositoryFailures() throws IOException {
        var libraryJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("retry-library.jar"),
                Map.of("marker.txt", "valid")
        );
        var repository = new TestRepository()
                .addArtifact(ROOT_COORDINATE, libraryJar, List.of())
                .failFirst(ROOT_JAR_PATH, 2);
        var repositoryUrl = startRepository(repository);
        var pluginJar = pluginJar(librariesJson(
                List.of(repositoryUrl),
                List.of(ROOT_COORDINATE),
                Map.of()
        ));

        var resolved = resolve(tempDirectory.resolve("retry-cache"), pluginJar);

        assertThat(resolved).hasSize(1);
        assertThat(repository.requests(ROOT_JAR_PATH)).hasValue(3);
    }

    @Test
    void mediatesMultipleVersionsWithinOnePlugin() throws IOException {
        var firstCoordinate = "com.example:conflict-library:1.0.0";
        var secondCoordinate = "com.example:conflict-library:2.0.0";
        var firstJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("conflict-1.jar"),
                Map.of("version.txt", "1")
        );
        var secondJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("conflict-2.jar"),
                Map.of("version.txt", "2")
        );
        var repository = new TestRepository()
                .addArtifact(firstCoordinate, firstJar, List.of())
                .addArtifact(secondCoordinate, secondJar, List.of());
        var repositoryUrl = startRepository(repository);
        var pluginJar = pluginJar(librariesJson(
                List.of(repositoryUrl),
                List.of(firstCoordinate, secondCoordinate),
                Map.of()
        ));

        var resolved = resolve(tempDirectory.resolve("conflict-cache"), pluginJar);

        assertThat(resolved).singleElement().satisfies(path ->
                assertThat(path.getFileName().toString()).isIn("conflict-library-1.0.0.jar", "conflict-library-2.0.0.jar"));
    }

    @Test
    void rejectsIncompleteSha256Lock() throws IOException {
        var transitiveJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("locked-transitive.jar"),
                Map.of("transitive.txt", "valid")
        );
        var rootJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("locked-root.jar"),
                Map.of("root.txt", "valid")
        );
        var repository = new TestRepository()
                .addArtifact(TRANSITIVE_COORDINATE, transitiveJar, List.of())
                .addArtifact(ROOT_COORDINATE, rootJar, List.of(TRANSITIVE_COORDINATE));
        var repositoryUrl = startRepository(repository);
        var pluginJar = pluginJar(librariesJson(
                List.of(repositoryUrl),
                List.of(ROOT_COORDINATE),
                Map.of(ROOT_COORDINATE, sha256(rootJar))
        ));

        assertThatThrownBy(() -> resolve(tempDirectory.resolve("lock-cache"), pluginJar))
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("SHA-256 lock is missing resolved artifact")
                .hasMessageContaining(TRANSITIVE_COORDINATE);
    }

    @Test
    void returnsNoLibrariesWhenDescriptorIsMissing() throws IOException {
        var pluginJar = PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("plugin.jar"),
                Map.of("fand-plugin.json", "{}")
        );

        assertThat(resolve(tempDirectory.resolve("cache"), pluginJar)).isEmpty();
    }

    @Test
    void rejectsInvalidCoordinatesBeforeDownloading() throws IOException {
        var repository = new TestRepository();
        var repositoryUrl = startRepository(repository);
        var pluginJar = pluginJar(librariesJson(
                List.of(repositoryUrl),
                List.of("com.example:../escape:1.0"),
                Map.of()
        ));

        assertThatThrownBy(() -> resolve(tempDirectory.resolve("cache"), pluginJar))
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("invalid library coordinate");
        assertThat(repository.totalRequests()).isZero();
    }

    @Test
    void rejectsInvalidDownloadedJarWithoutCachingIt() throws IOException {
        var invalidJar = tempDirectory.resolve("invalid.jar");
        Files.writeString(invalidJar, "not a jar", StandardCharsets.UTF_8);
        var repository = new TestRepository().addArtifact(ROOT_COORDINATE, invalidJar, List.of());
        var repositoryUrl = startRepository(repository);
        var pluginJar = pluginJar(librariesJson(
                List.of(repositoryUrl),
                List.of(ROOT_COORDINATE),
                Map.of()
        ));
        var cacheDirectory = tempDirectory.resolve("invalid-cache");

        assertThatThrownBy(() -> resolve(cacheDirectory, pluginJar))
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("not a valid jar");
        assertThat(cacheDirectory.resolve(ROOT_JAR_PATH.substring(1))).doesNotExist();
    }

    private Path pluginJar(String librariesJson) throws IOException {
        return PluginRuntimeTestSupport.createJarWithEntries(
                tempDirectory.resolve("plugin-" + System.nanoTime() + ".jar"),
                Map.of(PluginLibraryResolver.DESCRIPTOR_PATH, librariesJson)
        );
    }

    private static List<Path> resolve(Path cacheDirectory, Path pluginJar) {
        try (var resolver = new PluginLibraryResolver(cacheDirectory)) {
            return resolver.resolve(pluginJar, "example-plugin");
        }
    }

    private String startRepository(TestRepository repository) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", repository::handle);
        server.start();
        servers.add(server);
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    private void loadPlugins(Path pluginsDirectory) {
        var runtime = new PluginRuntime(
                pluginsDirectory,
                pluginsDirectory,
                getClass().getClassLoader(),
                new CommandManager(),
                new EventDispatcher(),
                new PermissionManager(),
                new TaskScheduler()
        );
        try {
            runtime.loadPlugins();
        } finally {
            runtime.close();
        }
    }

    private static String librariesJson(
            List<String> repositories,
            List<String> libraries,
            Map<String, String> sha256
    ) {
        var descriptor = new LinkedHashMap<String, Object>();
        descriptor.put("repositories", repositories);
        descriptor.put("libraries", libraries);
        if (!sha256.isEmpty()) {
            descriptor.put("sha256", sha256);
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(descriptor);
    }

    private static String artifactPath(String coordinate, String extension) {
        var parts = coordinate.split(":", -1);
        return "/" + parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2]
                + "/" + parts[1] + "-" + parts[2] + "." + extension;
    }

    private static String pom(String coordinate, List<String> dependencies) {
        var parts = coordinate.split(":", -1);
        var dependencyXml = new StringBuilder();
        for (var dependency : dependencies) {
            var dependencyParts = dependency.split(":", -1);
            dependencyXml.append("""
                    <dependency>
                      <groupId>%s</groupId>
                      <artifactId>%s</artifactId>
                      <version>%s</version>
                    </dependency>
                    """.formatted(dependencyParts[0], dependencyParts[1], dependencyParts[2]));
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                  <dependencies>
                %s  </dependencies>
                </project>
                """.formatted(parts[0], parts[1], parts[2], dependencyXml);
    }

    private static String sha256(Path path) {
        try {
            return digest("SHA-256", Files.readAllBytes(path));
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String digest(String algorithm, byte[] value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(value));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        try (exchange) {
            if (exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
                exchange.sendResponseHeaders(status, -1L);
                return;
            }
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private static final class TestRepository {

        private final Map<String, byte[]> resources = new LinkedHashMap<>();
        private final Map<String, AtomicInteger> requests = new java.util.concurrent.ConcurrentHashMap<>();
        private final Map<String, Integer> failuresBeforeSuccess = new java.util.concurrent.ConcurrentHashMap<>();

        private TestRepository addArtifact(String coordinate, Path jar, List<String> dependencies) throws IOException {
            return addArtifact(coordinate, jar, pom(coordinate, dependencies));
        }

        private TestRepository addArtifact(String coordinate, Path jar, String pom) throws IOException {
            addResource(artifactPath(coordinate, "jar"), Files.readAllBytes(jar));
            addResource(artifactPath(coordinate, "pom"), pom.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        private TestRepository failFirst(String path, int failures) {
            failuresBeforeSuccess.put(path, failures);
            return this;
        }

        private void addResource(String path, byte[] body) {
            resources.put(path, body);
            resources.put(path + ".sha1", digest("SHA-1", body).getBytes(StandardCharsets.US_ASCII));
            resources.put(path + ".sha256", digest("SHA-256", body).getBytes(StandardCharsets.US_ASCII));
        }

        private void handle(HttpExchange exchange) throws IOException {
            var path = exchange.getRequestURI().getPath();
            var count = requests.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();
            if (count <= failuresBeforeSuccess.getOrDefault(path, 0)) {
                respond(exchange, 503, "unavailable".getBytes(StandardCharsets.US_ASCII));
                return;
            }
            var body = resources.get(path);
            if (body == null) {
                respond(exchange, 404, new byte[0]);
                return;
            }
            respond(exchange, 200, body);
        }

        private AtomicInteger requests(String path) {
            return requests.computeIfAbsent(path, ignored -> new AtomicInteger());
        }

        private int totalRequests() {
            return requests.values().stream().mapToInt(AtomicInteger::get).sum();
        }
    }
}
