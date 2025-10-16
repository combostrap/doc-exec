package com.combostrap.docExec;

import com.combostrap.docExec.util.Fs;
import com.combostrap.docExec.util.Strings;
import com.combostrap.docExec.util.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.combostrap.docExec.DocExecutorCliResultCommand.RESULT_COMMAND_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocExecutorCliResultCommandTest {


    @Test
    void testResultCommandWithoutResults() {

        Path historyDir = DocExecutorCli.toDocExecutor(new DocExecutorCli()).build().getResults().getDirectory();
        Fs.delete(historyDir, true);
        String output = TestUtil.runAndCaptureConsoleOutput(RESULT_COMMAND_NAME);
        System.out.println(output);
        assertTrue(output.contains("No results found"));

    }


    @Test
    void testResultWithJsonlFiles() throws Exception {

        Path historyDir = DocExecutorCli.toDocExecutor(new DocExecutorCli()).build().getResults().getDirectory();

        // Create test .jsonl files with different timestamps
        Instant now = Instant.now();
        Path file1 = historyDir.resolve("2024-01-15_10-30-00.jsonl");
        Path file2 = historyDir.resolve("2024-01-15_14-45-30.jsonl");
        Path file3 = historyDir.resolve("2024-01-15_09-15-45.jsonl");
        Path nonJsonFile = historyDir.resolve("readme.txt");

        // Create files with content
        Files.writeString(file1, "{\"test\":\"data1\"}\n");
        Files.writeString(file2, "{\"test\":\"data2\"}\n{\"test\":\"data3\"}\n");
        Files.writeString(file3, "{\"test\":\"data4\"}\n");
        Files.writeString(nonJsonFile, "This is not a json file");

        // Set different modification times (file2 most recent, file3 oldest)
        Files.setLastModifiedTime(file3, FileTime.from(now.minus(2, ChronoUnit.HOURS)));
        Files.setLastModifiedTime(file1, FileTime.from(now.minus(1, ChronoUnit.HOURS)));
        Files.setLastModifiedTime(file2, FileTime.from(now));
        Files.setLastModifiedTime(nonJsonFile, FileTime.from(now.plus(1, ChronoUnit.HOURS)));


        String output = TestUtil.runAndCaptureConsoleOutput(RESULT_COMMAND_NAME);
        System.out.println(output);
        // 4 lines, 3 lines + 1 empty
        int numberOFFiles = 3;
        int emptyLineCount = 1;
        int headerLineCount = 1;
        Assertions.assertEquals(numberOFFiles + emptyLineCount + headerLineCount, Strings.getLineCount(output));

    }


}
