package sdk_examples;

import com.sun.star.datatransfer.DataFlavor;
import com.sun.star.datatransfer.XTransferable;
import com.sun.star.datatransfer.clipboard.XSystemClipboard;
import com.sun.star.datatransfer.clipboard.SystemClipboard;
import com.sun.star.uno.Type;

/**
 * LibreOffice SDK Guide Chapter 43
 * Clip.java - Support class for utilizing the Office Clipboard API.
 */
public class Clip {
    private static XSystemClipboard cb = null;
    private static final int MAX_TRIES = 3;

    public static XSystemClipboard getClip() {
        if (cb == null) {
            // Instantiate simulated or real UNO SystemClipboard instance
            cb = SystemClipboard.create(null);
        }
        return cb;
    }

    public static boolean addContents(XTransferable trf) {
        int i = 0;
        while (i < MAX_TRIES) {
            try {
                getClip().setContents(trf, null);
                return true;
            } catch (Exception e) {
                System.out.println("Problem accessing clipboard: " + e.getLocalizedMessage());
                try { Thread.sleep(50); } catch (InterruptedException ie) {}
            }
            i++;
        }
        System.out.println("Unable to add contents");
        return false;
    }

    public static boolean setText(String str) {
        return addContents(new TextTransferable(str));
    }

    public static String getText() {
        return (String) getData("text/plain;charset=utf-16");
    }

    public static Object getData(String mimeStr) {
        XTransferable trf = getClip().getContents();
        if (trf == null) {
            System.out.println("No transferable found on clipboard.");
            return null;
        }
        try {
            DataFlavor df = findFlavor(trf, mimeStr);
            if (df != null) {
                return trf.getTransferData(df);
            } else {
                System.out.println("Mime flavor \"" + mimeStr + "\" not found on transferable.");
            }
        } catch (Exception e) {
            System.out.println("Could not read clipboard data: " + e.getMessage());
        }
        return null;
    }

    public static DataFlavor findFlavor(XTransferable trf, String mimeStr) {
        DataFlavor[] dfs = trf.getTransferDataFlavors();
        for (DataFlavor df : dfs) {
            if (df.MimeType.startsWith(mimeStr)) {
                return df;
            }
        }
        return null;
    }

    public static void listFlavors() {
        XTransferable trf = getClip().getContents();
        if (trf == null) {
            System.out.println("No transferable found.");
            return;
        }
        DataFlavor[] dfs = trf.getTransferDataFlavors();
        System.out.println("No. of flavors: " + dfs.length);
        for (int i = 0; i < dfs.length; i++) {
            System.out.println((i + 1) + ". " + dfs[i].MimeType);
        }
    }
}
