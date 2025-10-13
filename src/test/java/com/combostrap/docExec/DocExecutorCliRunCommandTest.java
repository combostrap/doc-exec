package com.combostrap.docExec;

import org.junit.jupiter.api.Test;

class DocExecutorCliRunCommandTest {

    @Test
    void runCommand() {
        DocExecutorCli.main(new String[]{"run","--help"});
    }

    @Test
    void runCommandNegate() {
        DocExecutorCli.main(new String[]{"run","--no-overwrite-docs","./src/test/resources/docTest/fileTest.txt"});
    }

}