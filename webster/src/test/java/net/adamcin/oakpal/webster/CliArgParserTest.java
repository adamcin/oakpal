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

import org.junit.Test;

import static org.junit.Assert.*;

public class CliArgParserTest {

    @Test
    public void testComplexLine() {
        final String[] parsed = CliArgParser
                .parse("some \"command with\" -q\"uot\\\" e\\\"d\" args\\ and\\\\ stuff");
        assertArrayEquals("parsed is",
                new String[]{"some", "command with", "-quot\" e\"d", "args and\\", "stuff"},
                parsed);
    }

    @Test(expected = IllegalStateException.class)
    public void testUnterminatedEscape() {
        CliArgParser.parse("ends with a backslash\\");
    }

    @Test(expected = IllegalStateException.class)
    public void testUnterminatedQuote() {
        CliArgParser.parse("ends with a quote\"");
    }

    @Test
    public void testToken_toString() {
        final CliArgParser.Token token = new CliArgParser.Token(CliArgParser.TokenType.ARG, "arg");
        assertEquals("string should be", "(ARG arg)", token.toString());
    }
}