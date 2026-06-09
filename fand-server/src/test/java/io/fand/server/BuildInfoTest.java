package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class BuildInfoTest {

    @Test
    void loadPropertiesReadsBuildMetadata() {
        var input = new ByteArrayInputStream("""
                version=1.2.3
                minecraftVersion=1.21.8
                """.getBytes(StandardCharsets.UTF_8));

        var properties = BuildInfo.loadProperties(input);

        assertThat(properties)
                .containsEntry("version", "1.2.3")
                .containsEntry("minecraftVersion", "1.21.8");
    }

    @Test
    void loadPropertiesHandlesMissingMetadata() {
        var properties = BuildInfo.loadProperties(null);

        assertThat(properties).isEmpty();
    }

    @Test
    void loadPropertiesHandlesUnreadableMetadata() {
        var properties = BuildInfo.loadProperties(new FailingInputStream());

        assertThat(properties).isEmpty();
    }

    private static final class FailingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("metadata unavailable");
        }
    }
}
