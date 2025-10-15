package com.combostrap.docExec;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.*;


@SuppressWarnings("unused")
public class DocExecutor {


    public static final String APP_NAME = "doc-exec";
    private final String name;


    private final DocSecurityManager securityManager;
    private boolean captureStdErr = true;

    private final Map<String, Class<?>> shellCommandMainClassMap = new HashMap<>();
    // The fully qualified path of the command
    // to be sure that we don't hit another command
    private final Map<String, Path> shellCommandAbsolutePathMap = new HashMap<>();
    private final Map<String, Boolean> shellCommandUseShellBinaryMap = new HashMap<>();
    private Level logLevel = Level.INFO;
    private boolean contentShrinkingWarning = true;
    private boolean enableCache = true;
    private boolean purgeCache = false;
    private Path searchDocPath = Paths.get("");
    private String resumeFrom = null;


    /**
     * @param dryRun If set to true, the console and the file node will not be overwritten
     * @return the object for chaining
     */
    public DocExecutor setDryRun(boolean dryRun) {
        this.isDryRun = dryRun;
        return this;
    }

    /**
     * @param captureStdErr If set to true, the std err is added to the output
     * @return the object for chaining
     */
    public DocExecutor setCaptureStdErr(boolean captureStdErr) {
        this.captureStdErr = captureStdErr;
        return this;
    }

    private boolean isDryRun = false;


    /**
     * @param name The execution name
     */
    private DocExecutor(String name) {
        this.name = name;
        // Managing System.exit in code execution with the security manager
        securityManager = DocSecurityManager.create();
        // Will be Removed in JDK 24 https://openjdk.org/jeps/486
        // In most cases, we found that issues that seemed to need interception could be adequately addressed outside the JDK,
        // using techniques such as:
        // * source code modification,
        // * static code analysis and rewriting,
        // * or agent-based dynamic code rewriting at class load time.
        // Example: Agent that blocks code from calling System::exit
        // The transformer rewrites every call to System.exit(int) into throw new RuntimeException("System.exit not allowed").
        // https://openjdk.org/jeps/486#Appendix
        // https://github.com/stefanbirkner/system-lambda/issues/27
        System.setSecurityManager(securityManager);
    }


    /**
     * @param name - The name of the run (used in the console)
     * @return the object for chaining
     */
    public static DocExecutor create(String name) {

        return new DocExecutor(name);
    }

    /**
     * @param enableCache - A doc cache for this run
     * @return a {@link DocExecutor} for chaining
     */
    public DocExecutor setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
        return this;
    }

    /**
     * Build the DocExecutorRun instance
     *
     * @return a DocExecutorRun instance configured with this builder
     */
    public DocExecutorInstance build() {
        return new DocExecutorInstance(this);
    }

    private List<Path> searchFilePaths = List.of(Paths.get("."));

    /**
     * Do we stop at the first error
     */
    private boolean stopRunAtFirstError = true;

    public DocExecutor setStopRunAtFirstError(boolean stopRunAtFirstError) {
        this.stopRunAtFirstError = stopRunAtFirstError;
        return this;
    }


    /**
     * Execute a shell command via a Java Main Class
     * <p></p>
     * If the {@link DocUnit#getLanguage() language} is a shell language (dos or bash),
     * * the first name that we called cli is replaced by the mainClass
     * * the others args forms the args that are passed to the main method of the mainClass
     *
     * @param command   - the name of the command (ie the first word in a command statement)
     * @param mainClazz - a main class that will receive the parsed arguments
     * @throws IllegalArgumentException - if the command was already set to use {@link #setShellCommandExecuteViaShellBinary(String, Boolean)}
     */
    public DocExecutor setShellCommandExecuteViaMainClass(String command, Class<?> mainClazz) {
        shellCommandMainClassMap.put(command, mainClazz);
        if (shellCommandUseShellBinaryMap.get(command) == null) {
            shellCommandUseShellBinaryMap.put(command, false);
        } else {
            throw new IllegalArgumentException("The command " + command + " was already set to use the shell binary (bash -c) for execution. You can't have both the main class (" + mainClazz + ") and the shell binary execution");
        }
        return this;
    }

    /**
     * If the {@link DocUnit#getLanguage() language} is a shell language (dos or bash), set the full qualified path of the command.
     * When your command is not in the path and that you can't change it easily,
     * if a path is set, we will replace the command by its full qualified path
     *
     * @param command      - the name of the command (ie the first word in a command statement)
     * @param absolutePath - the qualified path of the command (not the directory, the binary file)
     * @throws IllegalArgumentException - if the path is not absolute
     */
    public DocExecutor setShellCommandQualifiedPath(String command, Path absolutePath) {
        if (!absolutePath.isAbsolute()) {
            throw new IllegalArgumentException("The path (" + absolutePath + ") is not absolute");
        }
        shellCommandAbsolutePathMap.put(command, absolutePath);
        return this;
    }

    /**
     * @param paths the base path (Where do we will find the files defined in the file node)
     * @return the runner for chaining instantiation
     */
    public DocExecutor setSearchFilePaths(List<Path> paths) {
        this.searchFilePaths = paths;
        return this;
    }

    public DocExecutor setSearchFilePaths(Path... paths) {
        return setSearchFilePaths(Arrays.asList(paths));
    }


    public DocExecutor setLogLevel(Level level) {
        this.logLevel = level;
        return this;
    }


    /**
     * Add a java system property
     *
     * @param key   the key
     * @param value the value
     * @return the object for chaining
     */
    public DocExecutor setSystemProperty(String key, String value) {
        System.setProperty(key, value);
        return this;
    }

    public boolean doesStopAtFirstError() {
        return this.stopRunAtFirstError;
    }

    /**
     * If the {@link DocUnit#getLanguage() language} is a shell language (dos, bash, ...)
     * Use the shell cli or parse the args and execute via java exec
     * (ie use "bash -c" or execute the arguments)
     * It can be handy in environment where no bash is provided
     * <p></p>
     * Note that this parameter has no effect if a {@link #setShellCommandExecuteViaMainClass(String, Class)}
     * command class was specified
     *
     * @param commandName - the command name (the first word in the command statement)
     * @param useShellCli - use "bash -c" to execute the command (True by default) or parse the args and execute via java exec
     * @return the object for chaining
     */
    public DocExecutor setShellCommandExecuteViaShellBinary(String commandName, Boolean useShellCli) {
        this.shellCommandUseShellBinaryMap.put(commandName, useShellCli);
        Class<?> mainClazz = shellCommandMainClassMap.get(commandName);
        if (mainClazz != null) {
            throw new IllegalArgumentException("The command " + commandName + " was already set to use the main class (" + mainClazz + ") for execution. You can't have both the main class (" + mainClazz + ") and the shell binary execution");
        }
        return this;
    }

    /**
     * @param commandName - the command name (the first word in the command statement)
     * @return the Path of the command on the system
     */
    protected Path getShellCommandPath(String commandName) {
        return this.shellCommandAbsolutePathMap.get(commandName);
    }

    /**
     * @param commandName - the cli/exec
     * @return the main class that implements a cli/exec
     * <p>
     * This is used to generate Java code when the documentation is a shell documentation
     */
    public Class<?> getShellCommandMainClass(String commandName) {
        return this.shellCommandMainClassMap.get(commandName);
    }

    protected boolean isExecuteShellCommandViaShellBinary(String commandName) {
        Boolean executeViaShellBinary = this.shellCommandUseShellBinaryMap.get(commandName);
        if (executeViaShellBinary == null) {
            return true;
        }
        return executeViaShellBinary;
    }

    protected DocSecurityManager getSecurityManager() {
        return this.securityManager;
    }


    public DocExecutor setContentShrinkWarning(boolean b) {
        this.contentShrinkingWarning = b;
        return this;
    }

    // Getter methods for DocExecutorRun
    public String getName() {
        return name;
    }

    public boolean getIsDryRun() {
        return isDryRun;
    }

    public List<Path> getSearchFilePaths() {
        return searchFilePaths;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public boolean isContentShrinkingWarning() {
        return contentShrinkingWarning;
    }

    public DocExecutor setPurgeCache(boolean purgeCache) {
        this.purgeCache = purgeCache;
        return this;
    }

    public boolean getPurgeCache() {
        return purgeCache;
    }

    public DocExecutor setSearchDocPath(Path searchDocPath) {
        if (!searchDocPath.isAbsolute()) {
            searchDocPath = searchDocPath.toAbsolutePath();
        }
        this.searchDocPath = searchDocPath.normalize();
        return this;
    }

    public Path getSearchDocPath() {
        return this.searchDocPath;
    }

    public boolean getCaptureStdErr() {
        return captureStdErr;
    }

    public List<String> getDocExtensions() {
        return List.of("txt", "md");
    }

    public Path getResumeFromPath() {
        if (this.resumeFrom == null || this.resumeFrom.isBlank()) {
            return null;
        }
        if (this.searchDocPath == null) {
            return Paths.get(this.resumeFrom);
        }
        return this.searchDocPath.resolve(this.resumeFrom);
    }

    public DocExecutor setResumeFrom(String resumeFrom) {
        this.resumeFrom = resumeFrom;
        return this;
    }


    public boolean getIsCacheEnabled() {
        return enableCache;
    }
}
