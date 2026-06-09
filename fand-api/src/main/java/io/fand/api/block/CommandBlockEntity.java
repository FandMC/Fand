package io.fand.api.block;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/** Command block block entity. */
public interface CommandBlockEntity extends BlockEntity {

    String command();

    void setCommand(String command);

    int successCount();

    void setSuccessCount(int count);

    boolean trackOutput();

    void setTrackOutput(boolean trackOutput);

    Component lastOutput();

    void setLastOutput(@Nullable Component output);

    Optional<Component> customName();

    void setCustomName(@Nullable Component name);

    boolean powered();

    void setPowered(boolean powered);

    boolean automatic();

    void setAutomatic(boolean automatic);

    boolean conditionMet();

    Mode mode();

    boolean conditional();

    enum Mode {
        SEQUENCE,
        AUTO,
        REDSTONE
    }
}
