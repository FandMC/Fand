package io.fand.testplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ParticleCommandTest {

    private final ParticleCommand command = new ParticleCommand();

    @Test
    void completesParticlesAndFunctions() {
        assertEquals(List.of("flame"), command.complete(null, "fandparticle", List.of("fla")));
        assertEquals(List.of("circle"), command.complete(null, "fandparticle", List.of("cir")));
        assertEquals(List.of("minecraft:flame"), command.complete(null, "fandparticle", List.of("mine")));
    }

    @Test
    void completesFunctionParticleArgumentAndCounts() {
        assertEquals(List.of("heart"), command.complete(null, "fandparticle", List.of("circle", "hea")));
        assertEquals(List.of("10", "100"), command.complete(null, "fandparticle", List.of("flame", "10")));
    }

    @Test
    void doesNotCompleteExtraArguments() {
        assertEquals(List.of(), command.complete(null, "fandparticle", List.of("flame", "10", "")));
    }
}
