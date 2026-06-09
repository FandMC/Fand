package io.fand.datagenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class PacketMetadataWriter {

    private static final String PACKET_PACKAGE = "io.fand.api.packet";

    private final Path outputSources;

    PacketMetadataWriter(Path outputSources) {
        this.outputSources = outputSources;
    }

    void write(PacketCatalog catalog) throws IOException {
        writePacketType(catalog);
        for (var view : catalog.views()) {
            writeView(view);
        }
    }

    private void writePacketType(PacketCatalog catalog) throws IOException {
        var outputFile = outputSources
                .resolve(Path.of(PACKET_PACKAGE.replace('.', '/')))
                .resolve("PacketType.java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, packetTypeSource(catalog), StandardCharsets.UTF_8);
    }

    private void writeView(PacketViewModel view) throws IOException {
        var outputFile = outputSources
                .resolve(Path.of(PacketViewModel.PACKAGE_NAME.replace('.', '/')))
                .resolve(view.typeName() + ".java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, viewSource(view), StandardCharsets.UTF_8);
    }

    private static String packetTypeSource(PacketCatalog catalog) {
        var source = new StringBuilder();
        source.append("package ").append(PACKET_PACKAGE).append(";\n\n");
        source.append("import java.util.Objects;\n");
        source.append("import java.util.Optional;\n");
        source.append("import net.kyori.adventure.key.Key;\n\n");
        source.append("/** Generated vanilla packet metadata. */\n");
        source.append("public enum PacketType {\n\n");
        for (int i = 0; i < catalog.packets().size(); i++) {
            var packet = catalog.packets().get(i);
            source.append("    ").append(packet.enumName()).append("(")
                    .append("PacketProtocol.").append(packet.protocolName()).append(", ")
                    .append("PacketDirection.").append(packet.directionName()).append(", ")
                    .append("\"").append(packet.key()).append("\", ")
                    .append(packet.view().qualifiedName()).append(".class, ")
                    .append("\"").append(packet.sourceClassName()).append("\")")
                    .append(i == catalog.packets().size() - 1 ? ";\n\n" : ",\n");
        }
        source.append("    private final PacketProtocol protocol;\n");
        source.append("    private final PacketDirection direction;\n");
        source.append("    private final Key key;\n");
        source.append("    private final Class<? extends PacketView> viewType;\n");
        source.append("    private final String vanillaClassName;\n\n");
        source.append("    PacketType(PacketProtocol protocol, PacketDirection direction, String key, Class<? extends PacketView> viewType, String vanillaClassName) {\n");
        source.append("        this.protocol = protocol;\n");
        source.append("        this.direction = direction;\n");
        source.append("        this.key = Key.key(key);\n");
        source.append("        this.viewType = viewType;\n");
        source.append("        this.vanillaClassName = vanillaClassName;\n");
        source.append("    }\n\n");
        source.append("    public PacketProtocol protocol() {\n");
        source.append("        return protocol;\n");
        source.append("    }\n\n");
        source.append("    public PacketDirection direction() {\n");
        source.append("        return direction;\n");
        source.append("    }\n\n");
        source.append("    public Key key() {\n");
        source.append("        return key;\n");
        source.append("    }\n\n");
        source.append("    public Class<? extends PacketView> viewType() {\n");
        source.append("        return viewType;\n");
        source.append("    }\n\n");
        source.append("    public String vanillaClassName() {\n");
        source.append("        return vanillaClassName;\n");
        source.append("    }\n\n");
        source.append("    public String asString() {\n");
        source.append("        return protocol.id() + \"/\" + direction.name().toLowerCase(java.util.Locale.ROOT) + \"/\" + key.asString();\n");
        source.append("    }\n\n");
        source.append("    public static Optional<PacketType> find(PacketProtocol protocol, PacketDirection direction, Key key) {\n");
        source.append("        Objects.requireNonNull(protocol, \"protocol\");\n");
        source.append("        Objects.requireNonNull(direction, \"direction\");\n");
        source.append("        Objects.requireNonNull(key, \"key\");\n");
        source.append("        for (var type : values()) {\n");
        source.append("            if (type.protocol == protocol && type.direction == direction && type.key.equals(key)) {\n");
        source.append("                return Optional.of(type);\n");
        source.append("            }\n");
        source.append("        }\n");
        source.append("        return Optional.empty();\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public String toString() {\n");
        source.append("        return asString();\n");
        source.append("    }\n");
        source.append("}\n");
        return source.toString();
    }

    private static String viewSource(PacketViewModel view) {
        var source = new StringBuilder();
        source.append("package ").append(PacketViewModel.PACKAGE_NAME).append(";\n\n");
        source.append("import io.fand.api.packet.PacketView;\n\n");
        source.append("/** Generated view for {@code ").append(view.sourceClassName()).append("}. */\n");
        source.append("public interface ").append(view.typeName()).append(" extends PacketView {\n");
        for (var field : view.fields()) {
            source.append("\n");
            if (field.wildcardType()) {
                source.append("    @SuppressWarnings(\"unchecked\")\n");
                source.append("    default ").append(field.apiType()).append(" ").append(field.accessorName()).append("() {\n");
                source.append("        return (").append(field.apiType()).append(") value(\"")
                        .append(field.name()).append("\", ").append(field.classLiteral()).append(");\n");
            } else {
                source.append("    default ").append(field.apiType()).append(" ").append(field.accessorName()).append("() {\n");
                source.append("        return value(\"").append(field.name()).append("\", ")
                        .append(field.classLiteral()).append(");\n");
            }
            source.append("    }\n");
            if (view.replaceable()) {
                source.append("\n");
                source.append("    default ").append(view.typeName()).append(" with")
                        .append(upperCamel(field.accessorName())).append("(")
                        .append(field.apiType()).append(" value) {\n");
                source.append("        return with(\"").append(field.name()).append("\", value, ")
                        .append(view.typeName()).append(".class);\n");
                source.append("    }\n");
            }
        }
        source.append("}\n");
        return source.toString();
    }

    private static String upperCamel(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
