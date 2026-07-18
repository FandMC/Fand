package io.fand.server.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.fand.api.resourcepack.ResourcePackBuild;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResourcePackHttpHost implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackHttpHost.class);

    private final Path hostedDirectory;
    private final boolean enabled;
    private final String bindAddress;
    private final int configuredPort;
    private final String publicBaseUrl;
    private final Supplier<String> fallbackHost;
    private final ConcurrentHashMap<String, Path> routes = new ConcurrentHashMap<>();
    private HttpServer server;
    private ExecutorService executor;

    ResourcePackHttpHost(
            Path hostedDirectory,
            boolean enabled,
            String bindAddress,
            int configuredPort,
            String publicBaseUrl,
            Supplier<String> fallbackHost
    ) {
        this.hostedDirectory = Objects.requireNonNull(hostedDirectory, "hostedDirectory").toAbsolutePath().normalize();
        this.enabled = enabled;
        this.bindAddress = Objects.requireNonNull(bindAddress, "bindAddress").trim();
        this.configuredPort = configuredPort;
        this.publicBaseUrl = trimTrailingSlash(Objects.requireNonNull(publicBaseUrl, "publicBaseUrl").trim());
        this.fallbackHost = Objects.requireNonNull(fallbackHost, "fallbackHost");
    }

    boolean enabled() {
        return enabled;
    }

    synchronized String publish(ResourcePackBuild build) {
        Objects.requireNonNull(build, "build");
        if (!enabled) {
            throw new IllegalStateException("Automatic resource-pack hosting is disabled");
        }
        try {
            var packDirectory = hostedDirectory.resolve(build.packId()).normalize();
            if (!packDirectory.startsWith(hostedDirectory)) {
                throw new IllegalArgumentException("Invalid hosted resource-pack path: " + build.packId());
            }
            Files.createDirectories(packDirectory);
            var immutable = packDirectory.resolve(build.sha1() + ".zip").normalize();
            if (!immutable.startsWith(hostedDirectory)) {
                throw new IllegalArgumentException("Invalid hosted resource-pack path: " + build.packId());
            }
            if (!Files.isRegularFile(immutable) || Files.size(immutable) != build.size()) {
                var temporary = Files.createTempFile(packDirectory, "resource-pack-", ".zip.tmp");
                try {
                    Files.copy(build.file(), temporary, StandardCopyOption.REPLACE_EXISTING);
                    publish(temporary, immutable);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            var route = route(build);
            routes.put(route, immutable);
            ensureStarted();
            return baseUrl() + route;
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to host resource pack " + build.packId(), failure);
        }
    }

    synchronized void remove(String packId) {
        var routePrefix = "/resourcepacks/" + packId + "/";
        routes.keySet().removeIf(route -> route.startsWith(routePrefix));
        deleteDirectory(hostedDirectory.resolve(packId).normalize(), packId);
    }

    @Override
    public synchronized void close() {
        routes.clear();
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void ensureStarted() throws IOException {
        if (server != null) {
            return;
        }
        var created = HttpServer.create(new InetSocketAddress(bindAddress, configuredPort), 0);
        var threadCounter = new AtomicInteger();
        var createdExecutor = Executors.newFixedThreadPool(2, task -> {
            var thread = new Thread(task, "Fand Resource Pack HTTP-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        created.createContext("/resourcepacks/", this::handle);
        created.setExecutor(createdExecutor);
        try {
            created.start();
        } catch (RuntimeException failure) {
            created.stop(0);
            createdExecutor.shutdownNow();
            throw failure;
        }
        server = created;
        executor = createdExecutor;
        LOGGER.info("Resource-pack HTTP host listening on {}:{}", bindAddress, created.getAddress().getPort());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            var method = exchange.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            var file = routes.get(exchange.getRequestURI().getRawPath());
            if (file == null || !Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var size = Files.size(file);
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            if (method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Content-Length", Long.toString(size));
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, size);
            try (var input = Files.newInputStream(file); var output = exchange.getResponseBody()) {
                input.transferTo(output);
            }
        }
    }

    private String baseUrl() {
        if (!publicBaseUrl.isEmpty()) {
            return publicBaseUrl;
        }
        var host = Objects.requireNonNullElse(fallbackHost.get(), "").trim();
        if (host.isEmpty() || host.equals("0.0.0.0") || host.equals("::")) {
            host = "127.0.0.1";
        }
        if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        return "http://" + host + ":" + server.getAddress().getPort();
    }

    private static String route(ResourcePackBuild build) {
        return "/resourcepacks/" + build.packId() + "/" + build.sha1() + ".zip";
    }

    private static String trimTrailingSlash(String value) {
        var result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static void publish(Path temporary, Path output) throws IOException {
        try {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteDirectory(Path directory, String packId) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var files = Files.walk(directory)) {
            for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to remove hosted resource pack " + packId, failure);
        }
    }
}
