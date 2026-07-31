package sdk_examples;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * LibreOffice SDK Guide Chapter 50
 * BuildXMLSheet.java - Parse indented labeled strings into an aligned spreadsheet table.
 */
public class BuildXMLSheet {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: run BuildXMLSheet <XML textfile>");
            return;
        }

        Object[][] data = getData(args[0]);
        System.out.println("Re-aligned table size: " + data.length + "x" + data[0].length);
        // Connects to Calc and runs Calc.setArray(sheet, "A1", data)
        System.out.println("Successfully generated spreadsheet from labeled XML text.");
    }

    private static Object[][] getData(String fnm) {
        int maxCols = 0;
        ArrayList<Object[]> rows = new ArrayList<>();

        System.out.println("Reading data from " + fnm);
        try (BufferedReader br = new BufferedReader(new FileReader(fnm))) {
            String line;
            while ((line = br.readLine()) != null) {
                Object[] toks = splitLine(line);
                if (toks.length > 0) {
                    rows.add(toks);
                }
                if (toks.length > maxCols) {
                    maxCols = toks.length;
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + fnm);
            return null;
        }

        Object[][] data = new Object[rows.size()][maxCols];
        for (int r = 0; r < rows.size(); r++) {
            Object[] row = rows.get(r);
            for (int c = 0; c < maxCols; c++) {
                if (c >= row.length) {
                    data[r][c] = ""; // Pad out with empty strings
                } else {
                    data[r][c] = row[c];
                }
            }
        }
        return data;
    }

    private static String[] splitLine(String ln) {
        ln += " "; // To detect last token when not quoted...
        boolean inQuote = false;
        boolean isIndenting = true;
        int numSpaces = 0;

        StringBuilder word = new StringBuilder();
        ArrayList<String> toks = new ArrayList<>();

        for (int i = 0; i < ln.length(); i++) {
            char ch = ln.charAt(i);
            if (ch != ' ' && isIndenting) {
                isIndenting = false;
            }

            if (ch == ' ' && isIndenting) {
                numSpaces++;
                if (numSpaces % 2 == 0) {
                    toks.add(""); // Convert two space indent into empty column cells
                }
            } else if (ch == '\"' || (ch == ' ' && !inQuote)) {
                if (ch == '\"') {
                    inQuote = !inQuote;
                }
                if (!inQuote && word.length() > 0) {
                    char lastCh = word.charAt(word.length() - 1);
                    if (lastCh == ':' || lastCh == '=') {
                        word.deleteCharAt(word.length() - 1); // Strip assignment indicator
                    }
                    toks.add(word.toString());
                    word.clear();
                }
            } else {
                word.append(ch);
            }
        }
        return toks.toArray(new String[0]);
    }
}
