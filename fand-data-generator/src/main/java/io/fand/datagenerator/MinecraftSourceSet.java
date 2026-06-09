package io.fand.datagenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class MinecraftSourceSet {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s*(?:public\\s+)?(?:static\\s+)?(?:final\\s+)?(.+?)\\s+([A-Z][A-Z0-9_]*)\\s*=",
            Pattern.MULTILINE);

    private final Path root;

    MinecraftSourceSet(Path root) {
        this.root = root;
    }

    String read(String relativePath) throws IOException {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }

    String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    Path resolve(String relativePath) {
        return root.resolve(relativePath);
    }

    List<Path> files(String relativeRoot, String fileNameSuffix) throws IOException {
        var base = root.resolve(relativeRoot);
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        try (var stream = Files.walk(base)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(fileNameSuffix))
                    .sorted()
                    .toList();
        }
    }

    String relativePath(Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    List<StaticField> staticFields(String relativePath) throws IOException {
        var source = read(relativePath);
        var fields = new ArrayList<StaticField>();
        var matcher = FIELD_PATTERN.matcher(source);
        int searchStart = 0;
        while (matcher.find(searchStart)) {
            var type = matcher.group(1).replace('\n', ' ').replace('\r', ' ').trim();
            var name = matcher.group(2);
            int initializerStart = matcher.end();
            int initializerEnd = findInitializerEnd(source, initializerStart);
            fields.add(new StaticField(type, name, source.substring(initializerStart, initializerEnd).trim()));
            searchStart = initializerEnd + 1;
        }
        return fields;
    }

    private static int findInitializerEnd(String source, int start) {
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = start; i < source.length(); i++) {
            char c = source.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && (inString || inChar)) {
                escaped = true;
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
                continue;
            }
            if (c == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) {
                continue;
            }
            switch (c) {
                case '(' -> parenDepth++;
                case ')' -> parenDepth--;
                case '{' -> braceDepth++;
                case '}' -> braceDepth--;
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth--;
                case ';' -> {
                    if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                        return i;
                    }
                }
                default -> {
                }
            }
        }
        throw new IllegalStateException("Unterminated static field initializer");
    }
}
