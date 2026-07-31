package sdk_examples;

import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Table;

/**
 * LibreOffice SDK Guide Chapter 51
 * CombineSheets.java - Load two spreadsheet files and append the sheets of the second to the first.
 */
public class CombineSheets {
    public static void main(String[] args) {
        try {
            System.out.println("Loading spreadsheets ss1.ods and ss2.ods...");
            SpreadsheetDocument doc1 = SpreadsheetDocument.loadDocument("ss1.ods");
            SpreadsheetDocument doc2 = SpreadsheetDocument.loadDocument("ss2.ods");
            
            int numSheets2 = doc2.getSheetCount();
            System.out.println("ss2 contains " + numSheets2 + " sheets. Appending...");

            for (int i = 0; i < numSheets2; i++) {
                Table t = doc2.getSheetByIndex(i);
                System.out.println("  Appending sheet \"" + t.getTableName() + "\" to doc1...");
                doc1.appendSheet(t, t.getTableName());
            }

            System.out.println("Saving combined spreadsheet to combined.ods...");
            doc1.save("combined.ods");
            doc1.close();
            doc2.close();
        } catch (Exception e) {
            System.out.println("Exception appending sheets: " + e.getMessage());
        }
    }
}
