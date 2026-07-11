package io.fand.server.resourcepack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandResourcePackServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createsPackMetadataWritesFilesAndBuildsZip() throws Exception {
        var service = new FandResourcePackService(tempDir.resolve("resourcepacks"));

        var registration = service.create("demo", "Demo resources", 42);
        service.writeText("demo", "assets/demo/lang/zh_cn.json", "{\"hello\":\"你好\"}");
        var build = service.build("demo");

        assertThat(registration.active()).isTrue();
        assertThat(service.pack("demo")).isPresent()
                .get()
                .satisfies(pack -> {
                    assertThat(pack.description()).isEqualTo("Demo resources");
                    assertThat(pack.packFormat()).isEqualTo(42);
                });
        assertThat(Files.readString(tempDir.resolve("resourcepacks/demo/pack.mcmeta"), StandardCharsets.UTF_8))
                .contains("\"description\": \"Demo resources\"")
                .contains("\"pack_format\": 42");
        assertThat(build.sha1()).matches("[0-9a-f]{40}");
        assertThat(build.size()).isPositive();
        try (var zip = new ZipFile(build.file().toFile())) {
            assertThat(zip.getEntry("pack.mcmeta")).isNotNull();
            assertThat(zip.getEntry("assets/demo/lang/zh_cn.json")).isNotNull();
        }
    }

    @Test
    void rejectsEscapingFilePaths() {
        var service = new FandResourcePackService(tempDir.resolve("resourcepacks"));
        service.create("demo", "Demo resources", 42);

        assertThatThrownBy(() -> service.writeText("demo", "../server.properties", "oops"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes");
    }

    @Test
    void buildsSamePackConcurrently() throws Exception {
        var service = new FandResourcePackService(tempDir.resolve("resourcepacks"));
        service.create("demo", "Demo resources", 42);
        service.writeText("demo", "assets/demo/lang/zh_cn.json", "{\"hello\":\"你好\"}");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            var builds = IntStream.range(0, 12)
                    .mapToObj(ignored -> CompletableFuture.supplyAsync(() -> service.build("demo"), executor))
                    .toList();
            CompletableFuture.allOf(builds.toArray(CompletableFuture[]::new)).join();

            var completed = builds.stream().map(CompletableFuture::join).toList();
            assertThat(completed.stream().map(build -> build.sha1()).distinct()).hasSize(1);
            try (var zip = new ZipFile(completed.getFirst().file().toFile())) {
                assertThat(zip.getEntry("pack.mcmeta")).isNotNull();
                assertThat(zip.getEntry("assets/demo/lang/zh_cn.json")).isNotNull();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
