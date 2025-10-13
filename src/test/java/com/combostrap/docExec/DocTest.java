package com.combostrap.docExec;


import com.combostrap.docExec.util.Fs;
import com.combostrap.docExec.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DocTest {

    @Test
    public void baselineTest() {

        // Parsing
        List<DocUnit> docUnits = DocParser.getDocTests("./src/test/resources/docTest/Baseline.txt");
        final int expected = 4;
        Assertions.assertEquals(expected, docUnits.size(), expected + " tests were found");

        DocExecutor docExecutor = DocExecutor.create("test")
                .setShellCommandExecuteViaMainClass("echo", DocCommandEcho.class);
        // A runnner
        DocExecutorUnit docExecutorUnit = DocExecutorUnit.create(docExecutor);

        // First test
        final DocUnit firstDocUnit = docUnits.get(0);
        String testName = "first test";
        Assertions.assertEquals("System.out.println(\"First test\");", firstDocUnit.getCode().trim(), testName + ": Code was found");
        Assertions.assertEquals("First test", firstDocUnit.getConsole().trim(), testName + ": Expectation was found");
        Assertions.assertEquals(firstDocUnit.getConsole().trim(), docExecutorUnit.run(firstDocUnit), testName + ": Expectation and result are the same");
        Assertions.assertEquals("java", firstDocUnit.getLanguage(), testName + ": Language is java");
        Assertions.assertEquals(Integer.valueOf(127), firstDocUnit.getConsoleLocation().getStart(), testName + " : The first index of the console is correct");
        Assertions.assertEquals(Integer.valueOf(151), firstDocUnit.getConsoleLocation().getEnd(), testName + " : The second index of the console is correct");


        final DocUnit secondDocUnit = docUnits.get(1);
        testName = "second test";
        Assertions.assertEquals("System.out.println(Second test\");", secondDocUnit.getCode().trim(), testName + ": Code was found");
        Assertions.assertNull(secondDocUnit.getConsole(), testName + ": Expectation is null");
        boolean error = false;
        try {
            docExecutorUnit.run(secondDocUnit);
        } catch (Exception e) {
            error = true;
        }
        Assertions.assertTrue(error, testName + ": Error when running");
        Assertions.assertEquals("java", secondDocUnit.getLanguage(), testName + ": Language is java");
        Assertions.assertNull(secondDocUnit.getConsoleLocation(), testName + " : The indexes of the expectation is null");

        final DocUnit thirdDocUnit = docUnits.get(2);
        testName = "third test";
        Assertions.assertEquals("System.out.println(\"Third test\");", thirdDocUnit.getCode().trim(), testName + ": Code was found");
        Assertions.assertNull(thirdDocUnit.getConsole(), testName + ": Expectation is null");
        Assertions.assertNotEquals(thirdDocUnit.getConsole(), docExecutorUnit.run(thirdDocUnit), testName + ": Expectation is null");
        Assertions.assertEquals("java", thirdDocUnit.getLanguage(), testName + ": Language is java");
        Assertions.assertNull(thirdDocUnit.getConsoleLocation(), testName + " : The indexes of the expecation is null");


        final DocUnit fourthDocUnit = docUnits.get(3);
        testName = "fourth test";

        Assertions.assertEquals("echo Hello Foo", fourthDocUnit.getCode().trim(), testName + ": Code was found");
        Assertions.assertEquals("Hello Foo", fourthDocUnit.getConsole().trim(), testName + ": Expectation is the same");
        Assertions.assertEquals("dos", fourthDocUnit.getLanguage(), testName + ": Language is dos");
        Assertions.assertEquals(fourthDocUnit.getConsole().trim(), docExecutorUnit.run(fourthDocUnit), testName + ": Expectation and result are the same");
        Assertions.assertEquals(Integer.valueOf(549), fourthDocUnit.getConsoleLocation().getStart(), testName + " : The first index of the expectation is correct");
        Assertions.assertEquals(Integer.valueOf(572), fourthDocUnit.getConsoleLocation().getEnd(), testName + " : The second index of the expectation is correct");

    }


    /**
     * Replace an expectation by the real result
     */
    @Test
    public void replaceDocTest() throws IOException {


        final Path path = Paths.get("./src/test/resources/docTest/TobeUpdated.txt");
        List<DocUnit> docUnits = DocParser.getDocTests(path);
        DocUnit docUnit = docUnits.get(0);
        DocExecutor docExecutor = DocExecutor.create("test");
        String result = DocExecutorUnit.create(docExecutor).run(docUnit);
        Assertions.assertNotEquals(docUnit.getConsole(), result, "The run and the expectations are not the same");


        String content = Fs.toString(path);
        String newContent = new StringBuilder()
                .append(content, 0, docUnit.getConsoleLocation().getStart())
                .append(result)
                .append(content, docUnit.getConsoleLocation().getEnd(), content.length())
                .toString();

        Path tempFile = Files.createTempFile("doctest", ".txt");
        Fs.toFile(newContent, tempFile);

        docUnits = DocParser.getDocTests(tempFile);
        docUnit = docUnits.get(0);
        Assertions.assertEquals(docUnit.getConsole(), result, "The run and the expectations are now the same with the file (" + tempFile.toAbsolutePath() + ")");


    }

    /**
     * When giving a shell code, you may use environment variable
     */
    @Test
    public void envSettingsInDosTest() {

        final Path path = Paths.get("./src/test/resources/docTest/withEnv.txt");
        DocUnit docUnit = DocParser.getDocTests(path).get(0);
        DocExecutor docExecutor = DocExecutor.create("test")
                .setShellCommandExecuteViaMainClass("echo", DocCommandEcho.class);
        DocExecutorUnit docExecutorUnit = DocExecutorUnit.create(docExecutor);


        Assertions.assertEquals(docUnit.getConsole().trim(), docExecutorUnit.run(docUnit), "The run and the expectations are the same ");

    }

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

}
