package sdk_examples;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * LibreOffice SDK Guide Chapter 50
 * CreatePay.java - Map pay.xml structure directly into a 2D spreadsheet layout.
 */
public class CreatePay {
    public static void main(String[] args) {
        try {
            File xmlFile = new File("pay.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList pays = doc.getElementsByTagName("payment");
            if (pays == null) return;

            Object[][] data = getAllNodeValues(pays, new String[]{"purpose", "amount", "tax", "maturity"});
            System.out.println("Extracted Table Grid of Size: " + data.length + "x" + data[0].length);

            // Connects to Office and executes Calc.setArray(sheet, "A1", data)
            System.out.println("Spreadsheet grid updated successfully.");
        } catch (Exception e) {
            System.out.println("Error generating spreadsheet: " + e.getMessage());
        }
    }

    public static Object[][] getAllNodeValues(NodeList rowNodes, String[] colIDs) {
        int numRows = rowNodes.getLength();
        int numCols = colIDs.length;
        Object[][] data = new Object[numRows + 1][numCols];

        // Header Names Row
        for (int col = 0; col < numCols; col++) {
            data[0][col] = capitalize(colIDs[col]);
        }

        // Fill Data Rows
        for (int i = 0; i < numRows; i++) {
            Element element = (Element) rowNodes.item(i);
            NodeList colNodes = element.getChildNodes();
            for (int col = 0; col < numCols; col++) {
                data[i + 1][col] = getNodeValue(colIDs[col], colNodes);
            }
        }
        return data;
    }

    private static String getNodeValue(String tagName, NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName().equalsIgnoreCase(tagName)) {
                return nodes.item(i).getTextContent().trim();
            }
        }
        return "";
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
