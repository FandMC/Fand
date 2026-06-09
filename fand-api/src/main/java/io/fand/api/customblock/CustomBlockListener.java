package io.fand.api.customblock;

/**
 * Optional callbacks for a custom block. All callbacks run on the server
 * thread.
 */
public interface CustomBlockListener {

    default void placed(CustomBlockContext context) {
    }

    default void broken(CustomBlockContext context) {
    }

    default void loaded(CustomBlockContext context) {
    }

    default void unloaded(CustomBlockContext context) {
    }

    default void tick(CustomBlockContext context) {
    }
}
