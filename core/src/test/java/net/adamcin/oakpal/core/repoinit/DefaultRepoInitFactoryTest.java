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
import net.adamcin.oakpal.core.repoinit.DefaultRepoInitFactory;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.jcr.repoinit.impl.JcrRepoInitOpsProcessorImpl;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.junit.Test;

import javax.jcr.Session;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DefaultRepoInitFactoryTest {

    Reader readSlingExampleRepoInit() {
        return new InputStreamReader(getClass().getResourceAsStream("sling_example.txt"), StandardCharsets.UTF_8);
    }

    @Test
    public void testBindParserProcessor_realDefaults() throws Exception {
        final OakMachine.RepoInitProcessor processor =
                DefaultRepoInitFactory.bindParserProcessor(
                        new RepoInitParserService(),
                        new JcrRepoInitOpsProcessorImpl());

        new OakMachine.Builder().build().adminInitAndInspect(admin -> {
            try (Reader reader = readSlingExampleRepoInit()) {
                processor.apply(admin, reader);
            }
        });
    }

    @Test(expected = RepoInitParsingException.class)
    public void testBindParserProcessor_throws() throws Exception {
        final OakMachine.RepoInitProcessor processor =
                DefaultRepoInitFactory.bindParserProcessor(
                        new ParserType(reader -> {
                            throw new RepoInitParsingException("expected error", null);
                        }),
                        new ProcessorType((session, operations) -> { /* */ }));

        processor.apply(null, new StringReader(""));
    }

    @Test
    public void testBindParserProcessor() throws Exception {
        final OakMachine.RepoInitProcessor processor =
                DefaultRepoInitFactory.bindParserProcessor(
                        new ParserType(),
                        new ProcessorType((session, operations) -> {
                        }));

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

    @FunctionalInterface
    interface ProcessorFunction {
        void apply(final Session session, final List<Operation> operations);
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


    static class ProcessorType implements JcrRepoInitOpsProcessor {
        private ProcessorFunction processorFunction;

        public ProcessorType() {
            processorFunction = (session, operations) -> { /* do nothing */ };
        }

        public ProcessorType(final ProcessorFunction processorFunction) {
            this.processorFunction = processorFunction;
        }

        @Override
        public void apply(final Session session, final List<Operation> operations) {
            processorFunction.apply(session, operations);
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

    static class ProcessorTypeWrongConstructor implements JcrRepoInitOpsProcessor {
        public ProcessorTypeWrongConstructor(final boolean anArgument) {
            /* default constructor */
        }

        @Override
        public void apply(final Session session, final List<Operation> list) {

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
                new DefaultRepoInitFactory(null, null).newParser());
        assertNull("null for wrong constructor",
                new DefaultRepoInitFactory(ParserTypeWrongConstructor.class, null)
                        .newParser());
        assertTrue("expect parser",
                new DefaultRepoInitFactory(ParserType.class, null)
                        .newParser() instanceof RepoInitParser);
    }

    @Test
    public void testNewOpsProcessor() {
        assertNull("null for null",
                new DefaultRepoInitFactory(null, null).newOpsProcessor());
        assertNull("null for wrong constructor",
                new DefaultRepoInitFactory(null, ProcessorTypeWrongConstructor.class)
                        .newOpsProcessor());
        assertTrue("expect ops processor",
                new DefaultRepoInitFactory(null, ProcessorType.class)
                        .newOpsProcessor() instanceof JcrRepoInitOpsProcessor);
    }

    @Test
    public void testNewInstance() {
        assertSame("NOOP for nulls", DefaultRepoInitFactory.NOOP_PROCESSOR,
                new DefaultRepoInitFactory(null, null).newInstance());
        assertSame("NOOP for null parser", DefaultRepoInitFactory.NOOP_PROCESSOR,
                new DefaultRepoInitFactory(null, ProcessorType.class).newInstance());
        assertSame("NOOP for null processor", DefaultRepoInitFactory.NOOP_PROCESSOR,
                new DefaultRepoInitFactory(ParserType.class, null).newInstance());
        assertNotSame("expect non-NOOP nonnulls", DefaultRepoInitFactory.NOOP_PROCESSOR,
                new DefaultRepoInitFactory(ParserType.class, ProcessorType.class).newInstance());
    }
}