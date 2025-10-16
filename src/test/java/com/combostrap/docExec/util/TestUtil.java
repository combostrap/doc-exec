package com.combostrap.docExec.util;

import com.combostrap.docExec.DocExecutorCli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class TestUtil {

    /**
     * A utility to run Tabul and capture the console
     */
    public static String runAndCaptureConsoleOutput(String... args) {

        // Save the original System.out
        PrintStream originalOut = System.out;

        // Create a ByteArrayOutputStream to capture the output
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PrintStream captureStream = new PrintStream(byteArrayOutputStream);
        ) {

            // Redirect System.out to our capture stream
            System.setOut(captureStream);

            // Execute
            DocExecutorCli.main(args);

            return byteArrayOutputStream.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            System.setOut(originalOut);
        }
    }
}
