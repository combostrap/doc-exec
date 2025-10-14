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
                "Run code in documentation page with unit elements",
        },
        subcommands = {DocExecutorCliRunCommand.class}
)
public class DocExecutorCli implements Callable<Integer> {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Execution name")
    private String name;

    @CommandLine.Option(
            names = {"--no-overwrite-docs"},
            // default value when no flag is present
            // because the flag is a negated form, the value is negated if present
            // https://github.com/remkop/picocli/issues/813#issuecomment-532423733
            defaultValue = "true",
            description = "Do not overwrite the documentation files (no result in the console, no inline file included)"
    )
    private boolean overwrite = true;

    @CommandLine.Option(
            names = {"--purge-cache"},
            // default value when no flag is present
            // because the flag is a negated form, the value is negated if present
            // https://github.com/remkop/picocli/issues/813#issuecomment-532423733
            defaultValue = "false",
            description = "Do not overwrite the documentation files (no result in the console, no inline file included)"
    )
    private boolean purgeCache = false;

    @CommandLine.Option(
            names = {"--no-capture-stderr"},
            defaultValue = "true",
            description = "Do not capture the standard error stream of the code executed")
    private boolean captureStdErr = true;

    @CommandLine.Option(names = {"--no-cache"},
            defaultValue = "true",
            description = "Disable the cache"
    )
    private boolean enableCache = true;

    @CommandLine.Option(names = {"--no-stop-at-first-error"},
            defaultValue = "true",
            description = "Do not stop the execution at the first error")
    private boolean stopRunAtFirstError = true;

    @CommandLine.Option(names = {"-pf", "--paths-inline-file"}, description = "Set a list of directories where to search for inline files (Files inlined in the doc)", defaultValue = ".")
    private List<Path> searchInlineFilePaths = List.of(Paths.get("."));

    @CommandLine.Option(names = {"--path-doc", "-pd"}, description = "A directory where to search for the documentation files if the glob path is relative", defaultValue = ".")
    private Path searchDocPath = Paths.get(".");

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
            name = docExecutorCli.getSearchInlineFilePaths().get(0).toAbsolutePath().getParent().getFileName().toString();
        }

        // Create DocExecutor with configured options
        DocExecutor executor = DocExecutor.create(name)
                .setOverwrite(docExecutorCli.isOverwrite())
                .setCaptureStdErr(docExecutorCli.isCaptureStdErr())
                .setEnableCache(docExecutorCli.isEnableCache())
                .setStopRunAtFirstError(docExecutorCli.isStopRunAtFirstError())
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

    public boolean isOverwrite() {
        return overwrite;
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
        CommandLine commandLine = new CommandLine(new DocExecutorCli())
                .registerConverter(Level.class, new LogLevelConverter());
        int exitCode;
        try {
            exitCode = commandLine.execute(args);
        } catch (Exception e) {
            exitCode = 1;
            System.err.println("Error: " + e.getMessage());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        if (!JavaEnvs.isJUnitTest()) {
            System.exit(exitCode);
        }
    }

}
