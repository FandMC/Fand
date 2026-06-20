package io.fand.api.nbs;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Parsed Note Block Studio song. */
public record NbsSong(
        NbsHeader header,
        List<NbsNote> notes,
        List<NbsLayer> layers,
        List<NbsCustomInstrument> customInstruments
) {

    public NbsSong {
        header = Objects.requireNonNull(header, "header");
        notes = List.copyOf(notes);
        layers = List.copyOf(layers);
        customInstruments = List.copyOf(customInstruments);
    }

    public int totalNotes() {
        return notes.size();
    }

    public boolean hasNotes() {
        return !notes.isEmpty();
    }

    public Optional<NbsLayer> layer(int index) {
        return layers.stream().filter(layer -> layer.index() == index).findFirst();
    }

    public Optional<NbsCustomInstrument> customInstrumentById(int instrumentId) {
        return customInstruments.stream()
                .filter(instrument -> instrument.instrumentId() == instrumentId)
                .findFirst();
    }

    public int lastTick() {
        return notes.stream().map(NbsNote::tick).max(Comparator.naturalOrder()).orElse(0);
    }
}
