package io.fand.api.nms;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class NmsHookResult {

    private static final NmsHookResult PASS = new NmsHookResult(Action.PASS, null);
    private static final NmsHookResult CANCEL = new NmsHookResult(Action.CANCEL, null);

    private final Action action;
    private final @Nullable Object replacement;

    private NmsHookResult(Action action, @Nullable Object replacement) {
        this.action = Objects.requireNonNull(action, "action");
        this.replacement = replacement;
    }

    public static NmsHookResult pass() {
        return PASS;
    }

    public static NmsHookResult cancel() {
        return CANCEL;
    }

    public static NmsHookResult replace(@Nullable Object replacement) {
        return new NmsHookResult(Action.REPLACE, replacement);
    }

    public Action action() {
        return action;
    }

    public @Nullable Object replacementOrNull() {
        return replacement;
    }

    public enum Action {
        PASS,
        CANCEL,
        REPLACE
    }
}
