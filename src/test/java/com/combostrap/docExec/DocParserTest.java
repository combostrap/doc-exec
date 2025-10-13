package com.combostrap.docExec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DocParserTest {


  /**
   * The console node is not mandatory
   */
  @Test
  public void consoleNotMandatory() {

    final Path path = Paths.get("./src/test/resources/docTest/ParserNoConsole.txt");
    List<DocUnit> docTests = DocParser.getDocTests(path);
    Assertions.assertEquals(1, docTests.size(), "One unit found");
    Assertions.assertNull(docTests.get(0).getConsoleLocation(), "Console is null");

  }

  @Test
  public void parserConsoleWithAttribute() {

    final Path path = Paths.get("./src/test/resources/docTest/ParserConsoleWithAttribute.txt");
    List<DocUnit> docTests = DocParser.getDocTests(path);
    Assertions.assertEquals(1, docTests.size(), "One unit found");
    DocUnit docUnit = docTests.get(0);
    DocBlockLocation consoleLocation = docUnit.getConsoleLocation();
    Assertions.assertNotNull(consoleLocation, "Console is not null");
    Assertions.assertEquals(90,consoleLocation.getStart(), "Console start");
    Assertions.assertEquals(91,consoleLocation.getEnd(), "Console End (Empty)");

  }

}
