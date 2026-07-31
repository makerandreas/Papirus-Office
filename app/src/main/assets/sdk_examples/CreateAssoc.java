package sdk_examples;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * LibreOffice SDK Guide Chapter 50
 * CreateAssoc.java - Parse association & club records into spreadsheets.
 */
public class CreateAssoc {
    public static void main(String[] args) {
        try {
            File xmlFile = new File("clubs.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            
            NodeList root = doc.getChildNodes();
            Node cdb = getNode("club-database", root);
            Node assoc1 = getNode("association", cdb.getChildNodes());
            NodeList clubs = assoc1.getChildNodes();

            // Extract select columns to display
            Object[][] data = getAllNodeValues(clubs, new String[]{"name", "contact", "location", "phone", "email"});
            System.out.println("Associations loaded. Main grid mapped.");
        } catch (Exception e) {
            System.out.println("Failed to build association spreadsheet: " + e.getMessage());
        }
    }

    private static Node getNode(String tagName, NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName().equalsIgnoreCase(tagName)) {
                return nodes.item(i);
            }
        }
        return null;
    }

    private static Object[][] getAllNodeValues(NodeList rowNodes, String[] colIDs) {
        // Implementation similar to CreatePay.java
        return new Object[rowNodes.getLength()][colIDs.length];
    }
}
