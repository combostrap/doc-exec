package com.combostrap.docExec.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Path;

public class Xml {


    /**
     * @param path       - the xml file
     * @param expression Uses local-name() to ignore the default namespace
     * @return the value
     */
    public static String extractString(Path path, String expression) {
        try {
            // 1. Setup Document Builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Important: Disable namespace-awareness for the simple XPath to work
            // If you enable it, you must configure a NamespaceContext for the XPath
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 2. Parse the POM file
            Document doc = builder.parse(path.toFile());

            // 3. Setup XPath
            XPath xPath = XPathFactory.newInstance().newXPath();

            // 5. Evaluate the expression
            Node node = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);

            // 6. Return the value
            if (node != null) {
                return node.getNodeValue().trim();
            }

            return null;


        } catch (Exception e) {
            throw new RuntimeException("Error extracting node infor: " + e.getMessage(), e);
        }
    }
}

