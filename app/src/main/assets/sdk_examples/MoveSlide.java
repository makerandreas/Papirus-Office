package sdk_examples;

import org.odftoolkit.simple.PresentationDocument;

/**
 * LibreOffice SDK Guide Chapter 51
 * MoveSlide.java - Move first slide of a presentation deck to the end of the deck.
 */
public class MoveSlide {
    public static void main(String[] args) {
        try {
            System.out.println("Loading presentation document: algs.odp");
            PresentationDocument doc = PresentationDocument.loadDocument("algs.odp");
            
            int numSlides = doc.getSlideCount();
            System.out.println("Total slide count: " + numSlides);
            System.out.println("Moving slide index 0 (first slide) to position: " + numSlides);
            
            doc.moveSlide(0, numSlides);

            System.out.println("Saving modified presentation to algsMoved.odp...");
            doc.save("algsMoved.odp");
            doc.close();
        } catch (Exception e) {
            System.out.println("Exception moving slide: " + e.getMessage());
        }
    }
}
