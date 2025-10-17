package com.combostrap.docExec;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(
        name = "result",
        description = "List execution results files"
)
public class DocExecutorCliResultCommand implements Callable<Integer> {

    public static final String RESULT_COMMAND_NAME = "result";

    @ParentCommand
    private DocExecutorCli parent;

    @Override
    public Integer call() throws Exception {

        DocExecutorInstance docExecutorInstance = DocExecutorCli.toDocExecutor(parent).build();
        DocExecutorResultStore resultStore = docExecutorInstance.getResultStore();

        Path historyDirectory = resultStore.getDirectory();

        if (!Files.exists(historyDirectory)) {
            System.out.println("No result directory found at: " + historyDirectory);
            return 0;
        }

        // List all .jsonl files in the directory, sorted by creation time (descending)
        try (Stream<Path> files = Files.list(historyDirectory)) {
            List<Path> filesPath = files.filter(path -> path.toString().endsWith(".jsonl"))
                    .sorted(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime(path);
                        } catch (IOException e) {
                            return null;
                        }
                    }, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            if (filesPath.isEmpty()) {
                System.out.println("No results found at: " + historyDirectory);
                return 0;
            }
            System.out.println("Results found at: " + historyDirectory);
            for (Path path : filesPath) {
                String fileName = path.getFileName().toString();
                long fileSize = Files.size(path);
                System.out.printf("* %-30s (%d bytes)%n", fileName, fileSize);
            }
        }

        return 0;

    }
}
