package sdk_examples;

import org.odftoolkit.simple.PresentationDocument;

/**
 * LibreOffice SDK Guide Chapter 51
 * CombineDecks.java - Combine slide decks deck1.odp and deck2.odp seamlessly.
 */
public class CombineDecks {
    public static void main(String[] args) {
        try {
            System.out.println("Loading slide deck presentation files deck1.odp and deck2.odp...");
            PresentationDocument doc1 = PresentationDocument.loadDocument("deck1.odp");
            PresentationDocument doc2 = PresentationDocument.loadDocument("deck2.odp");

            System.out.println("Appending deck2 slides to deck1 presentation container...");
            doc1.appendPresentation(doc2);

            System.out.println("Saving combined presentation to combined.odp...");
            doc1.save("combined.odp");
            doc1.close();
            doc2.close();
        } catch (Exception e) {
            System.out.println("Exception merging decks: " + e.getMessage());
        }
    }
}
