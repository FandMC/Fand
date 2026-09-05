package io.fand.server.command;

/** Coalesces registry changes into one client refresh per server tick. */
public final class CommandTreeSynchronizer {

    private final CommandManager commands;
    private long synchronizedRevision;

    public CommandTreeSynchronizer(CommandManager commands) {
        this.commands = commands;
        this.synchronizedRevision = commands.revision();
    }

    /** Called only on the server thread while player connections are available. */
    public void tick(Runnable refresh) {
        long revision = commands.revision();
        if (revision == synchronizedRevision) {
            return;
        }
        refresh.run();
        // Keep changes made during the refresh pending for the next tick.
        synchronizedRevision = revision;
    }
}
