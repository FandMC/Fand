package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FandServerIntegrationTest {

    @Test
    void exposesGeneratedBuildInformation() {
        assertThat(BuildInfo.VERSION).isNotBlank();
        assertThat(BuildInfo.MINECRAFT_VERSION).isNotBlank();
    }

    @Test
    void eventRegressionHooksStayWired() throws IOException {
        var damageSource = read("src/main/java/io/fand/server/event/EntityEvents.java");
        var blockItemSource = read("src/minecraft/java/net/minecraft/world/item/BlockItem.java");

        assertThat(damageSource).contains("var wrapped = FandHooks.wrapEntity(entity);");
        assertThat(damageSource).contains("if (wrapped instanceof io.fand.api.entity.LivingEntity living)");
        assertThat(blockItemSource).contains("restorePlacement(fandServerLevel, fandBeforeStates, fandBeforeComponents, fandServerPlayer)");
        assertThat(blockItemSource).contains("changedPlacementPositions(level, fandBeforeStates)");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
