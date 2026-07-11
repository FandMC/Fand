package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PluginClassLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void platformGuavaCannotBeOverriddenByPluginOrSoftDependency() throws IOException, ClassNotFoundException {
        var pluginJar = PluginRuntimeTestSupport.createJavaJar(
                tempDir,
                tempDir.resolve("plugin.jar"),
                Map.of("com/google/common/base/Preconditions.java", """
                        package com.google.common.base;
                        public final class Preconditions {
                            public static String pluginMarker() { return "plugin"; }
                        }
                        """),
                List.of());

        try (var loader = new PluginClassLoader(
                pluginJar.toUri().toURL(),
                getClass().getClassLoader(),
                List.of())) {
            assertThat(loader.loadClass(Preconditions.class.getName())).isSameAs(Preconditions.class);
        }
    }
}
