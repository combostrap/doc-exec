package com.combostrap.docExec;

import com.combostrap.docExec.util.Fs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class DocExecutorResultStoreTest {


    @Test
    void baselineStore() {
        DocExecutorInstance build = DocExecutor.create("test")
                .setEnableCache(false)
                .setSearchDocPath(Paths.get("src/test/resources"))
                .build();
        DocExecutorResultRun docExecutor = build
                .run("docFile/file.txt");
        DocExecutorResultStore executor = build.getResultStore();
        Path path = executor.store(docExecutor);
        Assertions.assertTrue(Files.exists(path));
        System.out.println(Fs.toString(path));
    }
}