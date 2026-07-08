package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class WorldSnapshotOptionsTest {

    @Test
    void defaultsToInMemorySnapshots() {
        var options = WorldSnapshotOptions.inMemory();

        assertThat(options.memory()).isTrue();
        assertThat(options.path()).isNull();
    }

    @Test
    void fileSnapshotsRequirePath() {
        var path = Path.of("snapshots", "arena");

        assertThat(WorldSnapshotOptions.file(path).path()).isEqualTo(path);
        assertThatIllegalArgumentException().isThrownBy(() -> new WorldSnapshotOptions(false, null));
    }
}
