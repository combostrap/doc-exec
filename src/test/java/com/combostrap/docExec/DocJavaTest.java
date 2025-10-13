package com.combostrap.docExec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DocJavaTest {


    /**
     * A normal system exit (ie 0 status) should not:
     * * thrown an error
     * * and exit the process
     * This exist is used when the help is asked to terminate the process in a normal way
     */
    @Test
    public void noExitTest() {

        final String textToPrint = "Exiting";
        DocUnit docUnit = DocUnit.get()
                .setLanguage("java")
                .setCode("System.out.println(\"" + textToPrint + "\");\n" +
                        "System.exit(0);")
                .setConsoleContent(textToPrint);

        DocExecutor docExec = DocExecutor.create("test");
        // A runner
        boolean error = false;
        try {
            DocExecutorUnit.create(docExec)
                    .run(docUnit);
        } catch (Exception e) {
            error = true;
        }

        Assertions.assertFalse(error, "An error was not thrown and the run has not exited");

    }

    /**
     * A normal system exit (ie 0 status) should not:
     * * thrown an error
     * * and exit the process
     * This exist is used when the help is asked to terminate the process in a normal wa
     */
    @Test
    public void throwExceptionTest() {

        final String textToPrint = "Exiting";
        DocUnit docUnit = DocUnit.get()
                .setLanguage("java")
                .setCode("throw new RuntimeException(\"Bad\");")
                .setConsoleContent(textToPrint);

        DocExecutor docExec = DocExecutor.create("test");

        Assertions.assertThrows(
                RuntimeException.class,
                () -> DocExecutorUnit.create(docExec).run(docUnit)
        );


    }

}
