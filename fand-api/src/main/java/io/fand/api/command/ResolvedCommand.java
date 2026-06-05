package io.fand.api.command;

public record ResolvedCommand(
        RegisteredCommand command,
        int matchedLength,
        String usedLabel
) {
}
