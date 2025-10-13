package com.combostrap.docExec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DocNodeOrderTest {

    /**
     * File Node must be before Code
     */
    @Test()
    public void docTestFileNodeAfterCode() {

        final Path path = Paths.get("./src/test/resources/docTest/NodeOrderBadfileNodeAfterCode.txt");
        Assertions.assertThrows(RuntimeException.class, () -> DocParser.getDocTests(path));

    }

    /**
     * Console after code node
     */
    @Test()
    public void docTestConsoleNodeAfterCode() {

        final Path path = Paths.get("./src/test/resources/docTest/NodeOrderBadfileNodeAfterCode.txt");
        Assertions.assertThrows(RuntimeException.class, () -> DocParser.getDocTests(path));

    }

    /**
     * Console after code node
     */
    @Test
    public void docTestGoodOrder() {

        final Path path = Paths.get("./src/test/resources/docTest/NodeOrderGood.txt");
        DocParser.getDocTests(path);

    }

}
