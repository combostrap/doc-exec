package com.combostrap.docExec;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "run",
        description = {
                "Execute documentation code blocks",
                "",
                "Examples:",
                "",
                "   To run a page with caching disabled",
                "      doc-exec run --no-cache page/namespace/name.txt",
                "",
                "   To run all page with caching disabled",
                "",
                "      doc-exec run --no-cache *",
                ""
        },
        mixinStandardHelpOptions = true
)
public class DocExecutorCliRunCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private DocExecutorCli parent;

    @CommandLine.Parameters(
            description = "One or more docs to execute as glob expression or file path",
            // at least 1
            arity = "1..*"
    )
    private String[] docs = new String[0];

    @Override
    public Integer call() {

        DocExecutorInstance instance = DocExecutorCli
                .toDocExecutor(parent)
                .build();

        // Build the executor instance and run
        DocExecutorResultRun results = instance.run(docs);
        instance.getResults()
                .store(results);

        // We throw if any error, so if we come here, it was successful
        return 0;
    }

}
