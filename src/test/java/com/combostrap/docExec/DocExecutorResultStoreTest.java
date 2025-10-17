package com.combostrap.docExec;

import com.combostrap.docExec.util.Fs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class DocExecutorResultStoreTest {


    @Test
    void baselineStore() {
        DocExecutorInstance build = DocExecutor.create("test")
                .setEnableCache(false)
                .build();
        DocExecutorResultRun docExecutor = build
                .run("src/test/resources/docFile/file.txt");
        DocExecutorResultStore executor = build.getResultStore();
        Path path = executor.store(docExecutor);
        Assertions.assertTrue(Files.exists(path));
        System.out.println(Fs.toString(path));
    }
}