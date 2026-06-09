package io.fand.fandclip;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class FandclipTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("fandclip.quietJvmWarnings");
        System.clearProperty("fandclip.quietJvmWarnings.restarted");
    }

    @Test
    void findsMissingQuietJvmArguments() {
        assertThat(Fandclip.missingQuietJvmArgs(List.of()))
                .containsExactly("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow");
    }

    @Test
    void doesNotAddQuietJvmArgumentsForOlderJava() {
        assertThat(Fandclip.missingQuietJvmArgs(List.of(), 23)).isEmpty();
    }

    @Test
    void acceptsSplitAndEqualsQuietJvmArguments() {
        assertThat(Fandclip.missingQuietJvmArgs(List.of(
                "--enable-native-access", "ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow")))
                .isEmpty();
    }

    @Test
    void acceptsCommaSeparatedNativeAccessTargets() {
        assertThat(Fandclip.missingQuietJvmArgs(List.of(
                "--enable-native-access=java.base,ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access", "allow")))
                .isEmpty();
    }

    @Test
    void relaunchesOnlyWhenEnabledAndNotAlreadyRestarted() {
        assertThat(Fandclip.shouldRelaunchWithQuietJvmWarnings(List.of())).isTrue();

        System.setProperty("fandclip.quietJvmWarnings", "false");
        assertThat(Fandclip.shouldRelaunchWithQuietJvmWarnings(List.of())).isFalse();

        System.clearProperty("fandclip.quietJvmWarnings");
        System.setProperty("fandclip.quietJvmWarnings.restarted", "true");
        assertThat(Fandclip.shouldRelaunchWithQuietJvmWarnings(List.of())).isFalse();
    }

    @Test
    void doesNotRelaunchWhenQuietJvmArgumentsArePresent() {
        assertThat(Fandclip.shouldRelaunchWithQuietJvmWarnings(List.of(
                "--enable-native-access=ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow")))
                .isFalse();
    }

    @Test
    void clipManifestFallsBackWhenMetadataCannotBeRead() {
        assertThat(ClipManifest.load(new FailingInputStream())).isEmpty();
    }

    private static final class FailingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("manifest unavailable");
        }
    }
}
