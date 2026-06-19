package io.fand.api.permission;

import java.util.Map;
import java.util.Objects;

public record PermissionDescriptor(String node, PermissionDefault defaultAccess, Map<String, Boolean> children) {

    public PermissionDescriptor(String node, PermissionDefault defaultAccess) {
        this(node, defaultAccess, Map.of());
    }

    public PermissionDescriptor {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(defaultAccess, "defaultAccess");
        children = Map.copyOf(children);
    }
}
