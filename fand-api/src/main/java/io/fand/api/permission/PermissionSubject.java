package io.fand.api.permission;

import java.util.Optional;

public interface PermissionSubject {

    boolean operator();

    Optional<Boolean> permissionValue(String node);
}
