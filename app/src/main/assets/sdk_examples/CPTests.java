package sdk_examples;

import com.sun.star.uno.XComponentContext;
import com.sun.star.comp.helper.Bootstrap;
import java.awt.image.BufferedImage;

/**
 * LibreOffice SDK Guide Chapter 43
 * CPTests.java - Office Clipboard API execution test.
 */
public class CPTests {
    public static void main(String[] args) {
        System.out.println("Initializing Office environment...");
        try {
            // 1. Set text on Office clipboard
            String timestamp = "Office timestamp: " + System.currentTimeMillis();
            Clip.setText(timestamp);
            System.out.println("Added text to clipboard: " + timestamp);

            // 2. Read text from clipboard
            String clipText = Clip.getText();
            System.out.println("Read clipboard text: " + clipText);

            // 3. List current flavors
            System.out.println("\nListing clipboard data flavors:");
            Clip.listFlavors();

        } catch (Exception e) {
            System.err.println("Error running clipboard tests: " + e.getMessage());
        }
    }
}
