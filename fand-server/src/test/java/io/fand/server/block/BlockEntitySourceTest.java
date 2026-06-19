package io.fand.server.block;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class BlockEntitySourceTest {

    @Test
    void dispatchesTypedBlockEntitiesBeforeGenericWrapper() throws IOException {
        var source = Files.readString(Path.of("src/main/java/io/fand/server/block/FandBlock.java"), StandardCharsets.UTF_8);

        assertBefore(source, "new FandSpawnerBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandBeaconBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandLecternBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandBrewingStandBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandFurnaceBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandJukeboxBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandBeehiveBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandSculkSensorBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandCommandBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandDecoratedPotBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandEndGatewayBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandBannerBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandSkullBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandSignBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandContainerBlockEntity", "new FandBlockEntity");
    }

    @Test
    void blockEntityControlFieldsStayMapped() throws IOException {
        var furnace = read("src/minecraft/java/net/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity.java");
        var brewing = read("src/minecraft/java/net/minecraft/world/level/block/entity/BrewingStandBlockEntity.java");
        var beehive = read("src/minecraft/java/net/minecraft/world/level/block/entity/BeehiveBlockEntity.java");
        var sculk = read("src/minecraft/java/net/minecraft/world/level/block/entity/SculkSensorBlockEntity.java");

        assertThat(furnace).contains("private int litTimeRemaining;");
        assertThat(furnace).contains("private int litTotalTime;");
        assertThat(furnace).contains("private int cookingTimer;");
        assertThat(furnace).contains("private int cookingTotalTime;");
        assertThat(brewing).contains("private int brewTime;");
        assertThat(brewing).contains("private int fuel;");
        assertThat(beehive).contains("public int getOccupantCount()");
        assertThat(beehive).contains("public void emptyAllLivingFromHive");
        assertThat(beehive).contains("private @Nullable BlockPos savedFlowerPos");
        assertThat(sculk).contains("public void setLastVibrationFrequency");
    }

    @Test
    void deeperBlockEntityControlsStayWired() throws IOException {
        var jukeboxApi = read("../fand-api/src/main/java/io/fand/api/block/JukeboxBlockEntity.java");
        var sculkApi = read("../fand-api/src/main/java/io/fand/api/block/SculkSensorBlockEntity.java");
        var beehiveApi = read("../fand-api/src/main/java/io/fand/api/block/BeehiveBlockEntity.java");
        var jukebox = read("src/main/java/io/fand/server/block/FandJukeboxBlockEntity.java");
        var sculk = read("src/main/java/io/fand/server/block/FandSculkSensorBlockEntity.java");
        var beehive = read("src/main/java/io/fand/server/block/FandBeehiveBlockEntity.java");

        assertThat(jukeboxApi).contains("boolean playing()");
        assertThat(jukeboxApi).contains("long playTicks()");
        assertThat(jukeboxApi).contains("void eject()");
        assertThat(jukebox).contains("getSongPlayer().isPlaying()");
        assertThat(jukebox).contains("tryForcePlaySong()");
        assertThat(jukebox).contains("popOutTheItem()");

        assertThat(sculkApi).contains("SculkSensorPhase phase()");
        assertThat(sculkApi).contains("void activate(int power, int vibrationFrequency)");
        assertThat(sculk).contains("sculk.activate(null");
        assertThat(sculk).contains("SculkSensorBlock.deactivate");

        assertThat(beehiveApi).contains("void releaseBees(BeeReleaseMode mode)");
        assertThat(beehiveApi).contains("Optional<Location> flowerPosition()");
        assertThat(beehive).contains("\"savedFlowerPos\"");
        assertThat(beehive).contains("BeeReleaseStatus.HONEY_DELIVERED");
        assertThat(beehive).contains("ReflectionFields.set(SAVED_FLOWER_POS");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
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
