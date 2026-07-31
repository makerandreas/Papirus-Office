package sdk_examples;

import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.text.Paragraph;

/**
 * LibreOffice SDK Guide Chapter 51
 * CombineTexts.java - Merge the contents of doc2.odt into doc1.odt with page breaks.
 */
public class CombineTexts {
    public static void main(String[] args) {
        try {
            System.out.println("Loading text documents doc1.odt and doc2.odt...");
            TextDocument doc1 = TextDocument.loadDocument("doc1.odt");
            TextDocument doc2 = TextDocument.loadDocument("doc2.odt");

            System.out.println("Appending a page break to doc1...");
            doc1.addPageBreak();
            
            // Get last paragraph to anchor insert position
            Paragraph lastPara = doc1.getParagraphByReverseIndex(0, false);

            System.out.println("Concatenating elements from doc2 after the page break...");
            doc1.insertContentFromDocumentAfter(doc2, lastPara, true);

            System.out.println("Saving merged document to combined.odt...");
            doc1.save("combined.odt");
            doc1.close();
            doc2.close();
        } catch (Exception e) {
            System.out.println("Exception combining text files: " + e.getMessage());
        }
    }
}
