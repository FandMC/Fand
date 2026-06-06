package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class ResourceExtractor {

    private ResourceExtractor() {}

    static void extract(String resource, Path target, FileEntry entry) throws IOException {
        if (Files.exists(target) && entry.matches(target)) {
            return;
        }
        try (InputStream in = ResourceExtractor.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Bundled resource missing: " + resource);
            }
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".part");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            if (!entry.matches(tmp)) {
                Files.deleteIfExists(tmp);
                throw new IOException("Hash mismatch for bundled resource " + resource);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
