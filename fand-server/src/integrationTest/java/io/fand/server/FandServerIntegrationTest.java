package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class FandServerIntegrationTest {

    @Test
    void exposesGeneratedBuildInformation() {
        assertThat(BuildInfo.VERSION).isNotBlank();
        assertThat(BuildInfo.MINECRAFT_VERSION).isNotBlank();
    }
}
