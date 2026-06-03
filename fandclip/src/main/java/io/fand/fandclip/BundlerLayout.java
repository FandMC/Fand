package io.fand.fandclip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Reads Mojang's bundler layout (the vanilla server jar produced for 1.18+).
 *
 * <p>Two manifest files matter:
 * <ul>
 *   <li>{@code META-INF/libraries.list} - tab-separated rows of {@code sha1\tcoords\tpath}</li>
 *   <li>{@code META-INF/versions.list}  - same shape, but the path is the real server jar</li>
 * </ul>
 * Every library lives at {@code META-INF/libraries/<path>} inside the bundler.
 */
final class BundlerLayout {

    private final Path bundler;
    private final List<Entry> libraries;

    private BundlerLayout(Path bundler, List<Entry> libraries) {
        this.bundler = bundler;
        this.libraries = libraries;
    }

    static BundlerLayout read(Path bundler) throws IOException {
        try (JarFile jar = new JarFile(bundler.toFile())) {
            return new BundlerLayout(bundler, parseList(jar, "META-INF/libraries.list"));
        }
    }

    /**
     * Ensures every library is materialised under {@code libDir} and returns the
     * resulting on-disk paths. Existing files with the right hash are reused.
     */
    List<Path> materialiseLibraries(Path libDir) throws IOException {
        List<Path> out = new ArrayList<>(libraries.size());
        try (JarFile jar = new JarFile(bundler.toFile())) {
            for (Entry e : libraries) {
                Path target = libDir.resolve(e.path).normalize();
                if (!target.startsWith(libDir)) {
                    throw new IOException("Refusing zip-slip path: " + e.path);
                }
                if (Files.exists(target) && e.sha1.equalsIgnoreCase(hash(target, e.sha1.length()))) {
                    out.add(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                String inner = "META-INF/libraries/" + e.path;
                var entry = jar.getEntry(inner);
                if (entry == null) {
                    throw new IOException("Bundler missing library " + inner);
                }
                Path tmp = target.resolveSibling(target.getFileName() + ".part");
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                String got = hash(tmp, e.sha1.length());
                if (!e.sha1.equalsIgnoreCase(got)) {
                    Files.deleteIfExists(tmp);
                    throw new IOException("Hash mismatch for " + e.path + ": expected " + e.sha1 + ", got " + got);
                }
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                out.add(target);
            }
        }
        return out;
    }

    private static List<Entry> parseList(JarFile jar, String name) throws IOException {
        var entry = jar.getEntry(name);
        if (entry == null) {
            throw new IOException("Bundler missing manifest " + name);
        }
        List<Entry> list = new ArrayList<>();
        try (var in = jar.getInputStream(entry);
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split("\t");
                if (cols.length < 3) {
                    throw new IOException("Malformed row in " + name + ": " + line);
                }
                list.add(new Entry(cols[0], cols[1], cols[2]));
            }
        }
        return list;
    }

    static String sha1(Path path) throws IOException {
        return hash(path, 40);
    }

    private static String hash(Path path, int expectedLength) throws IOException {
        String algorithm = expectedLength == 64 ? "SHA-256" : "SHA-1";
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(algorithm + " missing from JDK", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            out[i * 2] = hex[b >>> 4];
            out[i * 2 + 1] = hex[b & 0x0f];
        }
        return new String(out);
    }

    record Entry(String sha1, String coords, String path) {}
}
