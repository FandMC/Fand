package io.fand.fandclip;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VanillaDownloaderTest {

    @TempDir
    private Path tempDir;

    @Test
    void reusesCachedBundlerWithMatchingHash() throws Exception {
        var target = tempDir.resolve("cache/mojang_test.jar");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "cached");
        var downloader = new FakeDownloader(FileHashes.sha1(target), "downloaded");

        downloader.ensureCached("test", target);

        assertThat(Files.readString(target)).isEqualTo("cached");
        assertThat(downloader.downloads).isZero();
    }

    @Test
    void redownloadsCachedBundlerWithMismatchedHash() throws Exception {
        var target = tempDir.resolve("cache/mojang_test.jar");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "poisoned");
        var downloader = new FakeDownloader(FileHashes.sha1(tempFile("downloaded")), "downloaded");

        downloader.ensureCached("test", target);

        assertThat(Files.readString(target)).isEqualTo("downloaded");
        assertThat(downloader.downloads).isEqualTo(1);
    }

    private Path tempFile(String content) throws IOException {
        var file = Files.createTempFile(tempDir, "hash", ".tmp");
        Files.writeString(file, content);
        return file;
    }

    private static final class FakeDownloader extends VanillaDownloader {

        private final String sha1;
        private final String downloadedContent;
        private int downloads;

        private FakeDownloader(String sha1, String downloadedContent) {
            this.sha1 = sha1;
            this.downloadedContent = downloadedContent;
        }

        @Override
        String getString(String url) {
            if (url.contains("version_manifest")) {
                return "{\"versions\":[{\"id\":\"test\",\"url\":\"https://example.invalid/test.json\"}]}";
            }
            return "{\"downloads\":{\"server\":{\"url\":\"https://example.invalid/server.jar\",\"sha1\":\"" + sha1 + "\"}}}";
        }

        @Override
        void downloadVerified(String url, String expectedSha1, Path target) throws IOException {
            downloads++;
            Files.createDirectories(target.getParent());
            Files.writeString(target, downloadedContent);
        }
    }
}
