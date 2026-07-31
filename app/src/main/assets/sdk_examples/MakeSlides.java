package sdk_examples;

import java.net.URI;
import org.odftoolkit.simple.PresentationDocument;
import org.odftoolkit.simple.presentation.Slide;
import org.odftoolkit.simple.presentation.SlideLayout;
import org.odftoolkit.simple.draw.Textbox;
import org.odftoolkit.simple.draw.Image;
import org.odftoolkit.simple.draw.FrameRectangle;
import org.odftoolkit.simple.text.list.List;
import org.odftoolkit.simple.presentation.PresentationClass;

/**
 * LibreOffice SDK Guide Chapter 51
 * MakeSlides.java - Generate presentation slide decks containing titles, outlines, and figures.
 */
public class MakeSlides {
    public static void main(String[] args) {
        try {
            System.out.println("Instantiating new PresentationDocument...");
            PresentationDocument doc = PresentationDocument.newPresentationDocument();

            // Slide 1: Title Slide
            System.out.println("Generating Slide 1: Title");
            Slide slide1 = doc.newSlide(0, "slide1", SlideLayout.TITLE_ONLY);
            Textbox titleBox = slide1.getTextboxByUsage(PresentationClass.TITLE).get(0);
            titleBox.setTextContent("Important Slide Presentation");

            // Slide 2: Outline & Picture
            System.out.println("Generating Slide 2: Outline and Images");
            Slide slide2 = doc.newSlide(1, "slide2", SlideLayout.TITLE_OUTLINE);
            titleBox = slide2.getTextboxByUsage(PresentationClass.TITLE).get(0);
            titleBox.setTextContent("Overview");

            Textbox outline = slide2.getTextboxByUsage(PresentationClass.OUTLINE).get(0);
            List txtList = outline.addList();
            txtList.addItem("Item 1");
            txtList.addItem("Item 2");

            // Add an image to the slide and adjust positioning coordinates
            Image image = Image.newImage(slide2, new URI("skinner.png"));
            FrameRectangle rect = image.getRectangle();
            rect.setX(8.0); // Positions
            rect.setY(4.0);
            image.setRectangle(rect);

            System.out.println("Saving slide deck to makeSlides.odp...");
            doc.save("makeSlides.odp");
            doc.close();
        } catch (Exception e) {
            System.out.println("Exception building presentation deck: " + e.getMessage());
        }
    }
}
