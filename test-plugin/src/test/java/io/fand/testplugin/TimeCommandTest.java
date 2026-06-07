package io.fand.testplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class TimeCommandTest {

    private final TimeCommand command = new TimeCommand();

    @Test
    void completesNamedTimes() {
        assertEquals(List.of("day"), command.complete(null, "fandtime", List.of("d")));
        assertEquals(List.of("noon", "night"), command.complete(null, "fandtime", List.of("n")));
        assertEquals(List.of("noon"), command.complete(null, "fandtime", List.of("no")));
    }

    @Test
    void doesNotCompleteExtraArguments() {
        assertEquals(List.of(), command.complete(null, "fandtime", List.of("day", "")));
    }
}
