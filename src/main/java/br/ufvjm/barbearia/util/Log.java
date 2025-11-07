package br.ufvjm.barbearia.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Wrapper simples sobre {@link java.util.logging.Logger} para padronizar logs.
 */
public final class Log {

    private static final Logger LOGGER = Logger.getLogger("br.ufvjm.barbearia");
    private static final boolean DEBUG_ENABLED = Boolean.getBoolean("barbearia.debug");

    static {
        LOGGER.setUseParentHandlers(false);
        Level baseLevel = DEBUG_ENABLED ? Level.FINE : Level.INFO;
        LOGGER.setLevel(baseLevel);

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(baseLevel);
        consoleHandler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(consoleHandler);
    }

    private Log() {
    }

    public static void info(String message, Object... args) {
        LOGGER.log(Level.INFO, format(message, args));
    }

    public static void warning(String message, Object... args) {
        LOGGER.log(Level.WARNING, format(message, args));
    }

    public static void error(String message, Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable não pode ser nulo");
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    public static void debug(String message, Object... args) {
        if (DEBUG_ENABLED) {
            LOGGER.log(Level.FINE, format(message, args));
        }
    }

    private static String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    private static final class SimpleFormatter extends Formatter {

        private static final DateTimeFormatter DATE_TIME_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(record.getMillis()), ZoneId.systemDefault());
            return String.format("%s %-7s %s%n",
                    DATE_TIME_FORMATTER.format(dateTime),
                    record.getLevel().getName(),
                    record.getMessage());
        }
    }
}
