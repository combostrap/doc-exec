package com.combostrap.docExec.picoli;


import picocli.CommandLine;

import java.util.logging.Level;

/**
 * TypeConverter for java.util.logging.Level
 * When an option/argument variable has the type Level, picocli uses this method to convert it from string
 */
public class LogLevelConverter implements CommandLine.ITypeConverter<Level> {

    @Override
    public Level convert(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Level value cannot be null or empty");
        }

        // Try to parse as a standard level name (SEVERE, WARNING, INFO, etc.)
        try {
            return Level.parse(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid log level: " + value +
                            ". Valid levels are: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL, OFF"
            );
        }
    }
}
