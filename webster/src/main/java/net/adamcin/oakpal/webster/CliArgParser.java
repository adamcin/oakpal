/*
 * Copyright 2019 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.webster;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple parser for reliably turning a single property String value into an array of String arguments for passing to
 * CLI parsers, like oak-run's. Handles double-quotes and escapes to support any path character that might be needed.
 */
public final class CliArgParser {
    private CliArgParser() {
        /* no construction */
    }

    enum TokenType {
        WHITESPACE("\\s"),
        ESCAPE("\\\\"),
        QUOTE("\""),
        ARG("[^\"\\s\\\\]+");

        final String pattern;

        TokenType(final String pattern) {
            this.pattern = pattern;
        }

        Token nextFromMatcher(final Matcher matcher) {
            if (matcher.group(this.name()) != null) {
                return new Token(this, matcher.group(this.name()));
            } else {
                return null;
            }
        }

        static Pattern tokenPattern() {
            final StringBuilder sb = new StringBuilder();
            for (TokenType tokenType : TokenType.values()) {
                sb.append(String.format("|(?<%s>%s)", tokenType.name(), tokenType.pattern));
            }
            return Pattern.compile(sb.substring(1));
        }

    }

    static final class Token {
        final TokenType type;
        final String data;

        Token(final TokenType type, final String data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("(%s %s)", type.name(), data);
        }
    }

    static List<Token> lex(final String input) {
        final List<Token> tokens = new ArrayList<>();

        final Matcher matcher = TokenType.tokenPattern().matcher(input);
        match:
        while (matcher.find()) {
            for (TokenType tokenType : TokenType.values()) {
                final Token token = tokenType.nextFromMatcher(matcher);
                if (token != null) {
                    tokens.add(token);
                    continue match;
                }
            }
        }

        return tokens;
    }

    static final class Parser {
        final List<String> args = new ArrayList<>();
        StringBuilder arg = new StringBuilder();
        boolean escaped;
        boolean quoted;

        void parseNext(final Token token) {
            switch (token.type) {
                case WHITESPACE:
                    onWhitespace(token);
                    break;
                case ESCAPE:
                    onEscape(token);
                    break;
                case QUOTE:
                    onQuote(token);
                    break;
                case ARG:
                default:
                    onArg(token);
            }
        }

        private void onWhitespace(final Token token) {
            if (escaped) {
                arg.append(token.data);
                escaped = false;
            } else if (quoted) {
                arg.append(token.data);
            } else if (arg.length() > 0) {
                args.add(arg.toString());
                arg = new StringBuilder();
            }
        }

        private void onEscape(final Token token) {
            if (escaped) {
                arg.append(token.data);
                escaped = false;
            } else {
                escaped = true;
            }
        }

        private void onArg(final Token token) {
            this.arg.append(token.data);
            this.escaped = false;
        }

        private void onQuote(final Token token) {
            if (this.escaped) {
                this.arg.append(token.data);
                this.escaped = false;
            } else {
                this.quoted = !this.quoted;
            }
        }

        List<String> getArgs() {
            if (escaped) {
                throw new IllegalStateException("Unterminated escape sequence.");
            }
            if (quoted) {
                throw new IllegalStateException("Unterminated quote (\") sequence.");
            }
            if (arg.length() > 0) {
                args.add(arg.toString());
                arg = new StringBuilder();
            }
            return args;
        }
    }

    public static String[] parse(final String input) {
        List<Token> tokens = lex(input);
        Parser parser = new Parser();
        for (Token token : tokens) {
            parser.parseNext(token);
        }
        return parser.getArgs().toArray(new String[0]);
    }
}
