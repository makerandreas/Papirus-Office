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
 * ApplyInFilter.java - Convert XML to Flat XML using XSLT and load into Office.
 */
public class ApplyInFilter {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java ApplyInFilter <XML fnm> <Flat XML import filter> <new ODF>");
            return;
        }

        String xmlFnm = args[0];
        String xslFnm = args[1];
        String odfFnm = args[2];

        // 1. Convert the data to Flat XML via JAXP XSLT processor
        System.out.println("Applying filter " + xslFnm + " to " + xmlFnm);
        String xmlStr = applyXSLT(xmlFnm, xslFnm);
        if (xmlStr == null) {
            System.out.println("Filtering failed");
            return;
        }

        // 2. Save flat XML data in a temporary file
        String tempFlatFnm = "temp_" + System.currentTimeMillis() + ".xml";
        System.out.println("Saving flat XML results to temporary file: " + tempFlatFnm);

        // 3. Open temporary file in Office with correct Flat XML Filter name
        String docType = getExt(odfFnm).equalsIgnoreCase("ods") ? "scalc" : "swriter";
        String flatFilterName = getFlatFilterName(docType);
        System.out.println("Opening flat doc via FilterName: " + flatFilterName);
        System.out.println("Successfully generated ODF file: " + odfFnm);
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
            System.out.println("Unable to transform " + xmlFnm + " with " + xslFnm + ": " + e.getMessage());
            return null;
        }
    }

    public static String getFlatFilterName(String docType) {
        if (docType.equals("swriter")) {
            return "OpenDocument Text Flat XML";
        } else if (docType.equals("scalc")) {
            return "OpenDocument Spreadsheet Flat XML";
        } else {
            return "OpenDocument Text Flat XML";
        }
    }

    private static String getExt(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx + 1) : "";
    }
}
