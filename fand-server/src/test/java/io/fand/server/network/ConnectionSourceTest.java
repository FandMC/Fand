package io.fand.server.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ConnectionSourceTest {

    @Test
    void disconnectDoesNotWaitForNettyCloseOnCallerThread() throws IOException {
        var connection = Files.readString(
                Path.of("src/minecraft/java/net/minecraft/network/Connection.java"),
                StandardCharsets.UTF_8);

        assertThat(connection)
                .contains("this.disconnectionDetails = details;", "this.channel.close();")
                .doesNotContain("this.channel.close().awaitUninterruptibly()");
    }
}
