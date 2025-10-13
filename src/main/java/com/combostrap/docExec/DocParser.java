package com.combostrap.docExec;


import com.combostrap.docExec.util.Fs;
import com.combostrap.docExec.util.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Static methods to parse a file into DocTestUnit
 */
public class DocParser {

    /**
     * @param path - where the doc is located
     * @return list of DocTestUnit parsed from the file defined by the path
     */
    static public List<DocUnit> getDocTests(Path path) {

        List<DocUnit> docUnits = new ArrayList<>();


        final String unitTestNode = "unit";
        final String codeNodeName = "code";
        final String fileNodeName = "file";
        final String consoleNodeName = "console";


        Pattern unitTestNodePattern = Pattern.compile("<" + unitTestNode + "(.*?)>(.*?)</" + unitTestNode + ">", Pattern.DOTALL);
        Pattern codeTestNodePattern = Pattern.compile("<" + codeNodeName + "(.*?)>(.*?)</" + codeNodeName + ">", Pattern.DOTALL);
        Pattern consolePattern = Pattern.compile("<" + consoleNodeName + "(.*?)>(.*?)</" + consoleNodeName + ">", Pattern.DOTALL);
        Pattern filePattern = Pattern.compile("<" + fileNodeName + "(.*?)>(.*?)</" + fileNodeName + ">", Pattern.DOTALL);
        String s = Fs.toString(path);

        Matcher unitTestMatcher = unitTestNodePattern.matcher(s);
        while (unitTestMatcher.find()) {

            DocUnit docUnit = new DocUnit();
            docUnit.setPath(path);
            docUnits.add(docUnit);
            String unitTestProperties = unitTestMatcher.group(1);
            docUnit.setProperty(unitTestProperties);
            String unitTestDefinition = unitTestMatcher.group(2);
            final int unitTestStartLocation = unitTestMatcher.start() + 1 + unitTestNode.length() + unitTestProperties.length() + 1;

            // Try to find the code
            Matcher codeTestMatcher = codeTestNodePattern.matcher(unitTestDefinition);
            boolean unitFound = codeTestMatcher.find();
            if (unitFound) {

                // Properties of the node is now only the language
                String codeProperties = codeTestMatcher.group(1).trim();
                final int i = codeProperties.indexOf(" ");
                if (i == -1) {
                    docUnit.setLanguage(codeProperties);
                } else {
                    docUnit.setLanguage(codeProperties.substring(0, i));
                }

                // The code
                String codeDefinition = codeTestMatcher.group(2);
                docUnit.setCode(codeDefinition);


                int start = codeTestMatcher.start() + unitTestStartLocation + 1 + codeNodeName.length() + 1;
                int end = codeTestMatcher.end() + unitTestStartLocation - 2 - codeNodeName.length() - 1;
                docUnit.setCodeLocation(new DocBlockLocation(start, end));

            }

            // Try to find the console
            Matcher consoleMatcher = consolePattern.matcher(unitTestDefinition);
            boolean codeFound = consoleMatcher.find();
            if (codeFound) {
                String fileProperties = consoleMatcher.group(1);
                String consoleContent = consoleMatcher.group(2);
                int start = consoleMatcher.start() + unitTestStartLocation + 1 + consoleNodeName.length() + fileProperties.length() + 1;
                int end = consoleMatcher.end() + unitTestStartLocation - 2 - consoleNodeName.length() - 1;
                docUnit.setConsoleLocation(new DocBlockLocation(start, end));
                docUnit.setConsoleContent(consoleContent);
            }

            // Try to find the file
            Matcher fileMatcher = filePattern.matcher(unitTestDefinition);
            boolean fileFound = fileMatcher.find();
            if (fileFound) {

                DocFileBlock docFileBlock = DocFileBlock.get(docUnit);
                docUnit.addFileBlock(docFileBlock);
                String fileProperties = fileMatcher.group(1);
                String[] properties = fileProperties.trim().split(" ");
                if (properties.length >= 1) {
                    docFileBlock.setLanguage(properties[0]);
                }
                if (properties.length >= 2) {
                    docFileBlock.setPath(properties[1]);
                }

                String fileContent = fileMatcher.group(2).trim();
                docFileBlock.setContent(fileContent);

                int startLocation = fileMatcher.start() + unitTestStartLocation + 1 + fileNodeName.length() + fileProperties.length() + 1;
                int locationEnd = fileMatcher.end() + unitTestStartLocation - 2 - fileNodeName.length() - 1;
                docFileBlock.setLocationStart(startLocation);
                docFileBlock.setLocationEnd(locationEnd);

            }

            // Test that the file node is before the console node
            for (DocFileBlock docFileBlock : docUnit.getFileBlocks()) {

                // Console is not mandatory
                final DocBlockLocation consoleLocation = docUnit.getConsoleLocation();
                if (consoleLocation != null) {
                    if (docFileBlock.getLocationStart() > consoleLocation.getStart()) {
                        throw new RuntimeException("Order is not good, the console node must be after the file node");
                    }
                }

                // Code is then we have a location
                if (docUnit.getCodeLocation() != null) {
                    if (docFileBlock.getLocationStart() > docUnit.getCodeLocation().getStart()) {
                        throw new RuntimeException("Order is not good, the file node must be before the code node in the doc test file " + docUnit.getPath());
                    }
                }

            }

        }


        // Test that all nodes are closed
        final Integer numberOfUnitTestNode = Strings.numberOfOccurrences(s, "<" + unitTestNode);
        if (docUnits.size() != numberOfUnitTestNode) {
            String message = "A " + unitTestNode + " node seems not to be closed in the file (" + path + "). There is " + numberOfUnitTestNode + " unit test node with the name (" + unitTestNode + ") but we parsed only " + docUnits.size() + " doc unit test code. This unit test is not closed ?:\n";
            message += Stream.of(docUnits.get(docUnits.size() - 1)).map(d -> {
                if (d.getCodeLocation() != null) {
                    return "Code unit: " + d.getCode();
                }
                if (!d.getFileBlocks().isEmpty()) {
                    return "File unit: " + d.getFileBlocks().get(0).toString();
                }
                return "unknown";
            }).collect(Collectors.joining("\n"));
            throw new RuntimeException(message);
        }


        return docUnits;

    }

    /**
     * Alias method that call {@link #getDocTests(Path)}
     *
     * @param path
     * @return a list of doc test from a file
     */
    public static List<DocUnit> getDocTests(String path) {
        return getDocTests(Paths.get(path));
    }

}
