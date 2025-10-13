package com.combostrap.docExec;


import com.combostrap.docExec.util.Fs;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An example of a {@link DocExecutor#setShellCommandExecuteViaMainClass(String, Class)} MainClass}
 * implementing a basic cat command
 * <p>
 * This class is used for testing purpose
 * <p>
 * In the documentation, you would see something like that
 * <p>
 * cat file.txt
 */
public class DocCommandCat {

    public static void main(String[] args) {

        Path path = Paths.get(args[0]);
        System.out.println(Fs.toString(path));

    }

}
