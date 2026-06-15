package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Downloads a vanilla bundler jar via piston-meta. Zero external deps: HTTP via
 * {@link HttpClient} and a hand-rolled JSON-string scan since we only need three
 * fields ({@code url}, {@code sha1}, manifest entry's {@code url}).
 */
class VanillaDownloader {

    private static final String VERSION_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    void download(String version, Path target) throws IOException, InterruptedException {
        ServerDownload sd = serverDownload(version);
        if (sd == null) {
            throw new IOException("Version " + version + " has no server download");
        }
        downloadVerified(sd.url, sd.sha1, target);
    }

    void ensureCached(String version, Path target) throws IOException, InterruptedException {
        ServerDownload sd = serverDownload(version);
        if (sd == null) {
            throw new IOException("Version " + version + " has no server download");
        }
        if (Files.exists(target) && sd.sha1.equalsIgnoreCase(FileHashes.sha1(target))) {
            return;
        }
        if (Files.exists(target)) {
            Files.delete(target);
        }
        downloadVerified(sd.url, sd.sha1, target);
    }

    private ServerDownload serverDownload(String version) throws IOException, InterruptedException {
        String manifest = getString(VERSION_MANIFEST_URL);
        String versionUrl = findVersionUrl(manifest, version);
        if (versionUrl == null) {
            throw new IOException("Version " + version + " not in piston manifest");
        }
        return parseServerDownload(getString(versionUrl));
    }

    void downloadVerified(String url, String expectedSha1, Path target) throws IOException, InterruptedException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<Path> resp = http.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
        if (resp.statusCode() / 100 != 2) {
            Files.deleteIfExists(tmp);
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        }
        String got = FileHashes.sha1(tmp);
        if (!expectedSha1.equalsIgnoreCase(got)) {
            Files.deleteIfExists(tmp);
            throw new IOException("SHA-1 mismatch for " + url + ": expected " + expectedSha1 + ", got " + got);
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    String getString(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        }
        try (InputStream in = resp.body()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Scans the version manifest for the {@code id == version} entry and returns
     * its {@code url}. Avoids a JSON library by exploiting the manifest's stable
     * field ordering: {@code "id":"<v>","type":..,"url":"<u>"}.
     */
    private static String findVersionUrl(String manifest, String version) {
        String idTag = "\"id\": \"" + version + "\"";
        int idx = manifest.indexOf(idTag);
        if (idx < 0) {
            idTag = "\"id\":\"" + version + "\"";
            idx = manifest.indexOf(idTag);
        }
        if (idx < 0) return null;
        int urlStart = manifest.indexOf("\"url\":", idx);
        if (urlStart < 0) return null;
        int valueStart = manifest.indexOf('"', urlStart + "\"url\":".length());
        if (valueStart < 0) return null;
        valueStart++;
        int valueEnd = manifest.indexOf('"', valueStart);
        return manifest.substring(valueStart, valueEnd);
    }

    /**
     * Extracts the {@code downloads.server} url+sha1 from a version manifest. The
     * server block always nests {@code "url"} and {@code "sha1"} fields.
     */
    private static ServerDownload parseServerDownload(String json) {
        int server = json.indexOf("\"server\"");
        if (server < 0) return null;
        int braceOpen = json.indexOf('{', server);
        int braceClose = json.indexOf('}', braceOpen);
        if (braceOpen < 0 || braceClose < 0) return null;
        String block = json.substring(braceOpen, braceClose);
        String url = readField(block, "url");
        String sha1 = readField(block, "sha1");
        if (url == null || sha1 == null) return null;
        return new ServerDownload(url, sha1);
    }

    private static String readField(String block, String name) {
        String tag = "\"" + name + "\":";
        int s = block.indexOf(tag);
        if (s < 0) return null;
        int tagEnd = s + tag.length();
        // Skip whitespace
        while (tagEnd < block.length() && Character.isWhitespace(block.charAt(tagEnd))) {
            tagEnd++;
        }
        // Expect opening quote
        if (tagEnd >= block.length() || block.charAt(tagEnd) != '"') return null;
        int valueStart = tagEnd + 1;
        int valueEnd = block.indexOf('"', valueStart);
        if (valueEnd < 0) return null;
        return block.substring(valueStart, valueEnd);
    }

    private record ServerDownload(String url, String sha1) {}
}
