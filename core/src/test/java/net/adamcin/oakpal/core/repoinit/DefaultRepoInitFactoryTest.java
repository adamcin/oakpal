/*
 * Copyright 2020 Mark Adamcin
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

package net.adamcin.oakpal.core.repoinit;

import net.adamcin.oakpal.core.OakMachine;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class DefaultRepoInitFactoryTest {

    Reader readSlingExampleRepoInit() {
        return new InputStreamReader(getClass().getResourceAsStream("sling_example.txt"), StandardCharsets.UTF_8);
    }

    @Test
    public void testBindParserProcessor_realDefaults() throws Exception {
        final OakMachine.RepoInitProcessor processor =
                DefaultRepoInitFactory.bindParser(new RepoInitParserService());

        new OakMachine.Builder().build().adminInitAndInspect(admin -> {
            try (Reader reader = readSlingExampleRepoInit()) {
                processor.apply(admin, reader);
            }
        });
    }

    @Test(expected = RepoInitParsingException.class)
    public void testBindParserProcessor_throws() throws Exception {
        final OakMachine.RepoInitProcessor processor =
                DefaultRepoInitFactory.bindParser(
                        new ParserType(reader -> {
                            throw new RepoInitParsingException("expected error", null);
                        }));

        processor.apply(null, new StringReader(""));
    }

    @Test
    public void testBindParserProcessor() throws Exception {
        final OakMachine.RepoInitProcessor processor =
                DefaultRepoInitFactory.bindParser(new ParserType());

        processor.apply(null, new StringReader(""));
    }

    @Test(expected = RepoInitParsingException.class)
    public void testNoopProcessor() throws Exception {
        DefaultRepoInitFactory.NOOP_PROCESSOR.apply(null, null);
    }

    static class NeitherType {
        public NeitherType() {
            /* default constructor */
        }
    }

    @FunctionalInterface
    interface ParserFunction {
        List<Operation> parse(Reader reader) throws RepoInitParsingException;
    }

    static class ParserType implements RepoInitParser {
        private ParserFunction parserFunction;

        public ParserType() {
            this.parserFunction = (reader) -> Collections.emptyList();
        }

        public ParserType(final ParserFunction parserFunction) {
            this.parserFunction = parserFunction;
        }

        @Override
        public List<Operation> parse(final Reader reader) throws RepoInitParsingException {
            return parserFunction.parse(reader);
        }
    }

    static class ParserTypeWrongConstructor implements RepoInitParser {
        public ParserTypeWrongConstructor(final boolean anArgument) {
            /* default constructor */
        }

        @Override
        public List<Operation> parse(final Reader reader) throws RepoInitParsingException {
            return Collections.emptyList();
        }
    }

    @Test
    public void testLoadClazz() {
        final Class<? extends RepoInitParser> parserClazz =
                DefaultRepoInitFactory.loadClazz(getClass().getClassLoader(),
                        RepoInitParser.class, ParserType.class.getName());
        assertEquals("expect parser class", ParserType.class, parserClazz);

        final Class<? extends RepoInitParser> wrongConstructorClazz =
                DefaultRepoInitFactory.loadClazz(getClass().getClassLoader(),
                        RepoInitParser.class, ParserTypeWrongConstructor.class.getName());

        assertEquals("expect parser class", ParserTypeWrongConstructor.class, wrongConstructorClazz);

        final Class<? extends RepoInitParser> nullClazz =
                DefaultRepoInitFactory.loadClazz(getClass().getClassLoader(),
                        RepoInitParser.class, NeitherType.class.getName());

        assertNull("expect null type", nullClazz);

        final Class<? extends RepoInitParser> nonExistingClazz =
                DefaultRepoInitFactory.loadClazz(getClass().getClassLoader(),
                        RepoInitParser.class, ParserType.class.getName() + "NonExisting");

        assertNull("expect nonExistingClazz type", nonExistingClazz);
    }

    @Test
    public void testNewParser() {
        assertNull("null for null",
                new DefaultRepoInitFactory(null).newParser());
        assertNull("null for wrong constructor",
                new DefaultRepoInitFactory(ParserTypeWrongConstructor.class)
                        .newParser());
        assertNotNull("expect non null", new DefaultRepoInitFactory(ParserType.class).newParser());
    }

    @Test
    public void testNewOpsProcessor() {
        assertNotNull("expect ops processor", DefaultRepoInitFactory.newOpsProcessor());
    }

    @Test
    public void testNewInstance() {
        assertSame("NOOP for nulls", DefaultRepoInitFactory.NOOP_PROCESSOR,
                new DefaultRepoInitFactory(null).newInstance());
        assertNotSame("expect non-NOOP nonnulls", DefaultRepoInitFactory.NOOP_PROCESSOR,
                new DefaultRepoInitFactory(ParserType.class).newInstance());
    }
}