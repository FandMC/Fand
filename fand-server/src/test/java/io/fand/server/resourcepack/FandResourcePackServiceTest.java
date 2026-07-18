package io.fand.server.resourcepack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.block.BlockType;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.item.ItemType;
import io.fand.api.item.custom.CustomItemType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.zip.ZipFile;
import net.kyori.adventure.key.Key;
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

    @Test
    void hostsBuiltPackAndCreatesVanillaRequestWithoutExternalUrl() throws Exception {
        try (var service = new FandResourcePackService(tempDir.resolve("resourcepacks"))) {
            service.create("demo", "Demo resources", 42);
            service.writeText("demo", "assets/demo/lang/en_us.json", "{\"hello\":\"world\"}");

            var request = service.request("demo", true, null);
            var response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(request.url())).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            assertThat(request.required()).isTrue();
            assertThat(request.hash()).matches("[0-9a-f]{40}");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type")).contains("application/zip");
            assertThat(response.body()).isEqualTo(Files.readAllBytes(service.buildDirectory().resolve("demo.zip")));
        }
    }

    @Test
    void generatesModernItemAndBlockModels() throws Exception {
        var service = new FandResourcePackService(tempDir.resolve("resourcepacks"));
        service.create("content", "Custom content", 42);
        var models = service.models("content");
        var ruby = CustomItemType.of(Key.key("demo:ruby"), new TestItemType(Key.key("minecraft:paper"), 64));
        var rubyBlock = new CustomBlockType(Key.key("demo:ruby_block"), new TestBlockType(Key.key("minecraft:stone")));
        var rubyBlockItem = CustomItemType.of(
                Key.key("demo:ruby_block"),
                new TestItemType(Key.key("minecraft:paper"), 64));
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        models.flatItem(ruby, Key.key("demo:item/ruby"));
        models.cubeAllBlock(rubyBlock, Key.key("demo:block/ruby_block"));
        models.blockItem(rubyBlockItem, rubyBlock);
        models.texture(Key.key("demo:item/ruby"), png);

        var root = tempDir.resolve("resourcepacks/content/assets/demo");
        assertThat(Files.readString(root.resolve("items/ruby.json"), StandardCharsets.UTF_8))
                .contains("demo:item/ruby");
        assertThat(Files.readString(root.resolve("models/item/ruby.json"), StandardCharsets.UTF_8))
                .contains("minecraft:item/generated")
                .contains("demo:item/ruby");
        assertThat(Files.readString(root.resolve("models/block/ruby_block.json"), StandardCharsets.UTF_8))
                .contains("minecraft:block/cube_all")
                .contains("demo:block/ruby_block");
        assertThat(Files.readString(root.resolve("blockstates/ruby_block.json"), StandardCharsets.UTF_8))
                .contains("demo:block/ruby_block");
        assertThat(Files.readString(root.resolve("items/ruby_block.json"), StandardCharsets.UTF_8))
                .contains("demo:block/ruby_block");
        assertThat(Files.readAllBytes(root.resolve("textures/item/ruby.png"))).isEqualTo(png);
    }

    @Test
    void generatesCompleteCarrierBlockStateWithoutReplacingVanillaVariants() throws Exception {
        var service = new FandResourcePackService(tempDir.resolve("resourcepacks"));
        service.create("content", "Custom content", 42);
        var block = CustomBlockType.builder(
                        Key.key("demo:machine"),
                        new TestBlockType(Key.key("minecraft:note_block")))
                .state("instrument", "custom_head")
                .state("note", "1")
                .state("powered", "false")
                .build();
        var values = new LinkedHashMap<String, List<String>>();
        values.put("instrument", List.of("harp", "custom_head"));
        values.put("note", List.of("0", "1"));
        values.put("powered", List.of("false", "true"));

        service.models("content").carrierBlockState(block, values, Key.key("minecraft:block/note_block"));

        var json = Files.readString(
                tempDir.resolve("resourcepacks/content/assets/minecraft/blockstates/note_block.json"),
                StandardCharsets.UTF_8);
        assertThat(json)
                .contains("instrument=custom_head,note=1,powered=false")
                .contains("demo:block/machine")
                .contains("instrument=harp,note=0,powered=false")
                .contains("minecraft:block/note_block");
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private record TestBlockType(Key key) implements BlockType {
    }
}
