package io.fand.testplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class WeatherCommandTest {

    private final WeatherCommand command = new WeatherCommand();

    @Test
    void completesWeatherModes() {
        assertEquals(List.of("clear"), command.complete(null, "fandweather", List.of("c")));
        assertEquals(List.of("rain"), command.complete(null, "fandweather", List.of("r")));
        assertEquals(List.of("thunder"), command.complete(null, "fandweather", List.of("t")));
    }

    @Test
    void doesNotCompleteExtraArguments() {
        assertEquals(List.of(), command.complete(null, "fandweather", List.of("clear", "")));
    }
}
