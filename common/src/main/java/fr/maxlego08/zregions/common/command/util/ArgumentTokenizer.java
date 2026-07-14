/*
 * Ported from LuckPerms (https://github.com/LuckPerms/LuckPerms), MIT License.
 * Copyright (c) lucko (Luck) <luck@lucko.me>, Copyright (c) contributors
 */
package fr.maxlego08.zregions.common.command.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes command input into distinct argument tokens: splits on spaces, except
 * when the content is enclosed in double quotes (straight or smart).
 */
public enum ArgumentTokenizer {

    /** For execution: a trailing empty token is discarded. */
    EXECUTE {
        @Override
        public List<String> tokenizeInput(String input) {
            return new QuotedStringTokenizer(input).tokenize(true);
        }
    },

    /** For tab completion: a trailing empty token is preserved, it carries the cursor. */
    TAB_COMPLETE {
        @Override
        public List<String> tokenizeInput(String input) {
            return new QuotedStringTokenizer(input).tokenize(false);
        }
    };

    public List<String> tokenizeInput(String[] args) {
        return tokenizeInput(String.join(" ", args));
    }

    public abstract List<String> tokenizeInput(String input);

    private static final class QuotedStringTokenizer {

        private final String string;
        private int cursor;

        QuotedStringTokenizer(String string) {
            this.string = string;
        }

        List<String> tokenize(boolean omitEmptyTokenAtEnd) {
            List<String> output = new ArrayList<>();
            while (hasNext()) {
                output.add(readToken());
            }
            if (!omitEmptyTokenAtEnd && this.cursor > 0 && isWhitespace(peek(-1))) {
                output.add("");
            }
            return output;
        }

        private static boolean isQuoteCharacter(char c) {
            return c == '"' || c == '“' || c == '”';
        }

        private static boolean isWhitespace(char c) {
            return c == ' ';
        }

        private String readToken() {
            return isQuoteCharacter(peek()) ? readQuotedToken() : readUnquotedToken();
        }

        private String readUnquotedToken() {
            int start = this.cursor;
            while (hasNext() && !isWhitespace(peek())) {
                skip();
            }
            int end = this.cursor;

            if (hasNext()) {
                skip(); // skip whitespace
            }
            return this.string.substring(start, end);
        }

        private String readQuotedToken() {
            skip(); // skip start quote

            int start = this.cursor;
            while (hasNext() && !isQuoteCharacter(peek())) {
                skip();
            }
            int end = this.cursor;

            if (hasNext()) {
                skip(); // skip end quote
            }
            if (hasNext() && isWhitespace(peek())) {
                skip(); // skip whitespace
            }
            return this.string.substring(start, end);
        }

        private boolean hasNext() {
            return this.cursor + 1 <= this.string.length();
        }

        private char peek() {
            return this.string.charAt(this.cursor);
        }

        private char peek(int offset) {
            return this.string.charAt(this.cursor + offset);
        }

        private void skip() {
            this.cursor++;
        }
    }
}
