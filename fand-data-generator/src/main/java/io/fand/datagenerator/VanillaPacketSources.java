package io.fand.datagenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class VanillaPacketSources {

    private static final String PROTOCOL_ROOT = "net/minecraft/network/protocol";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\bpackage\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern PACKET_TYPE_PATTERN = Pattern.compile(
            "public\\s+static\\s+final\\s+PacketType<\\s*([^>]+?)\\s*>\\s+([A-Z][A-Z0-9_]*)\\s*=\\s*create(Clientbound|Serverbound)\\s*\\(\\s*\"([^\"]+)\"\\s*\\)",
            Pattern.MULTILINE);
    private static final Pattern PROTOCOL_CALL_PATTERN = Pattern.compile(
            "ProtocolInfoBuilder\\.(contextServerboundProtocol|contextClientboundProtocol|serverboundProtocol|clientboundProtocol)\\s*\\(");
    private static final Pattern PROTOCOL_NAME_PATTERN = Pattern.compile("ConnectionProtocol\\.([A-Z_]+)");
    private static final Pattern PACKET_REFERENCE_PATTERN = Pattern.compile(
            "\\.(?:addPacket|withBundlePacket)\\s*\\(\\s*([A-Za-z][A-Za-z0-9_]*PacketTypes)\\.([A-Z][A-Z0-9_]*)");
    private static final Pattern BUNDLE_DELIMITER_PATTERN = Pattern.compile(
            "\\.withBundlePacket\\s*\\([\\s\\S]*?new\\s+([A-Za-z][A-Za-z0-9_.]*)\\s*\\(");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "\\bpublic\\s+(?!static)([A-Za-z0-9_.$<>?,\\s\\[\\]]+)\\s+(get[A-Z][A-Za-z0-9_]*|is[A-Z][A-Za-z0-9_]*)\\s*\\(\\s*\\)");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "\\bprivate\\s+(?!static)(?:final\\s+)?([A-Za-z0-9_.$<>?,\\s\\[\\]]+)\\s+([a-z][A-Za-z0-9_]*)\\s*(?:=|;)");
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null");
    private static final Set<String> OBJECT_METHODS = Set.of(
            "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait");
    private static final Map<String, String> PROTOCOL_ORDER = Map.of(
            "HANDSHAKING", "0",
            "STATUS", "1",
            "LOGIN", "2",
            "CONFIGURATION", "3",
            "PLAY", "4");

    private final MinecraftSourceSet sources;
    private final Map<String, PacketDeclaration> declarationsByReference = new HashMap<>();
    private final Map<String, List<PacketDeclaration>> declarationsBySourceClass = new HashMap<>();
    private final Map<String, PacketViewModel> viewsBySourceClass = new LinkedHashMap<>();

    VanillaPacketSources(MinecraftSourceSet sources) {
        this.sources = sources;
    }

    PacketCatalog packetCatalog() throws IOException {
        collectPacketDeclarations();
        var packets = collectPacketEntries();
        return new PacketCatalog(packets, new ArrayList<>(viewsBySourceClass.values()));
    }

    private void collectPacketDeclarations() throws IOException {
        if (!declarationsByReference.isEmpty()) {
            return;
        }
        for (var file : sources.files(PROTOCOL_ROOT, "PacketTypes.java")) {
            var source = sources.read(file);
            var packageName = packageName(source, file);
            var owner = fileNameWithoutExtension(file);
            var matcher = PACKET_TYPE_PATTERN.matcher(source);
            while (matcher.find()) {
                var sourceTypeName = normalizePacketTypeName(matcher.group(1));
                var fieldName = matcher.group(2);
                var directionName = matcher.group(3).toUpperCase(Locale.ROOT);
                var key = KeyNames.vanillaKey(matcher.group(4));
                var declaration = new PacketDeclaration(
                        owner,
                        fieldName,
                        packageName,
                        sourceTypeName,
                        packageName + "." + sourceTypeName,
                        directionName,
                        key);
                declarationsByReference.put(owner + "." + fieldName, declaration);
                declarationsByReference.put(packageName + "." + owner + "." + fieldName, declaration);
                declarationsBySourceClass
                        .computeIfAbsent(declaration.sourceClassName(), ignored -> new ArrayList<>())
                        .add(declaration);
            }
        }
    }

    private List<PacketEntry> collectPacketEntries() throws IOException {
        var entries = new ArrayList<PacketEntry>();
        var usedEntryKeys = new LinkedHashSet<String>();
        var usedEnumNames = new LinkedHashMap<String, String>();
        for (var file : sources.files(PROTOCOL_ROOT, "Protocols.java")) {
            var source = sources.read(file);
            var matcher = PROTOCOL_CALL_PATTERN.matcher(source);
            while (matcher.find()) {
                var methodName = matcher.group(1);
                int open = source.indexOf('(', matcher.end() - 1);
                int close = findMatching(source, open, '(', ')');
                var call = source.substring(matcher.start(), close + 1);
                var protocolName = protocolName(call, file);
                var directionName = methodName.toLowerCase(Locale.ROOT).contains("clientbound")
                        ? "CLIENTBOUND"
                        : "SERVERBOUND";
                addReferencedPackets(entries, usedEntryKeys, usedEnumNames, protocolName, directionName, call);
                addBundleDelimiterPackets(entries, usedEntryKeys, usedEnumNames, protocolName, directionName, call);
            }
        }
        entries.sort(Comparator
                .comparing((PacketEntry entry) -> PROTOCOL_ORDER.getOrDefault(entry.protocolName(), "9"))
                .thenComparing(PacketEntry::directionName)
                .thenComparing(PacketEntry::key)
                .thenComparing(PacketEntry::enumName));
        if (entries.isEmpty()) {
            throw new IllegalStateException("No vanilla packets found in Minecraft protocol sources");
        }
        return List.copyOf(entries);
    }

    private void addReferencedPackets(
            List<PacketEntry> entries,
            Set<String> usedEntryKeys,
            Map<String, String> usedEnumNames,
            String protocolName,
            String directionName,
            String call
    ) throws IOException {
        var references = PACKET_REFERENCE_PATTERN.matcher(call);
        while (references.find()) {
            var reference = references.group(1) + "." + references.group(2);
            var declaration = declarationsByReference.get(reference);
            if (declaration == null) {
                throw new IllegalStateException("Unknown packet type reference: " + reference);
            }
            addPacketEntry(entries, usedEntryKeys, usedEnumNames, protocolName, directionName, declaration);
        }
    }

    private void addBundleDelimiterPackets(
            List<PacketEntry> entries,
            Set<String> usedEntryKeys,
            Map<String, String> usedEnumNames,
            String protocolName,
            String directionName,
            String call
    ) throws IOException {
        var delimiters = BUNDLE_DELIMITER_PATTERN.matcher(call);
        while (delimiters.find()) {
            var sourceClassName = sourceClassNameFromProtocolCall(delimiters.group(1), call);
            var declarations = declarationsBySourceClass.get(sourceClassName);
            if (declarations == null || declarations.isEmpty()) {
                continue;
            }
            addPacketEntry(entries, usedEntryKeys, usedEnumNames, protocolName, directionName, declarations.get(0));
        }
    }

    private void addPacketEntry(
            List<PacketEntry> entries,
            Set<String> usedEntryKeys,
            Map<String, String> usedEnumNames,
            String protocolName,
            String directionName,
            PacketDeclaration declaration
    ) throws IOException {
        if (!Objects.equals(directionName, declaration.directionName())) {
            throw new IllegalStateException(
                    "Packet direction mismatch for " + declaration.reference() + ": protocol has "
                            + directionName + " but declaration has " + declaration.directionName());
        }
        var entryKey = protocolName + "|" + directionName + "|" + declaration.key();
        if (!usedEntryKeys.add(entryKey)) {
            return;
        }
        var enumName = uniqueName(enumBaseName(protocolName, directionName, declaration.fieldName()), declaration.key(), usedEnumNames);
        var view = viewFor(declaration);
        entries.add(new PacketEntry(enumName, protocolName, directionName, declaration.key(), declaration.sourceClassName(), view));
    }

    private PacketViewModel viewFor(PacketDeclaration declaration) throws IOException {
        var existing = viewsBySourceClass.get(declaration.sourceClassName());
        if (existing != null) {
            return existing;
        }
        var sourceFile = sources.resolve(packetSourcePath(declaration));
        var source = sources.read(sourceFile);
        var recordComponents = recordComponents(source, simpleSourceName(declaration.sourceTypeName()));
        var fields = new LinkedHashMap<String, PacketFieldModel>();
        boolean replaceable = recordComponents.isPresent();
        if (recordComponents.isPresent()) {
            for (var field : recordComponents.get()) {
                fields.putIfAbsent(field.name(), field);
            }
        } else {
            collectGetterFields(source, fields);
            collectPrivateFields(source, fields);
        }
        var view = new PacketViewModel(viewTypeName(declaration.sourceTypeName()), declaration.sourceClassName(), replaceable, new ArrayList<>(fields.values()));
        viewsBySourceClass.put(declaration.sourceClassName(), view);
        return view;
    }

    private void collectGetterFields(String source, Map<String, PacketFieldModel> fields) {
        var matcher = METHOD_PATTERN.matcher(source);
        while (matcher.find()) {
            var type = matcher.group(1).replace('\n', ' ').replace('\r', ' ').trim();
            var methodName = matcher.group(2);
            if (OBJECT_METHODS.contains(methodName)) {
                continue;
            }
            var fieldName = methodName.startsWith("get")
                    ? decapitalize(methodName.substring(3))
                    : decapitalize(methodName.substring(2));
            fields.putIfAbsent(fieldName, field(fieldName, type));
        }
    }

    private void collectPrivateFields(String source, Map<String, PacketFieldModel> fields) {
        var matcher = FIELD_PATTERN.matcher(source);
        while (matcher.find()) {
            var type = matcher.group(1).replace('\n', ' ').replace('\r', ' ').trim();
            var name = matcher.group(2);
            fields.putIfAbsent(name, field(name, type));
        }
    }

    private Optional<List<PacketFieldModel>> recordComponents(String source, String simpleName) {
        var pattern = Pattern.compile("\\brecord\\s+" + Pattern.quote(simpleName) + "\\s*\\(");
        var matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int open = source.indexOf('(', matcher.end() - 1);
        int close = findMatching(source, open, '(', ')');
        var body = source.substring(open + 1, close);
        var fields = new ArrayList<PacketFieldModel>();
        for (var component : splitTopLevel(body, ',')) {
            var cleaned = component.replaceAll("@[A-Za-z0-9_.]+(?:\\([^)]*\\))?\\s*", "").trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            int split = lastWhitespace(cleaned);
            if (split <= 0 || split == cleaned.length() - 1) {
                continue;
            }
            var type = cleaned.substring(0, split).trim();
            var name = cleaned.substring(split + 1).trim();
            fields.add(field(name, type));
        }
        return Optional.of(fields);
    }

    private PacketFieldModel field(String name, String sourceType) {
        var apiType = apiType(sourceType);
        return new PacketFieldModel(
                name,
                accessorName(name),
                apiType.typeName(),
                apiType.classLiteral(),
                apiType.wildcardType());
    }

    private static ApiType apiType(String sourceType) {
        var type = sourceType
                .replace("final ", "")
                .replace("? extends ", "")
                .replace("? super ", "")
                .replaceAll("\\s+", " ")
                .trim();
        return switch (type) {
            case "boolean" -> new ApiType("boolean", "Boolean.class", false);
            case "byte" -> new ApiType("byte", "Byte.class", false);
            case "short" -> new ApiType("short", "Short.class", false);
            case "int" -> new ApiType("int", "Integer.class", false);
            case "long" -> new ApiType("long", "Long.class", false);
            case "float" -> new ApiType("float", "Float.class", false);
            case "double" -> new ApiType("double", "Double.class", false);
            case "char" -> new ApiType("char", "Character.class", false);
            case "String", "java.lang.String" -> new ApiType("String", "String.class", false);
            case "UUID", "java.util.UUID" -> new ApiType("java.util.UUID", "java.util.UUID.class", false);
            case "byte[]" -> new ApiType("byte[]", "byte[].class", false);
            case "int[]" -> new ApiType("int[]", "int[].class", false);
            case "long[]" -> new ApiType("long[]", "long[].class", false);
            default -> genericApiType(type);
        };
    }

    private static ApiType genericApiType(String type) {
        var raw = type.contains("<") ? type.substring(0, type.indexOf('<')).trim() : type;
        raw = raw.replace("java.util.", "");
        return switch (raw) {
            case "List" -> new ApiType("java.util.List<?>", "java.util.List.class", true);
            case "Set" -> new ApiType("java.util.Set<?>", "java.util.Set.class", true);
            case "Map" -> new ApiType("java.util.Map<?, ?>", "java.util.Map.class", true);
            case "Optional" -> new ApiType("java.util.Optional<?>", "java.util.Optional.class", true);
            case "BitSet" -> new ApiType("java.util.BitSet", "java.util.BitSet.class", false);
            case "Instant" -> new ApiType("java.time.Instant", "java.time.Instant.class", false);
            default -> new ApiType("Object", "Object.class", false);
        };
    }

    private String packetSourcePath(PacketDeclaration declaration) {
        var outerName = declaration.sourceTypeName().contains(".")
                ? declaration.sourceTypeName().substring(0, declaration.sourceTypeName().indexOf('.'))
                : declaration.sourceTypeName();
        return Path.of(declaration.packageName().replace('.', '/')).resolve(outerName + ".java").toString().replace('\\', '/');
    }

    private static String sourceClassNameFromProtocolCall(String className, String call) {
        if (className.contains(".")) {
            return className.startsWith("net.minecraft.") ? className : inferPackage(call) + "." + className;
        }
        return inferPackage(call) + "." + className;
    }

    private static String inferPackage(String call) {
        var matcher = Pattern.compile("\\b([a-z][A-Za-z0-9_.]+)\\.([A-Za-z][A-Za-z0-9_]*)\\.STREAM_CODEC").matcher(call);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "net.minecraft.network.protocol.game";
    }

    private static String packageName(String source, Path file) {
        var matcher = PACKAGE_PATTERN.matcher(source);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing package declaration in " + file);
        }
        return matcher.group(1);
    }

    private String protocolName(String call, Path file) {
        var matcher = PROTOCOL_NAME_PATTERN.matcher(call);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing ConnectionProtocol in " + sources.relativePath(file));
        }
        return matcher.group(1);
    }

    private static String normalizePacketTypeName(String typeName) {
        return typeName
                .replace("? extends ", "")
                .replace("? super ", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private static String simpleSourceName(String sourceTypeName) {
        return sourceTypeName.contains(".")
                ? sourceTypeName.substring(sourceTypeName.lastIndexOf('.') + 1)
                : sourceTypeName;
    }

    private static String viewTypeName(String sourceTypeName) {
        var compact = sourceTypeName.replace(".", "");
        return compact.endsWith("Packet")
                ? compact.substring(0, compact.length() - "Packet".length()) + "PacketView"
                : compact + "PacketView";
    }

    private static String uniqueName(String requestedName, String key, Map<String, String> usedNames) {
        var candidate = requestedName;
        int suffix = 2;
        while (usedNames.containsKey(candidate) && !Objects.equals(usedNames.get(candidate), key)) {
            candidate = requestedName + "_" + suffix;
            suffix++;
        }
        usedNames.put(candidate, key);
        return candidate;
    }

    private static String enumBaseName(String protocolName, String directionName, String fieldName) {
        if (fieldName.startsWith("CLIENTBOUND_") || fieldName.startsWith("SERVERBOUND_")) {
            return protocolName + "_" + fieldName;
        }
        return protocolName + "_" + directionName + "_" + fieldName;
    }

    private static String accessorName(String fieldName) {
        var name = sanitizeIdentifier(fieldName);
        if (JAVA_KEYWORDS.contains(name) || OBJECT_METHODS.contains(name)) {
            return name + "Value";
        }
        return name;
    }

    private static String sanitizeIdentifier(String value) {
        var builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i == 0) {
                builder.append(Character.isJavaIdentifierStart(c) ? c : '_');
            } else {
                builder.append(Character.isJavaIdentifierPart(c) ? c : '_');
            }
        }
        if (builder.length() == 0) {
            return "value";
        }
        return builder.toString();
    }

    private static String decapitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        if (value.chars().allMatch(ch -> !Character.isLetter(ch) || Character.isUpperCase(ch))) {
            return value.toLowerCase(Locale.ROOT);
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static int lastWhitespace(String value) {
        int genericDepth = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            char c = value.charAt(i);
            if (c == '>') {
                genericDepth++;
            } else if (c == '<') {
                genericDepth--;
            } else if (Character.isWhitespace(c) && genericDepth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> splitTopLevel(String value, char separator) {
        var parts = new ArrayList<String>();
        int genericDepth = 0;
        int parenDepth = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '<' -> genericDepth++;
                case '>' -> genericDepth--;
                case '(' -> parenDepth++;
                case ')' -> parenDepth--;
                default -> {
                    if (c == separator && genericDepth == 0 && parenDepth == 0) {
                        parts.add(value.substring(start, i));
                        start = i + 1;
                    }
                }
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private static int findMatching(String source, int open, char openChar, char closeChar) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && (inString || inChar)) {
                escaped = true;
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
                continue;
            }
            if (c == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) {
                continue;
            }
            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("Unmatched " + openChar + " in protocol source");
    }

    private static String fileNameWithoutExtension(Path file) {
        var name = file.getFileName().toString();
        return name.substring(0, name.length() - ".java".length());
    }

    private record PacketDeclaration(
            String owner,
            String fieldName,
            String packageName,
            String sourceTypeName,
            String sourceClassName,
            String directionName,
            String key
    ) {
        String reference() {
            return owner + "." + fieldName;
        }
    }

    private record ApiType(String typeName, String classLiteral, boolean wildcardType) {
    }
}
