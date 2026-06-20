package io.fand.api.nbs;

import java.util.Optional;

/** A single note entry positioned by song tick and layer. */
public record NbsNote(
        int tick,
        int layer,
        int instrument,
        int key,
        int volume,
        int panning,
        double pitch
) {

    public Optional<NbsVanillaInstrument> vanillaInstrument() {
        return NbsVanillaInstrument.byId(instrument);
    }

    public double effectiveKey() {
        return key + pitch;
    }
}
