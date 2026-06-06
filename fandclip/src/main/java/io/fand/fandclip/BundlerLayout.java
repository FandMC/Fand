package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Reads Mojang's bundled server jar layout and materialises its libraries.
 */
final class BundlerLayout {

    private final Path bundler;
    private final List<FileEntry> libraries;

    private BundlerLayout(Path bundler, List<FileEntry> libraries) {
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
            for (FileEntry library : libraries) {
                Path target = library.resolveUnder(libDir);
                if (Files.exists(target) && library.matches(target)) {
                    out.add(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                String inner = "META-INF/libraries/" + library.path();
                var entry = jar.getEntry(inner);
                if (entry == null) {
                    throw new IOException("Bundler missing library " + inner);
                }
                Path tmp = target.resolveSibling(target.getFileName() + ".part");
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!library.matches(tmp)) {
                    Files.deleteIfExists(tmp);
                    throw new IOException("Hash mismatch for " + library.path());
                }
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                out.add(target);
            }
        }
        return out;
    }

    private static List<FileEntry> parseList(JarFile jar, String name) throws IOException {
        var entry = jar.getEntry(name);
        if (entry == null) {
            throw new IOException("Bundler missing manifest " + name);
        }
        try (var in = jar.getInputStream(entry)) {
            return FileEntry.parse(in, name);
        }
    }
}
