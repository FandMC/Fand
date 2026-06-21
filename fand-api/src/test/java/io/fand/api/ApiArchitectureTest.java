package io.fand.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ApiArchitectureTest {

    @Test
    void handwrittenApiDoesNotReferenceServerOrMinecraftImplementationPackages() throws IOException {
        var violations = new ArrayList<String>();

        scan(Path.of("src/main/java"), violations);

        assertThat(violations).isEmpty();
    }

    private static void scan(Path root, List<String> violations) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (var path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                var source = Files.readString(path, StandardCharsets.UTF_8);
                reject(path, source, violations, "io.fand.server");
                reject(path, source, violations, "net.minecraft");
                reject(path, source, violations, "Class.forName(\"io.fand.server");
                reject(path, source, violations, "Class.forName(\"net.minecraft");
                reject(path, source, violations, "MethodHandles.lookup().findClass(\"io.fand.server");
                reject(path, source, violations, "MethodHandles.lookup().findClass(\"net.minecraft");
            }
        }
    }

    private static void reject(Path path, String source, List<String> violations, String forbidden) {
        if (source.contains(forbidden)) {
            violations.add(path + " contains " + forbidden);
        }
    }
}
