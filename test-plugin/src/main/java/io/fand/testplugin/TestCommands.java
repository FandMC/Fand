package io.fand.testplugin;

import io.fand.api.command.Arguments;
import io.fand.api.command.CommandBuilder;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class TestCommands {

    private TestCommands() {
    }

    static CommandRegistration register(CommandRegistry registry, Object command) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(command, "command");
        var spec = command.getClass().getAnnotation(TestCommand.class);
        if (spec == null) {
            throw new IllegalArgumentException("Missing @TestCommand: " + command.getClass().getName());
        }
        if (!(command instanceof TestCommandHandler handler)) {
            throw new IllegalArgumentException("Command must implement TestCommandHandler: " + command.getClass().getName());
        }
        var builder = new CommandBuilder(spec.label());
        builder.aliases(Arrays.asList(spec.aliases()));
        if (!spec.permission().isBlank()) {
            builder.permission(spec.permission());
        }
        builder.executes(context -> handler.execute(context.sender(), context.label(), List.of()));
        if (spec.arguments().length > 0 || command instanceof TestCommandTabHandler) {
            builder.argument("args", Arguments.greedyString(), args -> {
                if (command instanceof TestCommandTabHandler tabs) {
                    args.suggests(context -> tabs.complete(context.sender(), context.label(), context.args()));
                }
                args.executes(context -> handler.execute(context.sender(), context.label(), context.args()));
            });
        }
        return registry.register(builder.build());
    }

}
