package io.fand.server.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class FandMapServiceTest {

    @Test
    void returnsEmptyWithoutAttachedServer() {
        var service = new FandMapService(() -> null);

        assertThat(service.map(0)).isEmpty();
        assertThat(service.map(-1)).isEmpty();
    }

    @Test
    void createRequiresAttachedServer() {
        var service = new FandMapService(() -> null);

        assertThatThrownBy(() -> service.create((map, canvas) -> canvas.pixel(0, 0, (byte) 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Minecraft server is not attached");
    }
}
