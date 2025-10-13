package com.combostrap.docExec;

import com.combostrap.docExec.util.Fs;
import com.combostrap.docExec.util.Glob;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

    @CommandLine.Parameters(
            description = "One or more docs to execute as glob expression or file path",
            // at least 1
            arity = "1..*"
    )
    private String[] docs = new String[0];

    @Override
    public Integer call() throws Exception {
        if (name == null) {
            name = searchInlineFilePaths.get(0).toAbsolutePath().getParent().getFileName().toString();
        }
        try {
            // Create DocExecutor with configured options
            DocExecutor executor = DocExecutor.create(name)
                    .setOverwrite(overwrite)
                    .setCaptureStdErr(captureStdErr)
                    .setEnableCache(enableCache)
                    .setStopRunAtFirstError(stopRunAtFirstError)
                    .setSearchFilePaths(searchInlineFilePaths)
                    .setSearchDocPath(searchDocPath)
                    .setLogLevel(logLevel)
                    .setContentShrinkWarning(contentShrinkingWarning)
                    .setPurgeCache(purgeCache);

            // Set system properties
            for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
                executor.setSystemProperty(entry.getKey(), entry.getValue());
            }

            // Configure shell command main classes
            for (Map.Entry<String, String> entry : shellCommandMainClasses.entrySet()) {
                try {
                    Class<?> clazz = Class.forName(entry.getValue());
                    executor.setShellCommandExecuteViaMainClass(entry.getKey(), clazz);
                } catch (ClassNotFoundException e) {
                    System.err.println("Warning: Could not find class " + entry.getValue() + " for command " + entry.getKey());
                }
            }

            // Configure shell command paths
            for (Map.Entry<String, String> entry : shellCommandPaths.entrySet()) {
                Path commandPath = Paths.get(entry.getValue());
                executor.setShellCommandQualifiedPath(entry.getKey(), commandPath);
            }

            // Configure shell command binary usage
            for (Map.Entry<String, String> entry : shellCommandUseBinary.entrySet()) {
                boolean useBinary = Boolean.parseBoolean(entry.getValue());
                executor.setShellCommandExecuteViaShellBinary(entry.getKey(), useBinary);
            }

            // Build the executor instance and run
            executor.build().run(docs);

            // We throw if any error
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
