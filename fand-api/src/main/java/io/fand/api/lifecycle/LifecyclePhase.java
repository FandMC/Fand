package io.fand.api.lifecycle;

/**
 * Top-level lifecycle phase of a Fand runtime.
 *
 * <p>Phases progress strictly forward. Plugins observe transitions via
 * {@link io.fand.api.plugin.Plugin} callbacks and the corresponding events
 * fired on the {@link io.fand.api.event.EventBus}.
 */
public enum LifecyclePhase {

    /** Runtime constructed, before any plugin is loaded. */
    BOOTSTRAP,

    /** Plugins loaded, vanilla server not yet attached. */
    LOADED,

    /** Vanilla server attached and initialized; plugins about to enable. */
    STARTING,

    /** Plugins enabled and the server is accepting connections. */
    RUNNING,

    /** Server shutdown initiated; plugins are being disabled. */
    STOPPING,

    /** Runtime fully closed. */
    STOPPED
}
