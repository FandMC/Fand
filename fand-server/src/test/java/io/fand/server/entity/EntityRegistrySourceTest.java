package io.fand.server.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EntityRegistrySourceTest {

    @Test
    void dispatchesSpecializedWrappersBeforeGenericLivingEntity() throws IOException {
        var source = Files.readString(Path.of("src/main/java/io/fand/server/entity/EntityRegistry.java"), StandardCharsets.UTF_8);

        assertBefore(source, "new FandArmorStand", "new FandLivingEntity");
        assertBefore(source, "new FandEnderDragon", "new FandMob");
        assertBefore(source, "new FandAreaEffectCloud", "new FandEntity");
        assertBefore(source, "new FandExplosive", "new FandEntity");
        assertBefore(source, "new FandExperienceOrb", "new FandEntity");
        assertBefore(source, "new FandDisplay", "new FandEntity");
        assertBefore(source, "new FandHanging", "new FandEntity");
        assertBefore(source, "new FandMinecart", "new FandVehicle");
        assertBefore(source, "new FandVehicle", "new FandEntity");
        assertBefore(source, "new FandVillager", "new FandAnimal");
        assertBefore(source, "new FandHorse", "new FandAnimal");
        assertBefore(source, "new FandEnderman", "new FandMob");
        assertBefore(source, "new FandCreeper", "new FandMob");
    }

    private static void assertBefore(String source, String first, String second) {
        assertThat(source.indexOf(first))
                .as(first + " exists")
                .isGreaterThanOrEqualTo(0);
        assertThat(source.indexOf(second))
                .as(second + " exists")
                .isGreaterThanOrEqualTo(0);
        assertThat(source.indexOf(first))
                .as(first + " is dispatched before " + second)
                .isLessThan(source.indexOf(second));
    }
}
