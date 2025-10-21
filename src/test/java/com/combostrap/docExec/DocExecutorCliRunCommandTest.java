package com.combostrap.docExec;

import org.junit.jupiter.api.Test;

class DocExecutorCliRunCommandTest {

    @Test
    void runCommand() {
        DocExecutorCli.main(new String[]{"run","--help"});
    }

    @Test
    void runCommandNegate() {
        DocExecutorCli.main(new String[]{
                "--dry-run",
                "--no-cache",
                "--file-path",
                "src/test/resources",
                "run",
                "./src/test/resources/docTest/fileTest.txt"}
        );
    }

}