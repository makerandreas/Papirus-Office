package sdk_examples;

import java.net.URI;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.text.list.List;
import org.odftoolkit.simple.table.Table;
import org.odftoolkit.simple.table.Cell;

/**
 * LibreOffice SDK Guide Chapter 51
 * MakeTextDoc.java - Create an ODF Text Document (.odt) containing an image, list, and simple table.
 */
public class MakeTextDoc {
    public static void main(String[] args) {
        try {
            System.out.println("Instantiating new TextDocument...");
            TextDocument doc = TextDocument.newTextDocument();
            
            // Add a logo image
            doc.newImage(new URI("odf-logo.png"));

            // Add paragraphs
            doc.addParagraph("Hello World, Hello Simple ODF!");
            doc.addParagraph("The following is a list.");
            
            // Add a styled bullet list
            List list = doc.addList();
            String[] items = {"item1", "item2", "item3"};
            list.addItems(items);

            // Add a data table
            Table table = doc.addTable(2, 2);
            Cell cell = table.getCellByPosition(0, 0);
            cell.setStringValue("Hello World!");

            System.out.println("Saving document to MakeTextDoc.odt...");
            doc.save("MakeTextDoc.odt");
            doc.close();
        } catch (Exception e) {
            System.out.println("Exception building text document: " + e.getMessage());
        }
    }
}
