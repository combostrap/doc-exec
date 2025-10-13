package com.combostrap.docExec;

import com.combostrap.docExec.picoli.LogLevelConverter;
import com.combostrap.docExec.util.JavaEnvs;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Level;

@Command(
        name = "doc-exec",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = {
                "Run code in documentation page with unit elements",
        },
        subcommands = {DocExecutorCliRunCommand.class}
)
public class DocExecutorCli implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        // Show help when no subcommand is provided
        CommandLine.usage(this, System.out);
        return 1;
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new DocExecutorCli())
                .registerConverter(Level.class, new LogLevelConverter());
        int exitCode = commandLine.execute(args);
        if (!JavaEnvs.isJUnitTest()) {
            System.exit(exitCode);
        }
    }

}
