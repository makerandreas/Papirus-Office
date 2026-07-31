package sdk_examples;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.XCellRange;
import com.sun.star.view.XSelectionSupplier;
import com.sun.star.uno.UnoRuntime;

/**
 * LibreOffice SDK Guide Chapter 43
 * CopyPasteCalc.java - Copy-pasting cell range grids in Calc.
 */
public class CopyPasteCalc {

    public static void main(String[] args) {
        System.out.println("Executing Spreadsheet Copy-and-Paste Scenario...");
    }

    public static void useClipUtils(XSpreadsheetDocument doc) {
        // Invisible copy using 2D Arrays
        XSpreadsheet sheet = getSheet(doc, 0);
        XCellRange cellRange = sheet.getCellRangeByPosition(0, 2, 6, 2); // row 3

        // Extract cells data array and push to clipboard
        Object[][] data = getCellRangeArray(cellRange);
        JClip.setArray(data);

        // Paste array into row 9
        XCellRange targetRange = sheet.getCellRangeByPosition(0, 8, 6, 8);
        Object[][] clipboardData = JClip.getArray();
        setCellRangeArray(targetRange, clipboardData);

        System.out.println("Transferred cell ranges invisibly using 2D object arrays.");
    }

    public static void useDispatches(XSpreadsheetDocument doc, XSelectionSupplier selSupp) {
        XSpreadsheet sheet = getSheet(doc, 0);
        XCellRange cellRange = sheet.getCellRangeByPosition(0, 2, 6, 2);

        // Highlight selection and copy via OS dispatch
        selSupp.select(cellRange);
        System.out.println("Visible select on range. Dispatching 'Copy' command.");
    }

    private static XSpreadsheet getSheet(XSpreadsheetDocument doc, int index) {
        try {
            Object sheet = doc.getSheets().getByIndex(index);
            return UnoRuntime.queryInterface(XSpreadsheet.class, sheet);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object[][] getCellRangeArray(XCellRange range) {
        // Mock method wrapping XCellRangeData
        return new Object[][]{ {"A1", "B1", "C1"} };
    }

    private static void setCellRangeArray(XCellRange range, Object[][] data) {
        // Mock method wrapping XCellRangeData
    }
}

class JClip {
    public static boolean setArray(Object[][] vals) { return true; }
    public static Object[][] getArray() { return new Object[0][0]; }
}
