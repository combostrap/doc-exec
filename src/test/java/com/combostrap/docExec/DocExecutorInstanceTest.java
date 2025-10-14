package com.combostrap.docExec;


import com.combostrap.docExec.util.Fs;
import com.combostrap.docExec.util.Sorts;
import com.combostrap.docExec.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DocExecutorInstanceTest {


    /**
     * Run against a doc without expectation
     */
    @Test
    public void docTestWithoutConsole() {

        List<DocExecutorResult> doc =
                DocExecutor.create("whatever")
                        .setShellCommandExecuteViaMainClass("echo", DocCommandEcho.class)
                        .build()
                        .run(Paths.get("./src/test/resources/docTest/withoutExpectation.txt"));

        Assertions.assertEquals(1, doc.size());
    }

    @Test
    public void overWriteFileContentTest() throws IOException {

        final Path rootFile = Paths.get("./src/test/resources");
        Path docToRun = Paths.get("./src/test/resources/docTest/fileTest.txt");
        DocExecutorResult docTestRun = DocExecutor.create("defaultRun")
                .setShellCommandExecuteViaMainClass("cat", DocCommandCat.class)
                .setSearchFilePaths(rootFile)
                .setEnableCache(false)
                .build()
                .run(docToRun)
                .get(0);
        Assertions.assertEquals(0, docTestRun.getErrors(), "No Errors were seen");

        Path newPath = Files.createTempFile("replaceFileContent", "txt");
        String newDoc = docTestRun.getNewDoc();
        Fs.toFile(newDoc, newPath);

        // The new file structure must not be deleted
        List<DocUnit> docUnits = DocParser.getDocTests(newPath);
        Assertions.assertEquals(3, docUnits.size(), "Three unit");

        // First block
        List<DocFileBlock> fileBlocks = docUnits.get(0).getFileBlocks();
        Assertions.assertEquals(1, fileBlocks.size(), "One file block");
        DocFileBlock docFileBlock = fileBlocks.get(0);
        String docTestContent = docFileBlock.getContent();
        String fileContent = Fs.toString(Paths.get(rootFile.toString(), "docFile", "file.txt"));
        Assertions.assertEquals(Strings.normalize(fileContent), Strings.normalize(docTestContent), "Content should be the same");

        // Second block
        fileBlocks = docUnits.get(1).getFileBlocks();
        Assertions.assertEquals(1, fileBlocks.size(), "One file block");
        docFileBlock = fileBlocks.get(0);
        docTestContent = docFileBlock.getContent();
        fileContent = Fs.toString(Paths.get(rootFile.toString(), "docFile", "file.yml"));
        Assertions.assertEquals(Strings.normalize(fileContent), Strings.normalize(docTestContent), "Content should be the same");

        // Third block
        fileBlocks = docUnits.get(2).getFileBlocks();
        Assertions.assertEquals(1, fileBlocks.size(), "One file block");
        docFileBlock = fileBlocks.get(0);
        docTestContent = docFileBlock.getContent();
        fileContent = Fs.toString(Paths.get(rootFile.toString(), "docFile", "file.yml"));
        Assertions.assertEquals(Strings.normalize(fileContent), Strings.normalize(docTestContent), "Content should be the same for the third");

        // Last code unit section is the same
        String docToRunString = Fs.toString(docToRun);
        String unitClosingTag = "</unit>";
        String lastPart = docToRunString.substring(docToRunString.lastIndexOf(unitClosingTag) + unitClosingTag.length());
        String lastPartNewDoc = newDoc.substring(newDoc.lastIndexOf(unitClosingTag) + unitClosingTag.length());
        Assertions.assertEquals(lastPart, lastPartNewDoc, "The last part of the doc should be equal");

    }

    /**
     * When the doc terminates with a unit node, no eol should be added
     */
    @Test
    public void overWriteFileContentSecondTest() {

        final Path rootFile = Paths.get("./src/test/resources");
        Path docToRun = Paths.get("./src/test/resources/docTest/overwriteSecond.txt");
        String before = Fs.toString(docToRun);
        DocExecutorResult docTestRun = DocExecutor.create("defaultRun")
                .setEnableCache(false)
                .setShellCommandExecuteViaMainClass("echo", DocCommandEcho.class)
                .setSearchFilePaths(rootFile)
                .build()
                .run(docToRun)
                .get(0);
        Assertions.assertEquals(0, docTestRun.getErrors(), "No Errors were seen");


        String newDoc = docTestRun.getNewDoc();

        // Last code unit section is the same
        String docToRunString = Fs.toString(docToRun);
        String unitClosingTag = "</unit>";
        String lastPart = docToRunString.substring(docToRunString.lastIndexOf(unitClosingTag) + unitClosingTag.length());
        String lastPartNewDoc = newDoc.substring(newDoc.lastIndexOf(unitClosingTag) + unitClosingTag.length());
        Assertions.assertEquals(lastPart, lastPartNewDoc, "The last part of the doc should be equal");

        // Same
        String after = Fs.toString(docToRun);
        Assertions.assertEquals(before, after);

    }

    /**
     * Test the selection of files
     */
    @Test
    void selectDocsViaGlobs() {

        List<Path> docTestRun = DocExecutor.create("defaultRun")
                .setSearchDocPath(Paths.get("src/test/resources"))
                .build()
                .getPathsFromGlobs("docFile/*");
        System.out.println(docTestRun);
        Assertions.assertEquals(2, docTestRun.size());
        Assertions.assertEquals(Paths.get("src/test/resources/docFile/file.txt"), docTestRun.get(0));
        Assertions.assertEquals(Paths.get("src/test/resources/docFile/README.md"), docTestRun.get(1));

    }

    /**
     * Test the selection of files
     */
    @Test
    void selectDocsViaGlobsWithResumeFromSkip() {

        /**
         * By default, the path comparison is not good
         * Demo, we use the sort natural then
         */
        int comparison = Paths.get("src/test/resources/docFile/file.txt").compareTo(Paths.get("src/test/resources/docFile/file.txt"));
        Assertions.assertEquals(0, comparison);
        comparison = Paths.get("src/test/resources/docFile/file.txt").compareTo(Paths.get("src/test/resources/docFile/gile.txt"));
        Assertions.assertEquals(-1, comparison);
        comparison = Paths.get("src/test/resources/docFile/file.txt").compareTo(Paths.get("src/test/resources/docFile/g"));
        Assertions.assertEquals(-1, comparison);
        comparison = Paths.get("src/test/resources/docFile/file.txt").compareTo(Paths.get("src/test/resources/docFile/R"));
        Assertions.assertEquals(20, comparison, "file is greater than R");
        comparison = Sorts.naturalSortComparator(Paths.get("src/test/resources/docFile/file.txt"), Paths.get("src/test/resources/docFile/R"));
        Assertions.assertEquals(-12, comparison, "file is no more greater than R");


        /**
         * Run
         */
        List<DocExecutorResult> docTestRun = DocExecutor.create("defaultRun")
                .setSearchDocPath(Paths.get("src/test/resources"))
                .setResumeFrom("docFile/R")
                .build()
                .run("docFile/*");
        System.out.println(docTestRun);
        Assertions.assertEquals(1, docTestRun.size());
        Assertions.assertEquals(Paths.get("src/test/resources/docFile/README.md"), docTestRun.get(0).getPath());

    }
}
