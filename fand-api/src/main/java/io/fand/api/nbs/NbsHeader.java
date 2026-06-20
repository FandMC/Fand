package io.fand.api.nbs;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Metadata and playback settings from a Note Block Studio file header. */
public record NbsHeader(
        int version,
        int firstCustomInstrument,
        int songLengthTicks,
        int layerCount,
        @Nullable String name,
        @Nullable String author,
        @Nullable String originalAuthor,
        @Nullable String description,
        double ticksPerSecond,
        boolean autoSave,
        int autoSaveIntervalMinutes,
        int timeSignatureBeats,
        int minutesSpent,
        int leftClicks,
        int rightClicks,
        int blocksAdded,
        int blocksRemoved,
        @Nullable String importedFileName,
        boolean loop,
        int loopCount,
        int loopStartTick
) {

    public Optional<String> nameOptional() {
        return Optional.ofNullable(name);
    }

    public Optional<String> authorOptional() {
        return Optional.ofNullable(author);
    }

    public Optional<String> originalAuthorOptional() {
        return Optional.ofNullable(originalAuthor);
    }

    public Optional<String> descriptionOptional() {
        return Optional.ofNullable(description);
    }

    public Optional<String> importedFileNameOptional() {
        return Optional.ofNullable(importedFileName);
    }

    public double millisecondsPerTick() {
        return ticksPerSecond == 0.0D ? 0.0D : 1000.0D / ticksPerSecond;
    }

    public double beatsPerMinute() {
        return ticksPerSecond * timeSignatureBeats * 60.0D / 4.0D;
    }
}
