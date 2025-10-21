package com.combostrap.docExec;


import com.combostrap.docExec.util.Fs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DocFileBlockTest {


  @Test
  public void baselineTest() throws IOException {

    final Path path = Paths.get("./src/test/resources/docTest/fileTest.txt");
    List<DocUnit> docUnits = DocParser.getDocTests(path);
    DocUnit docUnit = docUnits.get(0);
    List<DocFileBlock> docFileBlocks = docUnit.getFileBlocks();
    Assertions.assertEquals(1, docFileBlocks.size(), "One file was found");
    Assertions.assertEquals("docFile/file.txt", docFileBlocks.get(0).getPath(), "Path is good");
    Assertions.assertEquals("txt", docFileBlocks.get(0).getLanguage(), "Language is good");

    DocExecutorUnit docExecutorUnit = DocExecutor.create("test")
      .setShellCommandExecuteViaMainClass("cat", DocCommandCat.class)
            .build()
            .getDocExecutorUnit();
    String result = docExecutorUnit.run(docUnit);
    Assertions.assertNotEquals(docUnit.getConsole(), result, "The run and the expectations are not the same");


    String content = Fs.toString(path);
    String newContent = content.substring(0, docUnit.getConsoleLocation().getStart()) +
      result + content.substring(docUnit.getConsoleLocation().getEnd());

    Path tempFile = Files.createTempFile("doctest", ".txt");
    Fs.toFile(newContent, tempFile);

    docUnits = DocParser.getDocTests(tempFile);
    docUnit = docUnits.get(0);
    Assertions.assertEquals(docUnit.getConsole(), result, "The run and the expectations are now the same with the file (" + tempFile.toAbsolutePath() + ")");

  }


}
