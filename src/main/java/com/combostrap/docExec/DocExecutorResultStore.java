package com.combostrap.docExec;

import com.combostrap.docExec.util.Fs;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocExecutorResultStore {
    private final Path resultsDirectory;

    public DocExecutorResultStore(DocExecutorInstance docExecutorInstance) {
        resultsDirectory = Fs.getTempDirectory()
                .resolve(DocExecutor.APP_NAME)
                .resolve(docExecutorInstance.getDocExecutor().getName())
                .resolve("results");
        if (!Files.exists(resultsDirectory)) {
            Fs.createDirectoryIfNotExists(resultsDirectory);
        }
    }

    public Path store(DocExecutorResultRun run) {
        ObjectMapper objectMapper = new ObjectMapper();
        String timeFormat = run.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path targetFile = resultsDirectory.resolve(timeFormat + ".jsonl");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile.toFile()))) {
            for (DocExecutorResultDocExecution result : run.getDocExecutionResults()) {
                String json = objectMapper.writeValueAsString(result);
                writer.write(json);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return targetFile;
    }


}
