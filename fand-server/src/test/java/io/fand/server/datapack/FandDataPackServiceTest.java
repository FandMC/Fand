package io.fand.server.datapack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandDataPackServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createsPackMetadataAndWritesFiles() throws Exception {
        var service = new FandDataPackService(tempDir.resolve("datapacks"), () -> null);

        var registration = service.create("demo", "Demo data");
        service.writeText("demo", "data/demo/tags/item/example.json", "{\"values\":[]}");

        assertThat(registration.active()).isTrue();
        assertThat(service.pack("demo")).isPresent();
        assertThat(Files.readString(tempDir.resolve("datapacks/demo/pack.mcmeta"), StandardCharsets.UTF_8))
                .contains("\"description\": \"Demo data\"")
                .contains("\"pack_format\"");
        assertThat(service.read("demo", "data/demo/tags/item/example.json"))
                .contains("{\"values\":[]}".getBytes(StandardCharsets.UTF_8));
        assertThat(service.files("demo").stream().map(io.fand.api.datapack.DataPackFile::path).toList())
                .contains("pack.mcmeta", "data/demo/tags/item/example.json");
    }

    @Test
    void rejectsEscapingFilePaths() {
        var service = new FandDataPackService(tempDir.resolve("datapacks"), () -> null);
        service.create("demo", "Demo data");

        assertThatThrownBy(() -> service.writeText("demo", "../server.properties", "oops"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes");
    }
}
