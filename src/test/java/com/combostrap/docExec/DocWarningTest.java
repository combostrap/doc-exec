package com.combostrap.docExec;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Test the warning
 */
public class DocWarningTest {

    /**
     * A code that output less content should throw a warning/error
     */
    @Test
    public void warningContentShrink() {
        final Path path = Paths.get("./src/test/resources/docTest/warning-content-shrink.txt");

        DocFirstErrorOrWarning firstError = Assertions.assertThrows(DocFirstErrorOrWarning.class, () -> DocExecutor.create("test")
                .setContentShrinkWarning(true)
                // don't overwrite otherwise if we commit the file, the next time we get a success
                .setDryRun(true)
                .setEnableCache(false)
                .build()
                .run(path)
        );
        Assertions.assertEquals(DocWarning.class, firstError.getCause().getClass());

    }


}
