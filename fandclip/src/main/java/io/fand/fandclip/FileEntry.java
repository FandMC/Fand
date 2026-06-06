package io.fand.fandclip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

record FileEntry(String hash, String id, String path) {

    static List<FileEntry> parse(InputStream in, String name) throws IOException {
        List<FileEntry> list = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] cols = line.split("\t");
                if (cols.length != 3) {
                    throw new IOException("Malformed row in " + name + ": " + line);
                }
                list.add(new FileEntry(cols[0], cols[1], cols[2]));
            }
        }
        return List.copyOf(list);
    }

    boolean matches(Path file) throws IOException {
        return hash.equalsIgnoreCase(FileHashes.hash(file, hash.length()));
    }

    Path resolveUnder(Path root) throws IOException {
        Path target = root.resolve(path).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Refusing zip-slip path: " + path);
        }
        return target;
    }
}
