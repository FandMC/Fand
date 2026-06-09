package io.fand.server.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public interface FandNoiseSettingsSource {

    Holder<NoiseGeneratorSettings> fand$noiseGeneratorSettings();
}
