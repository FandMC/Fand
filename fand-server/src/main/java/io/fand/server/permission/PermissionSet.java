package io.fand.server.permission;

import io.fand.api.permission.PermissionSubject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PermissionSet implements PermissionSubject {

    private final boolean operator;
    private final LinkedHashMap<String, Boolean> entries = new LinkedHashMap<>();

    public PermissionSet(boolean operator) {
        this.operator = operator;
    }

    public PermissionSet set(String node, boolean value) {
        entries.put(node, value);
        return this;
    }

    @Override
    public boolean operator() {
        return operator;
    }

    @Override
    public Optional<Boolean> permissionValue(String node) {
        return Optional.ofNullable(entries.get(node));
    }

    public Map<String, Boolean> entries() {
        return Map.copyOf(entries);
    }
}
