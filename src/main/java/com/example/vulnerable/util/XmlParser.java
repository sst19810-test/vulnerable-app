package com.example.vulnerable.util;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

@Component
public class XmlParser {

    // VULNERABILITY: XXE (XML External Entity) injection
    public String parse(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // VULNERABILITY: External entities not disabled
            // factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

            return "Parsed: " + doc.getDocumentElement().getNodeName();
        } catch (Exception e) {
            e.printStackTrace();  // VULNERABILITY: Stack trace exposure
            return "Error parsing XML: " + e.getMessage();
        }
    }

    // VULNERABILITY: XPath Injection
    public String xpathQuery(String xmlContent, String userInput) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

            javax.xml.xpath.XPathFactory xPathfactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xPathfactory.newXPath();

            // VULNERABILITY: XPath injection
            String expression = "//user[@name='" + userInput + "']";
            return xpath.evaluate(expression, doc);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
