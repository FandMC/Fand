package io.fand.api.gui;

/**
 * Reacts to a player clicking a mapped slot inside an open {@code Gui}.
 *
 * <p><b>Threading:</b> {@link #click(GuiClick)} runs on the server thread —
 * the vanilla container-click packet is processed there before being routed
 * to the handler. It is safe to read and mutate world, entity, and inventory
 * state directly.
 */
@FunctionalInterface
public interface GuiSlotHandler {

    void click(GuiClick click);
}
