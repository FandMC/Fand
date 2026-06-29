package io.fand.server.console;

import java.util.List;
import org.jline.reader.CompletingParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.ParsedLine;

final class FandConsoleCommandLine implements CompletingParsedLine {

    private final String line;
    private final int cursor;

    private FandConsoleCommandLine(String line, int cursor) {
        this.line = line;
        this.cursor = cursor;
    }

    static ParsedLine parse(String line, int cursor, Parser.ParseContext context) {
        return new FandConsoleCommandLine(line, Math.max(0, Math.min(cursor, line.length())));
    }

    @Override
    public String word() {
        return line;
    }

    @Override
    public int wordCursor() {
        return cursor;
    }

    @Override
    public int wordIndex() {
        return 0;
    }

    @Override
    public List<String> words() {
        return List.of(line);
    }

    @Override
    public String line() {
        return line;
    }

    @Override
    public int cursor() {
        return cursor;
    }

    @Override
    public CharSequence escape(CharSequence candidate, boolean complete) {
        return candidate;
    }

    @Override
    public int rawWordCursor() {
        return cursor;
    }

    @Override
    public int rawWordLength() {
        return line.length();
    }
}
