package io.fand.api.permission;

import java.util.Optional;

public interface PermissionService {

    void register(PermissionDescriptor descriptor);

    Optional<PermissionDescriptor> lookup(String node);

    boolean hasPermission(PermissionSubject subject, String node);
}
