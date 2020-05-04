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
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Default provider for RepoInitProcessors.
 */
public final class DefaultRepoInitFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRepoInitFactory.class);
    private static final String REPO_INIT_PARSER_IMPL_CLASS =
            "org.apache.sling.repoinit.parser.impl.RepoInitParserService";
    private static final String REPO_INIT_OPS_PROCESSOR_IMPL_CLASS =
            "org.apache.sling.jcr.repoinit.impl.JcrRepoInitOpsProcessorImpl";

    Class<? extends RepoInitParser> parserClazz;
    Class<? extends JcrRepoInitOpsProcessor> opsProcessorClazz;

    static final OakMachine.RepoInitProcessor NOOP_PROCESSOR = (admin, reader) -> {
        throw new RepoInitParsingException("repoinit processor is not available", null);
    };

    DefaultRepoInitFactory(final @Nullable Class<? extends RepoInitParser> parserClazz,
                           final @Nullable Class<? extends JcrRepoInitOpsProcessor> opsProcessorClazz) {
        this.parserClazz = parserClazz;
        this.opsProcessorClazz = opsProcessorClazz;
    }

    public static DefaultRepoInitFactory newFactoryInstance(final @NotNull ClassLoader parserCl,
                                                            final @NotNull ClassLoader processorCl) {
        return new DefaultRepoInitFactory(loadClazz(parserCl, RepoInitParser.class, REPO_INIT_PARSER_IMPL_CLASS),
                loadClazz(processorCl, JcrRepoInitOpsProcessor.class, REPO_INIT_OPS_PROCESSOR_IMPL_CLASS));
    }

    static <T> @Nullable Class<? extends T> loadClazz(final @NotNull ClassLoader classLoader,
                                                      final @NotNull Class<T> iface,
                                                      final @NotNull String clazzName) {
        try {
            Class<?> clazz = classLoader.loadClass(clazzName);
            if (iface.isAssignableFrom(clazz)) {
                return clazz.asSubclass(iface);
            } else {
                Throwable e = new IllegalStateException();
                LOGGER.warn(MessageFormat.format(
                        "class from classLoader argument does not implement {0} from oakpal classLoader: {1}",
                        iface.getSimpleName(), clazzName), e);
            }
        } catch (Exception e) {
            LOGGER.warn(MessageFormat.format(
                    "Failed to load the class from the provided classLoader: {0}", clazzName), e);
        }
        return null;
    }

    public static OakMachine.RepoInitProcessor newDefaultRepoInitProcessor(
            final @NotNull ClassLoader defaultClassLoader) {
        final DefaultRepoInitFactory factory =
                DefaultRepoInitFactory.newFactoryInstance(defaultClassLoader, defaultClassLoader);
        return factory.newInstance();
    }

    OakMachine.RepoInitProcessor newInstance() {
        final RepoInitParser repoInitParser = newParser();
        final JcrRepoInitOpsProcessor opsProcessor = newOpsProcessor();
        return ofNullable(repoInitParser)
                .flatMap(parser ->
                        ofNullable(opsProcessor).map(processor ->
                                bindParserProcessor(parser, processor)))
                .orElse(NOOP_PROCESSOR);
    }

    public static OakMachine.RepoInitProcessor bindParserProcessor(final @NotNull RepoInitParser parser,
                                                                   final @NotNull JcrRepoInitOpsProcessor processor) {
        return (admin, reader) -> {
            final List<Operation> parsed = parser.parse(reader);
            processor.apply(admin, parsed);
        };
    }

    RepoInitParser newParser() {
        if (parserClazz != null) {
            try {
                return parserClazz.getConstructor().newInstance();
            } catch (Exception e) {
                LOGGER.warn("failed to construct instance of " + parserClazz.getName(), e);
            }
        }
        return null;
    }

    JcrRepoInitOpsProcessor newOpsProcessor() {
        if (opsProcessorClazz != null) {
            try {
                return opsProcessorClazz.getConstructor().newInstance();
            } catch (Exception e) {
                LOGGER.warn("failed to construct instance of " + opsProcessorClazz.getName(), e);
            }
        }
        return null;
    }
}

