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
        assertBefore(source, "new FandCommandBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandDecoratedPotBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandEndGatewayBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandBannerBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandSkullBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandSignBlockEntity", "new FandBlockEntity");
        assertBefore(source, "new FandContainerBlockEntity", "new FandBlockEntity");
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
