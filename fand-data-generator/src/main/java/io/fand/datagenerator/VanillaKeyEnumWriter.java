package io.fand.datagenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class VanillaKeyEnumWriter {

    private final Path outputSources;

    VanillaKeyEnumWriter(Path outputSources) {
        this.outputSources = outputSources;
    }

    void write(VanillaKeyEnumSpec spec, List<KeyEntry> entries) throws IOException {
        if (entries.isEmpty()) {
            throw new IllegalStateException("No entries generated for " + spec.typeName());
        }

        var packagePath = Path.of(spec.packageName().replace('.', '/'));
        var outputFile = outputSources.resolve(packagePath).resolve(spec.typeName() + ".java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, enumSource(spec, entries), StandardCharsets.UTF_8);
    }

    private static String enumSource(VanillaKeyEnumSpec spec, List<KeyEntry> entries) {
        var source = new StringBuilder();
        source.append("package ").append(spec.packageName()).append(";\n\n");
        source.append("import io.fand.api.VanillaKey;\n");
        source.append("import net.kyori.adventure.key.Key;\n\n");
        source.append("/** ").append(spec.javadoc()).append(" */\n");
        source.append("public enum ").append(spec.typeName()).append(" implements VanillaKey {\n\n");
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            source.append("    ").append(entry.name()).append("(\"").append(entry.key()).append("\")");
            source.append(i == entries.size() - 1 ? ";\n\n" : ",\n");
        }
        source.append("    private final Key key;\n\n");
        source.append("    ").append(spec.typeName()).append("(String key) {\n");
        source.append("        this.key = Key.key(key);\n");
        source.append("    }\n\n");
        source.append("    public Key key() {\n");
        source.append("        return key;\n");
        source.append("    }\n\n");
        source.append("    public String asString() {\n");
        source.append("        return key.asString();\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public String toString() {\n");
        source.append("        return asString();\n");
        source.append("    }\n");
        source.append("}\n");
        return source.toString();
    }
}
