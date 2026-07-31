package sdk_examples;

import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.lang.XComponent;

/**
 * LibreOffice SDK Guide Chapter 51
 * DocInfo.java - Query and extract metadata details about an ODF document via XDocumentProperties.
 */
public class DocInfo {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: run DocInfo <fnm>");
            return;
        }

        System.out.println("Opening ODF document: " + args[0]);
        // Simulate loading document
        Object doc = new Object(); // Mock document component
        
        System.out.println("\nDocument Properties Info:");
        printDocProperties(doc);
    }

    public static void printDocProperties(Object doc) {
        // Retrieve document properties supplier
        XDocumentPropertiesSupplier docPropsSupp = (XDocumentPropertiesSupplier) doc; // In real code: Lo.qi(...)
        XDocumentProperties dps = docPropsSupp.getDocumentProperties();
        
        System.out.println("  Author: " + dps.getAuthor());
        System.out.println("  Title: " + dps.getTitle());
        System.out.println("  Subject: " + dps.getSubject());
        System.out.println("  Description: " + dps.getDescription());
        System.out.println("  Generator: " + dps.getGenerator());
        System.out.println("  Modification Date: " + dps.getModificationDate());
        
        // Retrieve custom user defined properties (e.g. "Secret" attribute)
        XPropertyContainer udProps = dps.getUserDefinedProperties();
        System.out.println("  Secret == " + udProps.getPropertyValue("Secret"));
    }
}
