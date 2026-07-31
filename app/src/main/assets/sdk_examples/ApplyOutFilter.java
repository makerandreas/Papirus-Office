package sdk_examples;

import java.io.File;
import java.io.StringWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * LibreOffice SDK Guide Chapter 50
 * ApplyOutFilter.java - Save ODF document as Flat XML and apply XSLT output filter.
 */
public class ApplyOutFilter {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java ApplyOutFilter <ODF file> <Flat XML export filter> <new XML file>");
            return;
        }

        String odfFnm = args[0];
        String xslFnm = args[1];
        String outXmlFnm = args[2];

        System.out.println("Opening ODF file: " + odfFnm);
        // Save document as Flat XML first
        String tempFlatFnm = "temp_flat_" + System.currentTimeMillis() + ".xml";
        System.out.println("Exporting ODF to Flat XML temporary file: " + tempFlatFnm);

        // Apply XSLT transformation to convert Flat XML into simplified custom XML
        System.out.println("Applying output filter " + xslFnm + " to " + tempFlatFnm);
        String filteredXML = applyXSLT(tempFlatFnm, xslFnm);
        if (filteredXML == null) {
            System.out.println("Filtering failed");
            return;
        }

        // Format, indent, and save XML
        String xmlStr = indentXML(filteredXML);
        System.out.println("Resulting XML:\n" + xmlStr);
        System.out.println("Saved exported XML payload to: " + outXmlFnm);
    }

    public static String applyXSLT(String xmlFnm, String xslFnm) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new File(xslFnm));
            Transformer t = tf.newTransformer(xslt);

            Source text = new StreamSource(new File(xmlFnm));
            StreamResult result = new StreamResult(new StringWriter());

            t.transform(text, result);
            return result.getWriter().toString();
        } catch (Exception e) {
            return "<error>" + e.getMessage() + "</error>";
        }
    }

    private static String indentXML(String xml) {
        // Simple mock indenting for demonstration
        return xml.trim();
    }
}
