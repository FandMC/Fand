package io.fand.datagenerator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class KeyNames {

    private static final String DEFAULT_NAMESPACE = "minecraft";

    private KeyNames() {
    }

    static String vanillaKey(String path) {
        return path.contains(":") ? path : DEFAULT_NAMESPACE + ":" + path;
    }

    static String enumNameToPath(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    static String keyToEnumName(String key) {
        var path = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        var name = path.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        name = name.replaceAll("^_+", "").replaceAll("_+$", "");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Cannot create enum name from key: " + key);
        }
        if (Character.isDigit(name.charAt(0))) {
            return "_" + name;
        }
        return name;
    }

    static Map<String, String> entriesByName(List<KeyEntry> entries) {
        var map = new LinkedHashMap<String, String>();
        for (var entry : entries) {
            map.put(entry.name(), entry.key());
        }
        return map;
    }
}
