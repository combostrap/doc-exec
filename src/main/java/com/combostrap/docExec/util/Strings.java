package com.combostrap.docExec.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {

    private final String eol = System.lineSeparator();

    public static int getLineCount(String s) {
        try (InputStream fis = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))) {
            // +1 because if it's a line without end of line, you get still one line
            return parseEndOfLine(fis, Integer.class) + 1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param clazz - `String.class`, you get the first eol, `Integer.class`, you get the count
     */
    private static <T> T parseEndOfLine(InputStream inputStream, Class<T> clazz) throws IOException {
        char current;

        int eolCount = 0;
        StringBuilder localLineSeparator = new StringBuilder();
        while (inputStream.available() > 0) {
            current = (char) inputStream.read();
            if ((current == '\n') || (current == '\r')) {
                localLineSeparator.append(current);
                if (inputStream.available() > 0) {
                    char next = (char) inputStream.read();
                    if (next == current) {
                        eolCount++;
                        localLineSeparator = new StringBuilder(String.valueOf(next));
                    } else if (((next == '\r') || (next == '\n'))) {
                        localLineSeparator.append(next);
                        return clazz.cast(localLineSeparator.toString());
                    }
                }
                if (clazz.equals(String.class)) {
                    return clazz.cast(localLineSeparator.toString());
                } else {
                    eolCount++;
                    localLineSeparator = new StringBuilder();
                }
            }
        }
        if (clazz == String.class) {
            return clazz.cast(System.lineSeparator());
        } else {
            return clazz.cast(eolCount);
        }
    }

    public static String toPrintableCharacter(String s) {
        return s
                .replaceAll("\n", "\\\\n")
                .replaceAll("\r", "\\\\r");
    }

    public static Integer numberOfOccurrences(String s, String regexp) {

        Pattern pattern = Pattern.compile(regexp, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(s);
        Integer counter = 0;
        while (matcher.find()) {
            counter++;
        }
        return counter;

    }

    /**
     * Function used before a text comparison to normalize the text
     *
     * @return a compact string that is written on one line, has no double space and is trimmed
     */
    public static String normalize(String s) {
        return s.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll("[ ]{2,10}", " ")
                .trim(); // No double space;
    }

    public static String onOneLine(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll(" ", " ");
    }
}
