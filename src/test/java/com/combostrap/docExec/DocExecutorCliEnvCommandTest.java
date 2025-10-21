package com.combostrap.docExec;

import org.junit.jupiter.api.Test;

class DocExecutorCliEnvCommandTest {

    @Test
    void envCommand() {
        DocExecutorCli.main(new String[]{"env"});
    }

}