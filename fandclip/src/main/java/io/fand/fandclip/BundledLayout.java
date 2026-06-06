package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class BundledLayout {

    private static final String VERSIONS_LIST = "/META-INF/versions.list";
    private static final String LIBRARIES_LIST = "/META-INF/libraries.list";
    private static final String MAIN_CLASS = "/META-INF/main-class";

    private final List<FileEntry> versions;
    private final List<FileEntry> libraries;
    private final String mainClass;

    private BundledLayout(List<FileEntry> versions, List<FileEntry> libraries, String mainClass) {
        this.versions = versions;
        this.libraries = libraries;
        this.mainClass = mainClass;
    }

    static BundledLayout read() throws IOException {
        return new BundledLayout(
                readEntries(VERSIONS_LIST),
                readEntries(LIBRARIES_LIST),
                readText(MAIN_CLASS).strip());
    }

    String mainClass() {
        return mainClass;
    }

    List<Path> materialiseVersions(Path versionsDir) throws IOException {
        return materialise(versions, "/META-INF/versions/", versionsDir);
    }

    List<Path> materialiseLibraries(Path librariesDir) throws IOException {
        return materialise(libraries, "/META-INF/libraries/", librariesDir);
    }

    private static List<Path> materialise(List<FileEntry> entries, String resourceBase, Path outputDir) throws IOException {
        List<Path> out = new ArrayList<>(entries.size());
        for (FileEntry entry : entries) {
            Path target = entry.resolveUnder(outputDir);
            ResourceExtractor.extract(resourceBase + entry.path(), target, entry);
            out.add(target);
        }
        return List.copyOf(out);
    }

    private static List<FileEntry> readEntries(String resource) throws IOException {
        try (InputStream in = BundledLayout.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Bundled resource missing: " + resource);
            }
            return FileEntry.parse(in, resource);
        }
    }

    private static String readText(String resource) throws IOException {
        try (InputStream in = BundledLayout.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Bundled resource missing: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
