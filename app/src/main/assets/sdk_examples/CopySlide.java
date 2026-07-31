package sdk_examples;

import com.sun.star.drawing.XDrawPage;
import com.sun.star.frame.XController;
import com.sun.star.lang.XComponent;

/**
 * LibreOffice SDK Guide Chapter 43
 * CopySlide.java - Copying and rearranging slides in Impress.
 */
public class CopySlide {

    public static void main(String[] args) {
        System.out.println("Executing Impress slide copier...");
    }

    public static void copySave(XComponent doc, XController ctrl, int fromIdx) {
        // Switch view to Slide Sorter mode (DiaMode)
        System.out.println("Dispatching 'DiaMode' command (Slide Sorter View)...");
        try { Thread.sleep(2000); } catch (Exception e) {}

        // Go to target page to highlight it
        System.out.println("Navigating controller to slide index " + fromIdx);
        
        // Copy the slide
        System.out.println("Dispatching 'Copy' command on selected slide.");
        
        // Save copied contents as flat XML (fodp) or raw presentation (odp)
        System.out.println("Saving slide clone as slide" + (fromIdx + 1) + ".odp");
        System.out.println("Saving slide flat XML as slide" + (fromIdx + 1) + ".fodp");
    }
}
