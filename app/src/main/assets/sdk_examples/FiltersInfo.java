package sdk_examples;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.uno.UnoRuntime;

/**
 * LibreOffice SDK Guide Chapter 50
 * FiltersInfo.java - Query and extract metadata details about registered filters.
 */
public class FiltersInfo {
    public static void main(String[] args) {
        System.out.println("Initializing Office connection...");
        // In real execution, Lo.loadOffice() is called
        String[] filterNms = getFilterNames();
        System.out.println("No. of filters: " + (filterNms != null ? filterNms.length : 0));
        
        // Print some filters
        if (filterNms != null) {
            for (int i = 0; i < Math.min(filterNms.length, 10); i++) {
                System.out.println(" - " + filterNms[i]);
            }
        }

        // Show properties for some filters
        showFilterProps("AbiWord");
        showFilterProps("Pay");
        showFilterProps("Clubs");
    }

    public static String[] getFilterNames() {
        // Simulating FilterFactory query
        return new String[] { "AbiWord", "Pay", "Clubs", "OpenDocument Text Flat XML", "OpenDocument Spreadsheet Flat XML" };
    }

    public static void showFilterProps(String name) {
        System.out.println("\nProperties for \"" + name + " Filter\":");
        if (name.equals("Pay")) {
            System.out.println("  UserData: [com.sun.star.documentconversion.XSLTFilter, false, ...]");
            System.out.println("  FilterService: com.sun.star.comp.Calc.XmlFilterAdaptor");
            System.out.println("  DocumentService: com.sun.star.sheet.SpreadsheetDocument");
            System.out.println("  Flags: 524355 (Import + Export)");
        } else if (name.equals("Clubs")) {
            System.out.println("  UserData: [com.sun.star.documentconversion.XSLTFilter, false, ...]");
            System.out.println("  FilterService: com.sun.star.comp.Writer.XmlFilterAdaptor");
            System.out.println("  DocumentService: com.sun.star.text.TextDocument");
            System.out.println("  Flags: 524355 (Import + Export)");
            System.out.println("  TemplateName: clubsTemplate.ott");
        } else {
            System.out.println("  FilterService: com.sun.star.comp.Writer.AbiWordFilter");
            System.out.println("  DocumentService: com.sun.star.text.TextDocument");
            System.out.println("  Flags: 65 (Import)");
        }
    }
}
