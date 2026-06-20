package io.fand.api.nbs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

final class NbsParserTest {

    @Test
    void parsesModernSongWithLayersNotesLoopAndCustomInstruments() {
        var song = NbsParser.parse(modernSong());

        assertThat(song.header().version()).isEqualTo(6);
        assertThat(song.header().firstCustomInstrument()).isEqualTo(20);
        assertThat(song.header().songLengthTicks()).isEqualTo(12);
        assertThat(song.header().layerCount()).isEqualTo(2);
        assertThat(song.header().name()).isEqualTo("Demo");
        assertThat(song.header().author()).isEqualTo("Fand");
        assertThat(song.header().description()).isEqualTo("NBS parser test");
        assertThat(song.header().ticksPerSecond()).isEqualTo(10.0D);
        assertThat(song.header().loop()).isTrue();
        assertThat(song.header().loopCount()).isEqualTo(2);
        assertThat(song.header().loopStartTick()).isEqualTo(4);

        assertThat(song.notes()).containsExactly(
                new NbsNote(0, 0, 0, 45, 80, -25, 1.5D),
                new NbsNote(2, 1, 20, 50, 70, 10, -0.25D));
        assertThat(song.notes().getFirst().vanillaInstrument()).contains(NbsVanillaInstrument.HARP);
        assertThat(song.notes().getLast().vanillaInstrument()).isEmpty();

        assertThat(song.layers()).containsExactly(
                new NbsLayer(0, "Lead", NbsLayerStatus.LOCKED, 90, -10),
                new NbsLayer(1, null, NbsLayerStatus.SOLO, 75, 20));
        assertThat(song.customInstruments()).containsExactly(
                new NbsCustomInstrument(0, 20, "Custom Bell", "custom_bell.ogg", 45, true));
        assertThat(song.customInstrumentById(20)).isPresent();
        assertThat(song.totalNotes()).isEqualTo(2);
        assertThat(song.lastTick()).isEqualTo(2);
    }

    @Test
    void parsesLegacyVersionZeroWithoutModernFields() {
        var song = NbsParser.parse(legacySong());

        assertThat(song.header().version()).isZero();
        assertThat(song.header().firstCustomInstrument()).isEqualTo(16);
        assertThat(song.header().songLengthTicks()).isEqualTo(8);
        assertThat(song.header().layerCount()).isEqualTo(1);
        assertThat(song.header().loop()).isFalse();
        assertThat(song.notes()).containsExactly(new NbsNote(0, 0, 5, 33, 100, 0, 0.0D));
        assertThat(song.layers()).containsExactly(new NbsLayer(0, "Legacy", NbsLayerStatus.NONE, 100, 0));
    }

    @Test
    void rejectsTruncatedInputWithContext() {
        assertThatThrownBy(() -> NbsParser.parse(new byte[] {0, 0, 6}))
                .isInstanceOf(NbsParseException.class)
                .hasMessageContaining("first custom instrument");
    }

    @Test
    void exposesVanillaInstrumentTable() {
        assertThat(NbsVanillaInstrument.byId(15)).contains(NbsVanillaInstrument.PLING);
        assertThat(NbsVanillaInstrument.byId(20)).isEmpty();
        assertThat(NbsVanillaInstrument.supportedBy(0)).contains(NbsVanillaInstrument.HARP);
        assertThat(NbsVanillaInstrument.supportedBy(0)).doesNotContain(NbsVanillaInstrument.TRUMPET);
        assertThat(NbsVanillaInstrument.TRUMPET.key().asString()).isEqualTo("minecraft:trumpet");
    }

    @Test
    void parsesOpenNbsSampleFiles() throws IOException {
        var samples = Path.of("C:/Users/winme/Desktop/git/nbs.js/tests/accuracy/samples");
        Assumptions.assumeTrue(Files.isDirectory(samples), "OpenNBS sample repository is not available");

        var simple = NbsParser.parse(samples.resolve("simple.nbs"));
        assertThat(simple.header().version()).isEqualTo(6);
        assertThat(simple.header().songLengthTicks()).isEqualTo(62);
        assertThat(simple.header().layerCount()).isEqualTo(34);
        assertThat(simple.totalNotes()).isEqualTo(49);
        assertThat(simple.customInstruments()).isEmpty();

        var complex = NbsParser.parse(samples.resolve("complex.nbs"));
        assertThat(complex.header().version()).isEqualTo(6);
        assertThat(complex.hasNotes()).isTrue();
        assertThat(complex.layers()).isNotEmpty();
    }

    private static byte[] modernSong() {
        var out = new Writer();
        out.shortValue(0);
        out.byteValue(6);
        out.byteValue(20);
        out.shortValue(12);
        out.shortValue(2);
        out.string("Demo");
        out.string("Fand");
        out.string("");
        out.string("NBS parser test");
        out.shortValue(1000);
        out.booleanValue(false);
        out.byteValue(5);
        out.byteValue(4);
        out.intValue(1);
        out.intValue(2);
        out.intValue(3);
        out.intValue(4);
        out.intValue(5);
        out.string("");
        out.booleanValue(true);
        out.byteValue(2);
        out.shortValue(4);

        out.shortValue(1);
        out.shortValue(1);
        out.byteValue(0);
        out.byteValue(45);
        out.byteValue(80);
        out.unsignedByte(75);
        out.shortValue(150);
        out.shortValue(0);
        out.shortValue(2);
        out.shortValue(2);
        out.byteValue(20);
        out.byteValue(50);
        out.byteValue(70);
        out.unsignedByte(110);
        out.shortValue(-25);
        out.shortValue(0);
        out.shortValue(0);

        out.string("Lead");
        out.byteValue(1);
        out.byteValue(90);
        out.unsignedByte(90);
        out.string("");
        out.byteValue(2);
        out.byteValue(75);
        out.unsignedByte(120);

        out.byteValue(1);
        out.string("Custom Bell");
        out.string("custom_bell.ogg");
        out.byteValue(45);
        out.booleanValue(true);
        return out.bytes();
    }

    private static byte[] legacySong() {
        var out = new Writer();
        out.shortValue(8);
        out.shortValue(1);
        out.string("Old");
        out.string("Author");
        out.string("");
        out.string("");
        out.shortValue(500);
        out.booleanValue(false);
        out.byteValue(5);
        out.byteValue(4);
        out.intValue(0);
        out.intValue(0);
        out.intValue(0);
        out.intValue(0);
        out.intValue(0);
        out.string("");

        out.shortValue(1);
        out.shortValue(1);
        out.byteValue(5);
        out.byteValue(33);
        out.shortValue(0);
        out.shortValue(0);

        out.string("Legacy");
        out.byteValue(100);

        out.byteValue(0);
        return out.bytes();
    }

    private static final class Writer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        private void booleanValue(boolean value) {
            byteValue(value ? 1 : 0);
        }

        private void byteValue(int value) {
            out.write(value & 0xFF);
        }

        private void unsignedByte(int value) {
            byteValue(value);
        }

        private void shortValue(int value) {
            out.write(value & 0xFF);
            out.write((value >>> 8) & 0xFF);
        }

        private void intValue(int value) {
            out.write(value & 0xFF);
            out.write((value >>> 8) & 0xFF);
            out.write((value >>> 16) & 0xFF);
            out.write((value >>> 24) & 0xFF);
        }

        private void string(String value) {
            var bytes = value.getBytes(StandardCharsets.ISO_8859_1);
            intValue(bytes.length);
            try {
                out.write(bytes);
            } catch (IOException ex) {
                throw new AssertionError(ex);
            }
        }

        private byte[] bytes() {
            return out.toByteArray();
        }
    }
}
