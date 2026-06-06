package io.fand.server.console;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.nio.file.Paths;

public final class FandConsole extends SimpleTerminalConsole {

    private final DedicatedServer server;

    public FandConsole(DedicatedServer server) {
        this.server = server;
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        builder
                .appName("Fand")
                .variable(LineReader.HISTORY_FILE, Paths.get(".console_history"))
                .option(LineReader.Option.COMPLETE_IN_WORD, true);
        return super.buildReader(builder);
    }

    @Override
    protected boolean isRunning() {
        return !this.server.isStopped() && this.server.isRunning();
    }

    @Override
    protected void runCommand(String command) {
        this.server.handleConsoleInput(command, this.server.createCommandSourceStack());
    }

    @Override
    protected void shutdown() {
        this.server.halt(false);
    }
}
