package com.combostrap.docExec;

import org.zeroturnaround.exec.ProcessExecutor;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Execute a unit (code block, file block) found in a doc
 * <p>
 * A {@link DocExecutorUnit} contains the environment variable and function to run a {@link DocUnit}
 */
public class DocExecutorUnit {


    private final DocExecutor docExecutor;
    private final DocLog log;

    /**
     * The directory where the compile class are saved
     */
    private final Path outputDirClass;


    /**
     * Get a {@link DocExecutorUnit} with the {@link #create(DocExecutorInstance)} function please
     *
     * @param docExecutorInstance - the context object
     */
    private DocExecutorUnit(DocExecutorInstance docExecutorInstance) {

        outputDirClass = Paths.get(System.getProperty("java.io.tmpdir"), "docTestClass").normalize().toAbsolutePath();
        this.docExecutor = docExecutorInstance.getConf();
        this.log = docExecutorInstance.getLog();
        try {
            Files.createDirectories(outputDirClass);// Safe if the dir already exist
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param docExecutorInstance - the context object
     * @return - a docTestRunner that contains the environment variable and function to run a test
     */
    protected static DocExecutorUnit create(DocExecutorInstance docExecutorInstance) {
        return new DocExecutorUnit(docExecutorInstance);
    }


    /**
     * Run and evaluate the code in a {@link DocUnit}
     * This function :
     * * wraps the code in a static method,
     * * run it
     * * capture the stdout and stderr
     * * and return it as a string
     *
     * @param docUnit - The docTestUnit to evaluate
     * @return the stdout and stderr in a string
     * @throws RuntimeException - if something is going wrong
     *                          The method {@link #run} is exception safe and return the error message back
     */
    private String eval(DocUnit docUnit) {


        switch (docUnit.getLanguage()) {
            case "java":
                return executeJavaCode(docUnit.getCode());
            case "dos":
            case "bash":
                // A shell code can have several commands statement
                List<String[]> commands = DocShell.parseShellCommand(docUnit, docUnit.getLanguage());
                StringBuilder output = new StringBuilder();
                // For each statement
                for (String[] command : commands) {
                    String[] args = command;
                    // the executable of the command
                    final String binaryCliName = args[0];
                    Class<?> importClass = this.docExecutor.getShellCommandMainClass(binaryCliName);

                    /**
                     * Use Java to execute the shell command
                     */
                    StringBuilder javaCode = new StringBuilder();
                    if (importClass != null) {
                        if (docExecutor.isExecuteShellCommandViaShellBinary(binaryCliName)) {
                            throw new RuntimeException("Conflict: The cli " + binaryCliName + " was set to use the main class " + importClass + " and to be execute via the shell binary (bash -c)");
                        }
                        args = Arrays.copyOfRange(args, 1, args.length);
                        javaCode
                                .append(importClass.getName())
                                .append(".main(new String[]{\"")
                                .append(String.join("\",\"", args))
                                .append("\"});\n");
                        output.append(executeJavaCode(javaCode.toString()));
                    } else {

                        List<String> cliCommand;
                        if (docExecutor.isExecuteShellCommandViaShellBinary(binaryCliName)) {

                            cliCommand = Arrays.asList("bash", "-c", docUnit.getCode());

                        } else {

                            // note: We could create the code in one java class
                            // but maven make it impossible to load the org.zeroturnaround.exec package
                            // ie for instance:
                            // mvn test-compile exec:java -Dexec.mainClass="com.combostrap.DocExec" -Dexec.classpathScope="test" -Dexec.args="howto/file/excel"
                            // results in
                            // Error: package org.zeroturnaround.exec does not exist
                            // We were parsing, but we can just call bash for code execution

                            String qualifiedPath = this.toQualifiedPathIfKnown(binaryCliName);
                            cliCommand = new ArrayList<>();
                            cliCommand.add(qualifiedPath);
                            cliCommand.addAll(Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));

                        }

                        try {
                            ProcessExecutor processExecutor = new ProcessExecutor()
                                    .command(cliCommand)
                                    .environment(docUnit.getEnv())
                                    .readOutput(true);
                            if (this.docExecutor.getCaptureStdErr()) {
                                processExecutor.redirectError(System.out);
                            }
                            output.append(
                                    processExecutor
                                            .exitValue(0)
                                            .execute()
                                            .outputUTF8()
                            );
                        } catch (IOException | InterruptedException | TimeoutException e) {
                            // Code exception is not there so we don't grow the normal application stack trace
                            // This is fired only if there is some resource errors
                            throw new RuntimeException("Error while running the command " + cliCommand, e);
                        }

                    }

                }
                return output.toString();
            default:
                throw new RuntimeException("Language (" + docUnit.getLanguage() + ") not yet implemented (Found in " + docUnit.getPath() + ")");
        }


    }

    /**
     * @param cliName - the cli name
     * @return the qualified path or the cli name if unknown
     */
    private String toQualifiedPathIfKnown(String cliName) {
        Path s = this.docExecutor.getShellCommandPath(cliName);
        if (s == null) {
            return cliName;
        }
        return s.toAbsolutePath().toString();
    }


    private String executeJavaCode(String javaCode) {

        // Creation of the java source file
        // You could also extend the SimpleJavaFileObject object as shown in the doc.
        // See SimpleJavaFileObject at https://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html

        // The class name that will be created
        // The file will have the same name, and we will also use it to put it as temporary directory name
        final String buildClassName = "javademo";
        final String runMethodName = "run";

        try {
            // Code
            String code = "public class " + buildClassName + " {\n" +
                    "    public static void " + runMethodName + "() {\n" +
                    "       " + javaCode +
                    "    }\n" +
                    "}";
            DocSource docSource = new DocSource(buildClassName, code);

            // Verification of the presence of the compilation tool archive
            ClassLoader classLoader = DocExecutorUnit.class.getClassLoader();

            String javaHome = System.getProperty("java.home");


            // The compile part
            // Get the compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {

                final String toolsJarFileName = "tools.jar";
                Path toolsJarFilePath = Paths.get(javaHome, "lib", toolsJarFileName);
                String message = "Unable to get the system Java Compiler. Are your running java with a JDK ?";
                if (!Files.exists(toolsJarFilePath)) {
                    message += System.lineSeparator() + "The tools jar file (" + toolsJarFileName + ") could not be found at (" + toolsJarFilePath + ")";
                }
                message += System.lineSeparator() + "Java Home: " + javaHome;
                throw new RuntimeException(message);

            }

            // Create a compilation unit (files)
            Iterable<? extends JavaFileObject> compilationUnits = Collections.singletonList(docSource);
            // A feedback object (diagnostic) to get errors
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            // Javac options here
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(outputDirClass.toString());

            // Add class path to get org.zeroturnaround.exec
            String currentClassPath = System.getProperty("java.class.path");
            log.fine("Using classpath: " + currentClassPath);
            if (currentClassPath != null && !currentClassPath.isEmpty()) {
                options.add("-classpath");
                options.add(currentClassPath);
            }

            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            // Compilation unit can be created and called only once
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    compilationUnits
            );
            // The compile task is called
            task.call();
            // Printing of any compile problems
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {

                final String msg = "Compile Error on line " +
                        diagnostic.getLineNumber() +
                        " source " +
                        diagnostic.getSource() +
                        "\nError: " +
                        diagnostic.getMessage(null);
                log.fine(msg);

                throw new RuntimeException(msg + "\nCode:\n" + code);

            }


            // Now that the class was created, we will load it and run it
            log.fine("Trying to load from " + outputDirClass);
            Class<?> buildClass;
            try (URLClassLoader urlClassLoader = new URLClassLoader(
                    new URL[]{outputDirClass.toUri().toURL()},
                    classLoader)) {
                // Loading the dynamically build class
                buildClass = urlClassLoader.loadClass(buildClassName);
            }
            Method method = buildClass.getMethod(runMethodName);


            // Capturing outputStream and running the command
            PrintStream backupSystemOut = System.out;
            PrintStream backupSystemErr = System.err;
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(byteArrayOutputStream);
            System.setOut(stream);
            if (this.docExecutor.getCaptureStdErr()) {
                System.setErr(stream);
            }
            // Invoke
            try {
                method.invoke(null);
            } catch (InvocationTargetException e) {
                /**
                 * is it a {@link DocExitStatusException} thrown by {@link DocSecurityManager}
                 */
                if (!e.getTargetException().getClass().equals(DocExitStatusException.class)) {
                    // if it's not, throw
                    throw new RuntimeException(e.getCause());
                }
                DocExitStatusException exitStatusException = (DocExitStatusException) e.getTargetException();
                if (exitStatusException.getExitStatus() != 0) {
                    // Error
                    System.out.flush(); // Into the byteArray
                    System.err.flush(); // Into the byteArray
                    String consoleOutput;
                    if (byteArrayOutputStream.size() == 0) {
                        consoleOutput = "No output was received";
                    } else {
                        consoleOutput = byteArrayOutputStream.toString();
                    }
                    throw new RuntimeException("Error has been seen.\nCode:\n" + javaCode + "Console Output: \n" + consoleOutput, e);
                }
                log.infoSecondLevel("Code execution with System exit with 0 has been prevented");

            } finally {

                // Get the output
                System.out.flush(); // Into the byteArray
                System.err.flush(); // Into the byteArray
                System.setOut(backupSystemOut);
                System.setErr(backupSystemErr);

            }

            return byteArrayOutputStream.toString();

        } catch (NoSuchMethodException | IOException | IllegalAccessException | ClassNotFoundException e) {

            throw new RuntimeException(e);

        }

    }


    /**
     * Call the function {@link #eval(DocUnit)} but is safe of exception
     * It returns the error message if an error occurs
     * The string is also trimmed to suppress the newline and other characters
     *
     * @param docUnit - The docTestUnit to run
     * @return the code evaluated or the message error trimmed
     */
    public String run(DocUnit docUnit) {

        DocSecurityManager securityManager = docExecutor.getSecurityManager();
        try {
            securityManager.setCodeIsRunning(true);
            return eval(docUnit).trim();
        } finally {
            securityManager.setCodeIsRunning(false);
        }


    }


}
