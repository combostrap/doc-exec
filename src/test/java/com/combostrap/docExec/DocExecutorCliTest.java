package com.combostrap.docExec;

import org.junit.jupiter.api.Test;

class DocExecutorCliTest {

    @Test
    void baseLine() {
        DocExecutorCli.main(new String[]{"--help"});
    }


}