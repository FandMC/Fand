package io.fand.datagenerator;

import java.util.Objects;

record KeyEntry(String name, String key) {

    KeyEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(key, "key");
    }
}
