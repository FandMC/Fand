package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;

final class PluginRuntimeTestSupport {

    private PluginRuntimeTestSupport() {
    }

    static Path createPluginJar(Path tempDir, Path jarPath, String descriptorJson, Map<String, String> sources, List<Path> extraClasspath) throws IOException {
        var sourceDir = Files.createDirectories(tempDir.resolve("src-" + jarPath.getFileName()));
        var classesDir = Files.createDirectories(tempDir.resolve("classes-" + jarPath.getFileName()));
        for (var entry : sources.entrySet()) {
            var file = sourceDir.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
        }

        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();

        var classpath = new StringBuilder(System.getProperty("java.class.path"));
        for (var path : extraClasspath) {
            classpath.append(System.getProperty("path.separator")).append(path);
        }

        try (var sourceStream = Files.walk(sourceDir)) {
            var sourceFiles = sourceStream
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toString)
                    .toList();
            var arguments = new java.util.ArrayList<String>();
            arguments.add("-classpath");
            arguments.add(classpath.toString());
            arguments.add("-d");
            arguments.add(classesDir.toString());
            arguments.addAll(sourceFiles);
            var exitCode = compiler.run(null, null, null, arguments.toArray(String[]::new));
            assertThat(exitCode).isZero();
        }

        Files.createDirectories(jarPath.getParent());
        try (var out = new JarOutputStream(Files.newOutputStream(jarPath));
             var classStream = Files.walk(classesDir)) {
            classStream.filter(Files::isRegularFile).forEach(path -> writeJarEntry(out, classesDir, path));
            out.putNextEntry(new JarEntry("fand-plugin.json"));
            out.write(descriptorJson.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jarPath;
    }

    static String descriptorJson(String id, String mainClass, List<String> depends) {
        return descriptorJson(id, mainClass, depends, "[]");
    }

    static String descriptorJson(String id, String mainClass, List<String> depends, String permissionsJson) {
        return """
                {
                  \"id\": \"%s\",
                  \"version\": \"1.0.0\",
                  \"mainClass\": \"%s\",
                  \"authors\": [\"test\"],
                  \"depends\": %s,
                  \"permissions\": %s
                }
                """.formatted(id, mainClass, dependsToJson(depends), permissionsJson);
    }

    static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static String dependsToJson(List<String> depends) {
        if (depends.isEmpty()) {
            return "[]";
        }
        return depends.stream().map(value -> "\"" + value + "\"").collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    private static void writeJarEntry(JarOutputStream out, Path root, Path file) {
        try {
            var name = root.relativize(file).toString().replace('\\', '/');
            out.putNextEntry(new JarEntry(name));
            out.write(Files.readAllBytes(file));
            out.closeEntry();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
