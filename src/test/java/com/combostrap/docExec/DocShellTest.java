package com.combostrap.docExec;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


/**
 * Test the execution of shell commands
 */
public class DocShellTest {

    /**
     * We execute an os command
     */
    @Test
    public void osCommandTest() {
        final Path path = Paths.get("./src/test/resources/docTest/dos.txt");
        List<DocUnit> docUnits = DocParser.getDocTests(path);
        DocUnit docUnit = docUnits.get(0);
        DocExecutorUnit docExecutorUnit = DocExecutor.
                create("test")
                .setCaptureStdErr(false)
                .build()
                .getDocExecutorUnit();
        String result = docExecutorUnit.run(docUnit);
        String console = docUnit.getConsole();
        Assertions.assertEquals(console.trim(), result, "The run and the expectations are the same");
    }

    /**
     * We provide the echo command
     */
    @Test
    public void commandWithClassTest() {
        final Path path = Paths.get("./src/test/resources/docTest/dos.txt");
        List<DocUnit> docUnits = DocParser.getDocTests(path);
        DocUnit docUnit = docUnits.get(0);
        DocExecutorUnit docExecutorUnit = DocExecutor.create("test")
                .setShellCommandExecuteViaMainClass("echo", DocCommandEcho.class)
                .build()
                .getDocExecutorUnit();
        String result = docExecutorUnit.run(docUnit);
        Assertions.assertEquals(docUnit.getConsole().trim(), result, "The run and the expectations are the same");
    }

    @Test
    public void parsingDos() {
        String code = "echo Foo\r\n" +
                ":: Comments\r\n" +
                "echo Yolo";
        DocUnit docUnit = DocUnit.get().setCode(code);
        List<String[]> commands = DocShell.parseShellCommand(docUnit, DocShell.DOS_LANG);
        Assertions.assertEquals(2, commands.size(), "There is only two commands in this code");
        String[] command1 = {"echo", "Foo"};
        Assertions.assertEquals(String.join(",", command1), String.join(",", commands.get(0)), "The first command");
    }

    @Test
    public void parsingBash() {
        String code = "echo Foo\n" +
                "# Comments\n" +
                "echo Yolo";
        DocUnit docUnit = DocUnit.get().setCode(code);
        List<String[]> commands = DocShell.parseShellCommand(docUnit, DocShell.BASH_LANG);
        Assertions.assertEquals(2, commands.size(), "There is only two commands in this code");
        String[] command1 = {"echo", "Foo"};
        Assertions.assertEquals(String.join(",", command1), String.join(",", commands.get(0)), "The first command");
    }

    /**
     * Test backslash in bash command
     */
    @Test
    public void bashWithBackslash() {
        final Path path = Paths.get("./src/test/resources/docTest/bash-with-backslash-command.txt");
        List<DocUnit> docUnits = DocParser.getDocTests(path);
        DocUnit docUnit = docUnits.get(0);
        DocExecutorUnit docExecutorUnit = DocExecutor.
                create("test")
                .setCaptureStdErr(false)
                .build()
                .getDocExecutorUnit();
        String result = docExecutorUnit.run(docUnit);
        String console = docUnit.getConsole();
        Assertions.assertEquals(console.trim(), result, "The run and the expectations are the same");
    }

    @Test
    public void envWindowsTest() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (!isWindows) {
            return;
        }
        String code = "echo %USERPROFILE%";
        DocUnit docUnit = DocUnit.get().setCode(code);
        List<String[]> commands = DocShell.parseShellCommand(docUnit, DocShell.DOS_LANG);
        final String userProfileExpansion = commands.get(0)[1];
        Assertions.assertTrue(userProfileExpansion.contains("Users"), "contains the user directory (ie C:\\Users\\name)");
    }

    @Test
    public void envBashTest() {
        String os = System.getProperty("os.name");
        if (!os.toLowerCase().contains("linux")) {
            return;
        }
        String code = "echo $HOME";
        DocUnit docUnit = DocUnit.get().setCode(code);
        List<String[]> commands = DocShell.parseShellCommand(docUnit, DocShell.BASH_LANG);
        final String userProfileExpansion = commands.get(0)[1];
        Assertions.assertEquals(System.getenv().get("HOME"), userProfileExpansion, "contains the user directory");

    }
}
