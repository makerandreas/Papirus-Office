package sdk_examples;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * LibreOffice SDK Guide Chapter 43
 * JCPTests.java - Java Clipboard API test for transferring 2D Arrays.
 */
public class JCPTests {
    private static final DataFlavor ARRAY_DF = new DataFlavor(Object[][].class, "2D Object Array");

    public static void main(String[] args) {
        System.out.println("Initializing JClip Java session...");

        // Define a 2D Object array (such as spreadsheet cells or database query rows)
        Object[][] studentMarks = {
            {"courseId", "subjectId", "courseNumber", "title", "numOfCredits"},
            {11111, "CSCI", 1301, "Introduction to Java I", 4},
            {11112, "CSCI", 1302, "Introduction to Java II", 3},
            {11113, "CSCI", 3720, "Database Systems", 3},
            {11114, "CoE", 3721, "Algorithms", 3}
        };

        // Copy array to Java Clipboard
        boolean success = setArray(studentMarks);
        if (success) {
            System.out.println("Added 2D array of student marks to clipboard.");
        }

        // Retrieve and print array from clipboard
        Object[][] retrieved = getArray();
        if (retrieved != null) {
            System.out.println("\nSuccessfully read 2D array from Clipboard. Size: " + retrieved.length + "x" + retrieved[0].length);
            for (Object[] row : retrieved) {
                for (Object cell : row) {
                    System.out.print(cell + "\t");
                }
                System.out.println();
            }
        }
    }

    public static boolean setArray(Object[][] vals) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            cb.setContents(new JArrayTransferable(vals), null);
            return true;
        } catch (IllegalStateException e) {
            System.out.println("Error setting array contents: " + e.getMessage());
            return false;
        }
    }

    public static Object[][] getArray() {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable trf = cb.getContents(null);
        if (trf != null && trf.isDataFlavorSupported(ARRAY_DF)) {
            try {
                return (Object[][]) trf.getTransferData(ARRAY_DF);
            } catch (Exception e) {
                System.out.println("Error reading array flavor: " + e.getMessage());
            }
        }
        return null;
    }
}

class JArrayTransferable implements Transferable {
    private Object[][] vals;
    private DataFlavor arrDF;

    public JArrayTransferable(Object[][] vals) {
        this.vals = vals;
        this.arrDF = new DataFlavor(Object[][].class, "2D Object Array");
    }

    public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
        if (df.equals(arrDF) && vals != null) {
            return vals;
        }
        throw new UnsupportedFlavorException(df);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{arrDF};
    }

    public boolean isDataFlavorSupported(DataFlavor df) {
        return df.equals(arrDF);
    }
}
