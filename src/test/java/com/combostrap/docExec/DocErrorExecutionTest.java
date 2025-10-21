package com.combostrap.docExec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class DocErrorExecutionTest {

    /**
     * An Error must be thrown
     */
    @Test()
    public void docTestError() {

        RuntimeException error = Assertions.assertThrows(RuntimeException.class, () -> DocExecutor
                .create("test")
                .setEnableCache(false)
                .setShellCommandExecuteViaMainClass("cat", DocCommandCat.class)
                .build()
                .run(Paths.get("./src/test/resources/docTest/Error.txt")));

        /**
         * Error message can be huge, we don't add it into our exception
         */
        Assertions.assertEquals(DocFirstErrorOrWarning.STOP_AT_FIRST_ERROR, error.getMessage());
        /**
         * The cause is, it does not exist
         */
        String message = error.getCause().getMessage();
        System.out.println(message);
        Assertions.assertTrue(message.contains("java.nio.file.NoSuchFileException"));
        Assertions.assertTrue(message.contains("doesnotexist.txt"));

    }
}
