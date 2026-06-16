package io.fand.datagenerator;

import java.io.IOException;
import java.nio.file.Path;

public final class FandDataGenerator {

    private FandDataGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Usage: FandDataGenerator <minecraft-source-root> <output-source-root> [api-source-root]");
        }

        var minecraftSources = new MinecraftSourceSet(Path.of(args[0]));
        var registrySources = new VanillaRegistrySources(minecraftSources);
        var outputSources = Path.of(args[1]);
        var apiSources = args.length == 3 ? Path.of(args[2]) : null;
        var writer = new VanillaKeyEnumWriter(outputSources);

        for (var spec : VanillaKeyEnumSpec.all()) {
            writer.write(spec, spec.entries(registrySources));
        }

        var packetCatalog = new VanillaPacketSources(minecraftSources).packetCatalog();
        new PacketMetadataWriter(outputSources).write(packetCatalog);

        new DamageEventMetadataWriter(outputSources, apiSources).write(registrySources.damageTypeKeys());
    }
}
