package sdk_examples;

import com.sun.star.text.*;
import com.sun.star.view.XSelectionSupplier;
import com.sun.star.frame.XController;
import com.sun.star.frame.XModel;
import com.sun.star.uno.UnoRuntime;

/**
 * LibreOffice SDK Guide Chapter 43
 * CopyPasteText.java - Sentence traversal and copying in Writer.
 */
public class CopyPasteText {

    public static void main(String[] args) {
        System.out.println("Executing Writer Copy-and-Paste Scenario...");
    }

    public static void useClipUtils(XTextDocument doc, int n) {
        // Invisible traversal using sentence cursor
        XSentenceCursor senCursor = getSentenceCursor(doc);
        senCursor.gotoStart(false);
        gotoSentence(senCursor, n);

        // Copy text to clipboard
        Clip.setText(senCursor.getString());
        System.out.println("Copied: " + senCursor.getString());
    }

    public static void useDispatches(XTextDocument doc, int n) {
        // Visible highlighting of text in window
        XTextViewCursor tvc = getViewCursor(doc);
        XSentenceCursor senCursor = getSentenceCursor(doc);
        senCursor.gotoStart(false);
        gotoSentence(senCursor, n);

        // Highlight selection visually
        tvc.gotoRange(senCursor.getStart(), false);
        tvc.gotoRange(senCursor.getEnd(), true);

        // Dispatch "Copy"
        System.out.println("Dispatched 'Copy' for highlighted text block.");
    }

    private static void gotoSentence(XSentenceCursor senCursor, int n) {
        do {
            senCursor.gotoEndOfSentence(true);
            n--;
        } while (n > 0 && senCursor.gotoNextSentence(false));
    }

    private static XSentenceCursor getSentenceCursor(XTextDocument doc) {
        XText xText = doc.getText();
        XTextCursor xTextCursor = xText.createTextCursor();
        return UnoRuntime.queryInterface(XSentenceCursor.class, xTextCursor);
    }

    private static XTextViewCursor getViewCursor(XTextDocument textDoc) {
        XModel model = UnoRuntime.queryInterface(XModel.class, textDoc);
        XController xController = model.getCurrentController();
        XTextViewCursorSupplier supplier = UnoRuntime.queryInterface(XTextViewCursorSupplier.class, xController);
        return supplier.getViewCursor();
    }
}
