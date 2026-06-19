package io.fand.server.plugin;

import io.fand.api.permission.PermissionSubject;
import io.fand.api.plugin.PluginContext;
import io.fand.server.permission.PermissionSet;

public final class PermissionAttachmentTestBridge {

    public static final PermissionSubject SUBJECT = new PermissionSet(false);

    private PermissionAttachmentTestBridge() {
    }

    public static void attach(PluginContext context) {
        context.permissions().attach(SUBJECT, "fand.injected", true);
    }
}
