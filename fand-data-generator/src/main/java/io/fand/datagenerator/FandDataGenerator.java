package io.fand.datagenerator;

import java.io.IOException;
import java.nio.file.Path;

public final class FandDataGenerator {

    private FandDataGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: FandDataGenerator <minecraft-source-root> <output-source-root>");
        }

        var minecraftSources = new MinecraftSourceSet(Path.of(args[0]));
        var registrySources = new VanillaRegistrySources(minecraftSources);
        var writer = new VanillaKeyEnumWriter(Path.of(args[1]));

        for (var spec : VanillaKeyEnumSpec.all()) {
            writer.write(spec, spec.entries(registrySources));
        }

        var packetCatalog = new VanillaPacketSources(minecraftSources).packetCatalog();
        new PacketMetadataWriter(Path.of(args[1])).write(packetCatalog);
    }
}
