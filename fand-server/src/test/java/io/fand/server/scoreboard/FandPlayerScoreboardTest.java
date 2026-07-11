package io.fand.server.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FandPlayerScoreboardTest {

    @Test
    void offThreadPacketSendWaitsForActualSendResult() throws IOException {
        var source = Files.readString(Path.of(
                "src/main/java/io/fand/server/scoreboard/FandPlayerScoreboard.java"));

        assertThat(source).contains(
                "return ScoreboardThreading.call(server, () -> {",
                "handle.connection.send(packet);",
                "return true;");
        assertThat(source).doesNotContain(
                "ServerThreading.run(server",
                "new AtomicBoolean()");
    }
}
