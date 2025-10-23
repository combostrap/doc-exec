package com.combostrap.docExec;

import com.combostrap.docExec.util.TestUtil;
import org.junit.jupiter.api.Test;

class DocExecutorCliTest {

    @Test
    void helpTest() {
        DocExecutorCli.main(new String[]{"--help"});
    }

    @Test
    void versionTest() {

        String output = TestUtil.runAndCaptureConsoleOutput("--version");
        System.out.println(output);

    }


}