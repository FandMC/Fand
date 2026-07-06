package io.fand.server.command;

import io.fand.api.command.Aliases;
import io.fand.api.command.Arg;
import io.fand.api.command.Argument;
import io.fand.api.command.Arguments;
import io.fand.api.command.Command;
import io.fand.api.command.CommandBuilder;
import io.fand.api.command.CommandContext;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSuggestionProvider;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;
import io.fand.api.command.Subcommand;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AnnotatedCommands {

    private AnnotatedCommands() {
    }

    public static CommandRegistration register(CommandRegistry registry, Object command) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(command, "command");

        var namespace = registry instanceof io.fand.server.plugin.PluginCommandRegistry pluginRegistry ? pluginRegistry.namespace() : "";
        return register(registry, namespace, command);
    }

    public static CommandRegistration register(CommandRegistry registry, String namespace, Object command) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(command, "command");

        var annotation = command.getClass().getAnnotation(Command.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Command class is missing @Command: " + command.getClass().getName());
        }
        return registerStructured(registry, namespace, command, annotation);
    }

    private static CommandRegistration registerStructured(
            CommandRegistry registry,
            String namespace,
            Object command,
            Command commandAnnotation
    ) {
        var builder = new CommandBuilder(commandAnnotation.value());
        var resolvedNamespace = commandAnnotation.namespace().isBlank() ? namespace : commandAnnotation.namespace();
        if (resolvedNamespace.isBlank()) {
            throw new IllegalArgumentException("A namespace is required for command class " + command.getClass().getName());
        }
        builder.namespace(resolvedNamespace);

        var aliases = command.getClass().getAnnotation(Aliases.class);
        if (aliases != null) {
            builder.aliases(Arrays.asList(aliases.value()));
        }
        var permission = command.getClass().getAnnotation(Permission.class);
        if (permission != null) {
            builder.permission(permission.value());
        }

        var methods = Arrays.stream(command.getClass().getDeclaredMethods())
                .filter(AnnotatedCommands::isCommandMethod)
                .sorted(Comparator.comparing(Method::getName))
                .toList();
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("Command class has no @Default or @Subcommand methods: " + command.getClass().getName());
        }
        for (var method : methods) {
            registerMethod(builder, command, method);
        }
        return registry.register(builder.build());
    }

    private static void registerMethod(CommandBuilder root, Object command, Method method) {
        method.setAccessible(true);
        CommandBuilder current = root;
        var subcommand = method.getAnnotation(Subcommand.class);
        if (subcommand != null) {
            for (var segment : subcommand.value().trim().split("\\s+")) {
                if (!segment.isBlank()) {
                    current = current.literal(segment);
                }
            }
        }
        var permission = method.getAnnotation(Permission.class);
        if (permission != null) {
            current.permission(permission.value());
        }
        var invoker = new MethodInvoker(command, method);
        for (var parameter : method.getParameters()) {
            var arg = parameter.getAnnotation(Arg.class);
            if (arg != null) {
                current = current.argument(arg.value(), argument(parameter, arg));
            }
        }
        current.executes(invoker);
    }

    private static boolean isCommandMethod(Method method) {
        return method.isAnnotationPresent(Default.class) || method.isAnnotationPresent(Subcommand.class);
    }

    private static Argument<?> argument(Parameter parameter, Arg arg) {
        Argument<?> argument = switch (arg.type()) {
            case BOOLEAN -> Arguments.bool();
            case INTEGER -> Arguments.integer(arg.min(), arg.max());
            case LONG -> Arguments.longValue(arg.min(), arg.max());
            case FLOAT -> Arguments.floatValue();
            case DOUBLE -> Arguments.doubleValue();
            case PLAYER -> Arguments.player();
            case REGISTRY_KEY -> parameter.getType() == ItemType.class ? Arguments.item() : Arguments.string();
            case ENUM -> Arguments.enumValue(Arrays.asList(arg.suggestions()));
            case GREEDY_STRING -> Arguments.greedyString();
            case STRING -> Arguments.string();
            default -> inferArgument(parameter);
        };
        if (arg.suggestions().length > 0 && arg.type() != io.fand.api.command.CommandArgumentType.ENUM) {
            argument = argument.suggests(Arrays.asList(arg.suggestions()));
        }
        if (arg.optionalSender()) {
            argument = argument.optionalSender();
        } else if (arg.optional()) {
            argument = optional(argument, parameter.getType(), arg);
        }
        return argument;
    }

    private static Argument<?> inferArgument(Parameter parameter) {
        var type = parameter.getType();
        if (type == int.class || type == Integer.class) {
            return Arguments.integer();
        }
        if (type == long.class || type == Long.class) {
            return Arguments.longValue();
        }
        if (type == boolean.class || type == Boolean.class) {
            return Arguments.bool();
        }
        if (type == float.class || type == Float.class) {
            return Arguments.floatValue();
        }
        if (type == double.class || type == Double.class) {
            return Arguments.doubleValue();
        }
        if (type == Player.class) {
            return Arguments.player();
        }
        if (type == ItemType.class) {
            return Arguments.item();
        }
        return Arguments.string();
    }

    private static Argument<?> optional(Argument<?> argument, Class<?> type, Arg arg) {
        if (type == int.class || type == Integer.class) {
            return cast(argument, Integer.class).asOptional(arg.defaultInt());
        }
        if (type == long.class || type == Long.class) {
            return cast(argument, Long.class).asOptional(arg.defaultLong());
        }
        if (type == boolean.class || type == Boolean.class) {
            return cast(argument, Boolean.class).asOptional(arg.defaultBoolean());
        }
        if (type == double.class || type == Double.class) {
            return cast(argument, Double.class).asOptional(arg.defaultDouble());
        }
        if (type == float.class || type == Float.class) {
            return cast(argument, Float.class).asOptional((float) arg.defaultDouble());
        }
        if (!arg.defaultValue().isBlank()) {
            return cast(argument, String.class).asOptional(arg.defaultValue());
        }
        return argument.asOptional();
    }

    private static <T> Argument<T> cast(Argument<?> argument, Class<T> type) {
        if (!argument.valueType().equals(type)) {
            throw new IllegalArgumentException("Argument type mismatch: expected " + type.getName() + ", got " + argument.valueType().getName());
        }
        return unsafeCast(argument);
    }

    @SuppressWarnings("unchecked")
    private static <T> Argument<T> unsafeCast(Argument<?> argument) {
        return (Argument<T>) argument;
    }

    private static final class MethodInvoker implements io.fand.api.command.CommandAction, CommandSuggestionProvider {

        private final Object target;
        private final Method method;
        private final List<ParameterBinding> bindings;

        private MethodInvoker(Object target, Method method) {
            this.target = target;
            this.method = method;
            this.bindings = bindings(method);
        }

        @Override
        public void execute(CommandContext context) throws Exception {
            invoke(context);
        }

        @Override
        public List<String> suggest(CommandContext context) {
            return List.of();
        }

        private void invoke(CommandContext context) throws Exception {
            var values = new Object[bindings.size()];
            for (int i = 0; i < bindings.size(); i++) {
                values[i] = bindings.get(i).value(context);
            }
            try {
                method.invoke(target, values);
            } catch (InvocationTargetException ex) {
                var cause = ex.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw ex;
            }
        }

        private static List<ParameterBinding> bindings(Method method) {
            var bindings = new ArrayList<ParameterBinding>();
            for (var parameter : method.getParameters()) {
                var arg = parameter.getAnnotation(Arg.class);
                if (arg == null) {
                    if (parameter.getType() == CommandContext.class) {
                        bindings.add(CommandContext.class::cast);
                    } else {
                        throw new IllegalArgumentException("Command method parameter is missing @Arg: " + method);
                    }
                } else {
                    bindings.add(context -> {
                        var type = boxed(parameter.getType());
                        if (!arg.optional() && !arg.optionalSender()) {
                            return context.argument(arg.value(), type);
                        }
                        return context.optionalArgument(arg.value(), type).orElse(null);
                    });
                }
            }
            return List.copyOf(bindings);
        }

        private static Class<?> boxed(Class<?> type) {
            if (!type.isPrimitive()) {
                return type;
            }
            if (type == int.class) {
                return Integer.class;
            }
            if (type == long.class) {
                return Long.class;
            }
            if (type == boolean.class) {
                return Boolean.class;
            }
            if (type == float.class) {
                return Float.class;
            }
            if (type == double.class) {
                return Double.class;
            }
            return type;
        }
    }

    @FunctionalInterface
    private interface ParameterBinding {
        Object value(CommandContext context);
    }
}
