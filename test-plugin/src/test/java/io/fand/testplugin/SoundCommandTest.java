package io.fand.testplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SoundCommandTest {

    private final SoundCommand command = new SoundCommand();

    @Test
    void completesSoundAliasesAndCustomKeyExample() {
        assertEquals(List.of("levelup"), command.complete(null, "fandsound", List.of("le")));
        assertEquals(List.of("minecraft:block.note_block.pling"), command.complete(null, "fandsound", List.of("mine")));
    }

    @Test
    void completesNumericPlaybackParameters() {
        assertEquals(List.of("1"), command.complete(null, "fandsound", List.of("levelup", "1")));
        assertEquals(List.of("1", "1.5"), command.complete(null, "fandsound", List.of("levelup", "1", "1")));
        assertEquals(List.of("0", "0.25"), command.complete(null, "fandsound", List.of("levelup", "1", "1", "0")));
    }

    @Test
    void doesNotSuggestSeedValues() {
        assertEquals(List.of(), command.complete(null, "fandsound", List.of("levelup", "1", "1", "0", "")));
    }
}
