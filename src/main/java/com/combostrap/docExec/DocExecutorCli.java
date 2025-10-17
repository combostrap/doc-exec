package com.combostrap.docExec;

import com.combostrap.docExec.picoli.LogLevelConverter;
import com.combostrap.docExec.util.JavaEnvs;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;

@Command(
        name = "doc-exec",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = {
                "Run code in documentation page",
        },
        subcommands = {
                DocExecutorCliRunCommand.class,
                DocExecutorCliEnvCommand.class,
                DocExecutorCliResultCommand.class
        }
)
public class DocExecutorCli implements Callable<Integer> {

    @CommandLine.Option(names = {"-n", "--doc-name"}, description = "The doc name (default to the name of the doc path directory). It determines the directory name for the cache and history.")
    private String name;

    @CommandLine.Option(
            names = {"--dry-run"},
            // default value is when no flag is present
            // if the flag is in a negated form (no), the value is negated if present
            // https://github.com/remkop/picocli/issues/813#issuecomment-532423733
            defaultValue = "false",
            description = "Do not overwrite the documentation files (no result in the console, no inline file included)"
    )
    private boolean dryRun = true;

    @CommandLine.Option(
            names = {"--purge-cache"},
            // default value when no flag is present
            // if the flag is in a negated form (no), the value is negated if present
            // https://github.com/remkop/picocli/issues/813#issuecomment-532423733
            defaultValue = "false",
            description = "Do not overwrite the documentation files (no result in the console, no inline file included)"
    )
    private boolean purgeCache = false;

    @CommandLine.Option(
            names = {"--no-capture-stderr"},
            // default value when no flag is present
            // if the flag is in a negated form (no), the value is negated if present
            // https://github.com/remkop/picocli/issues/813#issuecomment-532423733
            defaultValue = "true",
            description = "Do not capture the standard error stream of the code executed")
    private boolean captureStdErr = true;

    @CommandLine.Option(names = {"--no-cache"},
            // default value is when no flag is present
            // if the flag is in a negated form (no), the value is negated if present
            // https://github.com/remkop/picocli/issues/813#issuecomment-532423733
            defaultValue = "true",
            description = "Disable the cache"
    )
    private boolean enableCache = true;

    @CommandLine.Option(names = {"--no-stop"},
            defaultValue = "true",
            description = "Do not stop the execution at the first error or warning")
    private boolean stopRunAtFirstError = true;

    @CommandLine.Option(names = {"-fp", "--file-path"}, description = "One or more directories where to search for inline files (embedded in the doc)", defaultValue = "")
    private List<Path> searchInlineFilePaths = List.of(Paths.get(""));

    @CommandLine.Option(
            names = {"--doc-path", "-dp"},
            description = "A directory where to search for the documentation files if the glob path is relative",
            defaultValue = "")
    private Path searchDocPath = Paths.get("");

    @CommandLine.Option(
            names = {"--resume-from", "-rf"},
            description = "The path to resume execution from",
            defaultValue = ""
    )
    private String resumeFrom = "";

    @CommandLine.Option(names = {"--log-level"}, description = "Log level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)", defaultValue = "INFO")
    private Level logLevel = Level.INFO;

    @CommandLine.Option(names = {
            "--no-content-shrink-warning"},
            description = "Do not enable the content shrinking warning",
            defaultValue = "true"
    )
    private boolean contentShrinkingWarning = true;

    @CommandLine.Option(names = {"-D", "--system-property"}, description = "Set system property (key=value)")
    private Map<String, String> systemProperties = new HashMap<>();

    @CommandLine.Option(names = {"--shell-command-main-class"}, description = "Map shell command to main class (command=className)")
    private Map<String, String> shellCommandMainClasses = new HashMap<>();

    @CommandLine.Option(names = {"--shell-command-path"}, description = "Set qualified path for shell command (command=path)")
    private Map<String, String> shellCommandPaths = new HashMap<>();

    @CommandLine.Option(names = {"--shell-command-use-binary"}, description = "Use shell binary for command execution (command=true/false)")
    private Map<String, String> shellCommandUseBinary = new HashMap<>();

    /**
     * Utility function to build an instance from a cli
     */
    static DocExecutor toDocExecutor(DocExecutorCli docExecutorCli) {
        String name = docExecutorCli.getName();
        if (name == null) {
            /**
             * Name of the doc search path directory
             */
            name = docExecutorCli.getSearchInlineFilePaths().get(0).toAbsolutePath().getFileName().toString();
        }

        // Create DocExecutor with configured options
        DocExecutor executor = DocExecutor.create(name)
                .setDryRun(docExecutorCli.isDryRun())
                .setCaptureStdErr(docExecutorCli.isCaptureStdErr())
                .setEnableCache(docExecutorCli.isEnableCache())
                .setStopRunAtFirstErrorOrWarning(docExecutorCli.isStopRunAtFirstError())
                .setSearchFilePaths(docExecutorCli.getSearchInlineFilePaths())
                .setSearchDocPath(docExecutorCli.getSearchDocPath())
                .setLogLevel(docExecutorCli.getLogLevel())
                .setContentShrinkWarning(docExecutorCli.isContentShrinkingWarning())
                .setResumeFrom(docExecutorCli.getResumeFrom())
                .setPurgeCache(docExecutorCli.isPurgeCache());

        // Set system properties
        for (Map.Entry<String, String> entry : docExecutorCli.getSystemProperties().entrySet()) {
            executor.setSystemProperty(entry.getKey(), entry.getValue());
        }

        // Configure shell command main classes
        for (Map.Entry<String, String> entry : docExecutorCli.getShellCommandMainClasses().entrySet()) {
            try {
                Class<?> clazz = Class.forName(entry.getValue());
                executor.setShellCommandExecuteViaMainClass(entry.getKey(), clazz);
            } catch (ClassNotFoundException e) {
                System.err.println("Warning: Could not find class " + entry.getValue() + " for command " + entry.getKey());
            }
        }

        // Configure shell command paths
        for (Map.Entry<String, String> entry : docExecutorCli.getShellCommandPaths().entrySet()) {
            Path commandPath = Paths.get(entry.getValue());
            executor.setShellCommandQualifiedPath(entry.getKey(), commandPath);
        }

        // Configure shell command binary usage
        for (Map.Entry<String, String> entry : docExecutorCli.getShellCommandUseBinary().entrySet()) {
            boolean useBinary = Boolean.parseBoolean(entry.getValue());
            executor.setShellCommandExecuteViaShellBinary(entry.getKey(), useBinary);
        }
        return executor;
    }

    private String getResumeFrom() {
        return resumeFrom;
    }


    // Getter methods for subcommands to access options
    public String getName() {
        return name;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isPurgeCache() {
        return purgeCache;
    }

    public boolean isCaptureStdErr() {
        return captureStdErr;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    public boolean isStopRunAtFirstError() {
        return stopRunAtFirstError;
    }

    public List<Path> getSearchInlineFilePaths() {
        return searchInlineFilePaths;
    }

    public Path getSearchDocPath() {
        return searchDocPath;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public boolean isContentShrinkingWarning() {
        return contentShrinkingWarning;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public Map<String, String> getShellCommandMainClasses() {
        return shellCommandMainClasses;
    }

    public Map<String, String> getShellCommandPaths() {
        return shellCommandPaths;
    }

    public Map<String, String> getShellCommandUseBinary() {
        return shellCommandUseBinary;
    }


    @Override
    public Integer call() {
        // Show help when no subcommand is provided
        CommandLine.usage(this, System.out);
        return 1;
    }

    public static void main(String[] args) {
        CommandLine commandLine = getCommandLine();

        int exitCode = commandLine.execute(args);
        if (JavaEnvs.isJUnitTest()) {
            if (exitCode != 0) {
                throw new RuntimeException("Exit code (" + exitCode + ") is not zero. Errors has been seen.");
            }
            return;
        }
        System.exit(exitCode);
    }

    /**
     * A command line with converter
     * For now, there is no to configure it,
     * but it could be used in test if it was possible
     */
    protected static CommandLine getCommandLine() {

        CommandLine commandLine = new CommandLine(new DocExecutorCli())
                .registerConverter(Level.class, new LogLevelConverter());

        /**
         * Picocli catch the exception by default
         * We overwrite it here
         */
        commandLine.setExecutionExceptionHandler((ex, commandLine1, parseResult) -> {

            /**
             * Print the stack trace
             * If we throw, we get the stack trace but before JUL messages output
             * because it seems they are in another thread
             */
            Throwable throwable = ex;
            if (ex instanceof DocFirstErrorOrWarning) {
                throwable = ex.getCause();
            }
            DocLog.LOGGER.log(Level.SEVERE, "Command execution failed", throwable);
            return 1;
        });
        return commandLine;
    }

}
