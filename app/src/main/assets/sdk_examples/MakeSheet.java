package sdk_examples;

import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Table;

/**
 * LibreOffice SDK Guide Chapter 51
 * MakeSheet.java - Generate a Spreadsheet (.ods) with tabular math grid inputs.
 */
public class MakeSheet {
    public static void main(String[] args) {
        try {
            System.out.println("Instantiating new SpreadsheetDocument...");
            SpreadsheetDocument doc = SpreadsheetDocument.newSpreadsheetDocument();
            
            // Access sheet by index
            Table sheet = doc.getSheetByIndex(0);
            sheet.getCellByPosition(0, 0).setStringValue("Hello");
            
            // Loop and fill mathematical formulas or multipliers
            for (int row = 0; row < 5; row++) {
                sheet.getCellByPosition(1, row).setDoubleValue(row * 2.0);
            }

            System.out.println("Saving spreadsheet to makeSheet.ods...");
            doc.save("makeSheet.ods");
            doc.close();
        } catch (Exception e) {
            System.out.println("Exception creating sheet: " + e.getMessage());
        }
    }
}
