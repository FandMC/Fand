package io.fand.datagenerator;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class EntryCollector {

    private final Map<String, String> entries = new LinkedHashMap<>();
    private final Set<String> keys = new LinkedHashSet<>();

    void add(String requestedName, String key) {
        if (!keys.add(key)) {
            return;
        }
        var name = uniqueName(requestedName, key);
        entries.put(name, key);
    }

    List<KeyEntry> sorted() {
        return entries.entrySet().stream()
                .map(entry -> new KeyEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(KeyEntry::name))
                .toList();
    }

    private String uniqueName(String requestedName, String key) {
        var candidate = requestedName;
        int suffix = 2;
        while (entries.containsKey(candidate) && !Objects.equals(entries.get(candidate), key)) {
            candidate = requestedName + "_" + suffix;
            suffix++;
        }
        return candidate;
    }
}
