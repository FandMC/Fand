package io.fand.api.world;

/**
 * Vanilla block update behavior used by a batch mutation.
 */
public enum BlockUpdateMode {
    /** Notify neighbors and clients, matching ordinary block placement/update behavior. */
    NORMAL,
    /** Notify clients but suppress neighbor updates for faster structure pastes. */
    CLIENTS_ONLY,
    /** Suppress vanilla notifications; callers are responsible for any follow-up refresh. */
    SILENT
}
