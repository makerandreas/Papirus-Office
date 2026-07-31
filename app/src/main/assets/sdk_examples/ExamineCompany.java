package sdk_examples;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * LibreOffice SDK Guide Chapter 50
 * ExamineCompany.java - DOM tree loading and node/attribute parsing.
 */
public class ExamineCompany {
    public static void main(String[] args) throws Exception {
        String companyXml = "<?xml version=\"1.0\"?><Companies><Company><Name>ABC</Name><Executive type=\"CEO\"><LastName>Smith</LastName><FirstName>Jim</FirstName><street>123 Broad Street</street></Executive></Company></Companies>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(companyXml)));
        
        NodeList root = doc.getChildNodes();
        System.out.println("Root count: " + root.getLength());

        Node comps = getNode("Companies", root);
        Node comp = getNode("Company", comps.getChildNodes());
        Node exec = getNode("Executive", comp.getChildNodes());

        String execType = getNodeAttr("type", exec);
        String lastName = getNodeValue("LastName", exec.getChildNodes());
        String firstName = getNodeValue("FirstName", exec.getChildNodes());

        System.out.println("Executive Type: " + execType);
        System.out.println("Name: " + lastName + ", " + firstName);
    }

    public static Node getNode(String tagName, NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equalsIgnoreCase(tagName)) {
                return node;
            }
        }
        return null;
    }

    public static String getNodeValue(String tagName, NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeName().equalsIgnoreCase(tagName)) {
                return getNodeValue(n);
            }
        }
        return "";
    }

    public static String getNodeValue(Node node) {
        if (node == null) return "";
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                return n.getNodeValue().trim();
            }
        }
        return "";
    }

    public static String getNodeAttr(String attrName, Node node) {
        if (node == null) return "";
        org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) return "";
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (attr.getNodeName().equalsIgnoreCase(attrName)) {
                return attr.getNodeValue().trim();
            }
        }
        return "";
    }
}
