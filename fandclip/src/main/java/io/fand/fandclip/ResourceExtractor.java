package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class ResourceExtractor {

    private ResourceExtractor() {}

    static void extract(String resource, Path target) throws IOException {
        try (InputStream in = ResourceExtractor.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Bundled resource missing: " + resource);
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
