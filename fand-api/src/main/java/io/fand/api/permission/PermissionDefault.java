package io.fand.api.permission;

public enum PermissionDefault {
    TRUE,
    FALSE,
    OPERATOR,
    NOT_OPERATOR;

    public boolean value(boolean operator) {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            case OPERATOR -> operator;
            case NOT_OPERATOR -> !operator;
        };
    }
}
