package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class WorldFileOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    void copiesWorldDirectoryWithoutSessionLock() throws Exception {
        var source = tempDir.resolve("source");
        var nested = source.resolve("region");
        Files.createDirectories(nested);
        Files.writeString(source.resolve("level.dat"), "level");
        Files.writeString(source.resolve("session.lock"), "lock");
        Files.writeString(nested.resolve("r.0.0.mca"), "chunk");

        var target = tempDir.resolve("target");
        WorldFileOperations.copyWorldDirectory(source, target);

        assertThat(Files.readString(target.resolve("level.dat"))).isEqualTo("level");
        assertThat(Files.readString(target.resolve("region").resolve("r.0.0.mca"))).isEqualTo("chunk");
        assertThat(target.resolve("session.lock")).doesNotExist();
    }

    @Test
    void rejectsNestedTargets() throws Exception {
        var source = tempDir.resolve("source");
        Files.createDirectories(source);

        assertThatThrownBy(() -> WorldFileOperations.copyWorldDirectory(source, source.resolve("copy")))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("inside source directory");
    }

    @Test
    void rejectsTargetsContainingSource() throws Exception {
        var target = tempDir.resolve("target");
        var source = target.resolve("source");
        Files.createDirectories(source);

        assertThatThrownBy(() -> WorldFileOperations.copyWorldDirectory(source, target))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("cannot contain source directory");
    }

    @Test
    void atomicCopyDoesNotReplaceExistingTarget() throws Exception {
        var source = tempDir.resolve("source");
        Files.createDirectories(source);
        Files.writeString(source.resolve("level.dat"), "new");

        var target = tempDir.resolve("target");
        Files.createDirectories(target);
        Files.writeString(target.resolve("level.dat"), "old");

        assertThatThrownBy(() -> WorldFileOperations.copyWorldDirectoryAtomically(source, target))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("already exists");
        assertThat(Files.readString(target.resolve("level.dat"))).isEqualTo("old");
    }

    @Test
    void atomicCopyPublishesCompleteDirectory() throws Exception {
        var source = tempDir.resolve("source");
        Files.createDirectories(source.resolve("region"));
        Files.writeString(source.resolve("level.dat"), "level");
        Files.writeString(source.resolve("region").resolve("r.0.0.mca"), "chunk");

        var target = tempDir.resolve("target");
        WorldFileOperations.copyWorldDirectoryAtomically(source, target);

        assertThat(Files.readString(target.resolve("level.dat"))).isEqualTo("level");
        assertThat(Files.readString(target.resolve("region").resolve("r.0.0.mca"))).isEqualTo("chunk");
    }
}
