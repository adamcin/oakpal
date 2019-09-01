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

package net.adamcin.oakpal.maven.mojo;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class MockMojoLog implements Log {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockMojoLog.class);

    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    public static final class MockMojoLogEntry {
        public final LogLevel level;
        public final String message;
        public final Throwable cause;

        private MockMojoLogEntry(LogLevel level, CharSequence message, Throwable cause) {
            this.level = level;
            this.message = Optional.ofNullable(message).map(CharSequence::toString).orElse(null);
            this.cause = cause;
        }

        public boolean isDebug() {
            return level == LogLevel.DEBUG;
        }

        public boolean isInfo() {
            return level == LogLevel.INFO;
        }

        public boolean isWarn() {
            return level == LogLevel.WARN;
        }

        public boolean isError() {
            return level == LogLevel.ERROR;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", level, message);
        }
    }

    private boolean debugEnabled = false;
    private boolean infoEnabled = true;
    private boolean warnEnabled = true;
    private boolean errorEnabled = true;

    final List<MockMojoLogEntry> entries = new ArrayList<>();

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void setInfoEnabled(boolean infoEnabled) {
        this.infoEnabled = infoEnabled;
    }

    public void setWarnEnabled(boolean warnEnabled) {
        this.warnEnabled = warnEnabled;
    }

    public void setErrorEnabled(boolean errorEnabled) {
        this.errorEnabled = errorEnabled;
    }

    public boolean any(final Predicate<MockMojoLogEntry> entryPredicate) {
        return this.entries.stream().anyMatch(entryPredicate);
    }

    public boolean none(final Predicate<MockMojoLogEntry> entryPredicate) {
        return this.entries.stream().noneMatch(entryPredicate);
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void debug(CharSequence charSequence) {
        this.debug(charSequence, null);
    }

    @Override
    public void debug(CharSequence charSequence, Throwable throwable) {
        this.entries.add(new MockMojoLogEntry(LogLevel.DEBUG, charSequence, throwable));
    }

    @Override
    public void debug(Throwable throwable) {
        this.debug(null, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return infoEnabled;
    }

    @Override
    public void info(CharSequence charSequence) {
        this.info(charSequence, null);
    }

    @Override
    public void info(CharSequence charSequence, Throwable throwable) {
        this.entries.add(new MockMojoLogEntry(LogLevel.INFO, charSequence, throwable));
    }

    @Override
    public void info(Throwable throwable) {
        this.info(null, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return warnEnabled;
    }

    @Override
    public void warn(CharSequence charSequence) {
        this.warn(charSequence, null);
    }

    @Override
    public void warn(CharSequence charSequence, Throwable throwable) {
        this.entries.add(new MockMojoLogEntry(LogLevel.WARN, charSequence, throwable));
    }

    @Override
    public void warn(Throwable throwable) {
        this.warn(null, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return errorEnabled;
    }

    @Override
    public void error(CharSequence charSequence) {
        this.error(charSequence, null);
    }

    @Override
    public void error(CharSequence charSequence, Throwable throwable) {
        this.entries.add(new MockMojoLogEntry(LogLevel.ERROR, charSequence, throwable));
    }

    @Override
    public void error(Throwable throwable) {
        this.error(null, throwable);
    }

    public void printAll() {
        entries.stream().forEachOrdered(entry -> {
            if (entry.cause != null) {
                LOGGER.info(entry.toString(), entry.cause);
            } else {
                LOGGER.info(entry.toString());
            }
        });
    }

    public Optional<MockMojoLogEntry> last() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(entries.size() - 1));
    }
}
