package sdk_examples;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * LibreOffice SDK Guide Chapter 50
 * ExtractXMLInfo.java - Extract element names and attribute assignments as labeled lines.
 */
public class ExtractXMLInfo {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: run ExtractXMLInfo <XML file>");
            return;
        }

        File xmlFile = new File(args[0]);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        String outFnm = args[0].substring(0, args[0].lastIndexOf('.')) + "XML.txt";
        System.out.println("Writing XML data to labeled file: " + outFnm);
        PrintWriter pw = new PrintWriter(new FileWriter(outFnm));

        NodeList root = doc.getChildNodes();
        for (int i = 0; i < root.getLength(); i++) {
            visitNode(pw, root.item(i), "");
            pw.write("\n");
        }
        pw.close();
        System.out.println("Finished stripping XML tags.");
    }

    private static void visitNode(PrintWriter pw, Node node, String ind) {
        if (node.getNodeType() != Node.ELEMENT_NODE) return;

        pw.write(ind + node.getNodeName());
        visitAttrs(pw, node);

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                String trimmedVal = child.getNodeValue().trim();
                if (trimmedVal.length() == 0) {
                    pw.write("\n");
                } else {
                    pw.write(": \"" + trimmedVal + "\""); // Element content with ':'
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                visitNode(pw, child, ind + "  ");
            }
        }
    }

    private static void visitAttrs(PrintWriter pw, Node node) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                pw.write("  " + attr.getNodeName() + "= \"" + attr.getNodeValue() + "\""); // Attribute names with '='
            }
        }
    }
}
