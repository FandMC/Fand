package io.fand.api.nbs;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Vanilla/OpenNBS instrument identifiers keyed by their binary instrument id. */
public enum NbsVanillaInstrument {
    HARP(0, "Harp", "harp.ogg", "minecraft:harp", 0),
    DOUBLE_BASS(1, "Double Bass", "dbass.ogg", "minecraft:double_bass", 0),
    BASS_DRUM(2, "Bass Drum", "bdrum.ogg", "minecraft:bass_drum", 0),
    SNARE_DRUM(3, "Snare Drum", "sdrum.ogg", "minecraft:snare_drum", 0),
    CLICK(4, "Click", "click.ogg", "minecraft:click", 0),
    GUITAR(5, "Guitar", "guitar.ogg", "minecraft:guitar", 0),
    FLUTE(6, "Flute", "flute.ogg", "minecraft:flute", 0),
    BELL(7, "Bell", "bell.ogg", "minecraft:bell", 0),
    CHIME(8, "Chime", "chime.ogg", "minecraft:chime", 0),
    XYLOPHONE(9, "Xylophone", "xylobone.ogg", "minecraft:xylophone", 0),
    IRON_XYLOPHONE(10, "Iron Xylophone", "iron_xylophone.ogg", "minecraft:iron_xylophone", 1),
    COW_BELL(11, "Cow Bell", "cow_bell.ogg", "minecraft:cow_bell", 1),
    DIDGERIDOO(12, "Didgeridoo", "didgeridoo.ogg", "minecraft:didgeridoo", 1),
    BIT(13, "Bit", "bit.ogg", "minecraft:bit", 1),
    BANJO(14, "Banjo", "banjo.ogg", "minecraft:banjo", 1),
    PLING(15, "Pling", "pling.ogg", "minecraft:pling", 1),
    TRUMPET(16, "Trumpet", "trumpet.ogg", "minecraft:trumpet", 6),
    TRUMPET_EXPOSED(17, "Exposed Trumpet", "exposed_trumpet.ogg", "minecraft:trumpet_exposed", 6),
    TRUMPET_WEATHERED(18, "Weathered Trumpet", "weathered_trumpet.ogg", "minecraft:trumpet_weathered", 6),
    TRUMPET_OXIDIZED(19, "Oxidized Trumpet", "oxidized_trumpet.ogg", "minecraft:trumpet_oxidized", 6);

    private static final List<NbsVanillaInstrument> BY_ID = List.of(values());

    private final int id;
    private final String displayName;
    private final String soundFile;
    private final Key key;
    private final int supportedVersion;

    NbsVanillaInstrument(int id, String displayName, String soundFile, String key, int supportedVersion) {
        this.id = id;
        this.displayName = displayName;
        this.soundFile = soundFile;
        this.key = Key.key(key);
        this.supportedVersion = supportedVersion;
    }

    public int id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String soundFile() {
        return soundFile;
    }

    public Key key() {
        return key;
    }

    public int supportedVersion() {
        return supportedVersion;
    }

    public static Optional<NbsVanillaInstrument> byId(int id) {
        return id >= 0 && id < BY_ID.size() ? Optional.of(BY_ID.get(id)) : Optional.empty();
    }

    public static List<NbsVanillaInstrument> supportedBy(int version) {
        return BY_ID.stream().filter(instrument -> instrument.supportedVersion <= version).toList();
    }
}
