package io.fand.api.nbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Parser for the binary Note Block Studio {@code .nbs} format. */
public final class NbsParser {

    private NbsParser() {
    }

    public static NbsSong parse(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        return parse(Files.readAllBytes(path));
    }

    public static NbsSong parse(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        var output = new ByteArrayOutputStream();
        input.transferTo(output);
        return parse(output.toByteArray());
    }

    public static NbsSong parse(byte[] data) {
        Objects.requireNonNull(data, "data");
        return new Reader(data).parse();
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static final class Reader {

        private final ByteBuffer buffer;

        private Reader(byte[] data) {
            this.buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        }

        private NbsSong parse() {
            var header = readHeader();
            var notes = readNotes(header);
            header = withLegacySongLength(header, notes);
            var layers = readLayers(header);
            var instruments = readCustomInstruments(header);
            ensureFullyConsumed();
            return new NbsSong(header, notes, layers, instruments);
        }

        private NbsHeader withLegacySongLength(NbsHeader header, List<NbsNote> notes) {
            if (header.version() <= 0 || header.version() >= 3 || header.songLengthTicks() != 0) {
                return header;
            }
            var lastTick = notes.stream().mapToInt(NbsNote::tick).max().orElse(0);
            return new NbsHeader(
                    header.version(),
                    header.firstCustomInstrument(),
                    lastTick,
                    header.layerCount(),
                    header.name(),
                    header.author(),
                    header.originalAuthor(),
                    header.description(),
                    header.ticksPerSecond(),
                    header.autoSave(),
                    header.autoSaveIntervalMinutes(),
                    header.timeSignatureBeats(),
                    header.minutesSpent(),
                    header.leftClicks(),
                    header.rightClicks(),
                    header.blocksAdded(),
                    header.blocksRemoved(),
                    header.importedFileName(),
                    header.loop(),
                    header.loopCount(),
                    header.loopStartTick());
        }

        private NbsHeader readHeader() {
            var sizeOrMarker = readShort("song length or modern header marker");
            int version;
            int firstCustomInstrument = 16;
            int songLengthTicks = sizeOrMarker;
            if (sizeOrMarker == 0) {
                version = readUnsignedByte("NBS version");
                firstCustomInstrument = readUnsignedByte("first custom instrument index");
                if (version >= 3) {
                    songLengthTicks = unsignedShortWithSignedOverflow(readShort("song length"));
                }
            } else {
                version = 0;
            }

            var layerCount = readShort("layer count");
            return new NbsHeader(
                    version,
                    firstCustomInstrument,
                    songLengthTicks,
                    layerCount,
                    emptyToNull(readString("song name")),
                    emptyToNull(readString("song author")),
                    emptyToNull(readString("song original author")),
                    emptyToNull(readString("song description")),
                    readShort("tempo") / 100.0D,
                    readBoolean("auto-save enabled"),
                    readUnsignedByte("auto-save interval"),
                    readUnsignedByte("time signature"),
                    readInt("minutes spent"),
                    readInt("left clicks"),
                    readInt("right clicks"),
                    readInt("blocks added"),
                    readInt("blocks removed"),
                    emptyToNull(readString("imported file name")),
                    version >= 4 && readBoolean("loop enabled"),
                    version >= 4 ? readUnsignedByte("loop count") : 0,
                    version >= 4 ? readShort("loop start tick") : 0);
        }

        private List<NbsNote> readNotes(NbsHeader header) {
            var notes = new ArrayList<NbsNote>();
            var tick = -1;
            while (true) {
                var jumpTicks = readShort("note tick jump");
                if (jumpTicks == 0) {
                    break;
                }
                tick += jumpTicks;

                var layer = -1;
                while (true) {
                    var jumpLayers = readShort("note layer jump");
                    if (jumpLayers == 0) {
                        break;
                    }
                    layer += jumpLayers;

                    var instrument = readUnsignedByte("note instrument");
                    var key = readUnsignedByte("note key");
                    var volume = 100;
                    var panning = 0;
                    var pitch = 0.0D;
                    if (header.version() >= 4) {
                        volume = readUnsignedByte("note volume");
                        panning = readUnsignedByte("note panning") - 100;
                        pitch = readShort("note pitch") / 100.0D;
                    }
                    notes.add(new NbsNote(tick, layer, instrument, key, volume, panning, pitch));
                }
            }
            return notes;
        }

        private List<NbsLayer> readLayers(NbsHeader header) {
            var layers = new ArrayList<NbsLayer>(Math.max(0, header.layerCount()));
            for (int index = 0; index < header.layerCount(); index++) {
                var name = emptyToNull(readString("layer name"));
                var status = NbsLayerStatus.NONE;
                if (header.version() >= 4) {
                    status = NbsLayerStatus.fromId(readUnsignedByte("layer status"));
                }
                var volume = readUnsignedByte("layer volume");
                var panning = header.version() >= 2
                        ? readUnsignedByte("layer panning") - 100
                        : 0;
                layers.add(new NbsLayer(index, name, status, volume, panning));
            }
            return layers;
        }

        private List<NbsCustomInstrument> readCustomInstruments(NbsHeader header) {
            var count = readUnsignedByte("custom instrument count");
            var instruments = new ArrayList<NbsCustomInstrument>(Math.max(0, count));
            for (int index = 0; index < count; index++) {
                instruments.add(new NbsCustomInstrument(
                        index,
                        header.firstCustomInstrument() + index,
                        emptyToNull(readString("custom instrument name")),
                        emptyToNull(readString("custom instrument sound file")),
                        readUnsignedByte("custom instrument key"),
                        readBoolean("custom instrument press key")));
            }
            return instruments;
        }

        private int unsignedShortWithSignedOverflow(short value) {
            return value >= 0 ? value : value & 0xFFFF;
        }

        private boolean readBoolean(String field) {
            return readByte(field) != 0;
        }

        private int readByte(String field) {
            requireRemaining(1, field);
            return buffer.get();
        }

        private int readUnsignedByte(String field) {
            requireRemaining(1, field);
            return Byte.toUnsignedInt(buffer.get());
        }

        private short readShort(String field) {
            requireRemaining(2, field);
            return buffer.getShort();
        }

        private int readInt(String field) {
            requireRemaining(4, field);
            return buffer.getInt();
        }

        private String readString(String field) {
            var length = readInt(field + " length");
            if (length < 0) {
                throw new NbsParseException("Negative string length for " + field + ": " + length);
            }
            requireRemaining(length, field);
            var bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }

        private void requireRemaining(int bytes, String field) {
            if (buffer.remaining() < bytes) {
                throw new NbsParseException(
                        "Unexpected end of NBS data while reading " + field
                                + " at byte " + buffer.position()
                                + " (needed " + bytes + ", remaining " + buffer.remaining() + ")");
            }
        }

        private void ensureFullyConsumed() {
            if (buffer.hasRemaining()) {
                throw new NbsParseException("Trailing unread NBS data at byte " + buffer.position()
                        + " (" + buffer.remaining() + " bytes)");
            }
        }
    }
}
