package io.fand.server.command;

import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;

public final class CommandSourceSender implements CommandSender, PermissionSubject {

    private final CommandSourceStack source;
    private final PermissionService permissions;

    public CommandSourceSender(CommandSourceStack source, PermissionService permissions) {
        this.source = source;
        this.permissions = permissions;
    }

    public CommandSourceStack source() {
        return source;
    }

    @Override
    public String name() {
        return source.getTextName();
    }

    @Override
    public void sendMessage(net.kyori.adventure.text.Component message) {
        source.sendSystemMessage(AdventureBridge.toVanilla(message, source.registryAccess()));
    }

    @Override
    public boolean can(String permission) {
        return permissions.can(this, permission);
    }

    @Override
    public boolean operator() {
        return source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
    }

    @Override
    public Optional<Boolean> permissionValue(String node) {
        return Optional.empty();
    }
}
