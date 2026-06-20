package io.fand.api.nbs;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Custom instrument definition stored after the layer section. */
public record NbsCustomInstrument(
        int index,
        int instrumentId,
        @Nullable String name,
        @Nullable String soundFile,
        int key,
        boolean pressKey
) {

    public Optional<String> nameOptional() {
        return Optional.ofNullable(name);
    }

    public Optional<String> soundFileOptional() {
        return Optional.ofNullable(soundFile);
    }
}
