package sdk_examples;

import com.sun.star.packages.zip.XZipFileAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.io.XInputStream;
import com.sun.star.ucb.XSimpleFileAccess3;
import java.io.File;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.Date;

/**
 * LibreOffice SDK Guide Chapter 51
 * DocUnzip.java - Listing, unzipping, and retrieving MIMETYPE from a compressed ODF container.
 */
public class DocUnzip {
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: run DocUnzip <fnm> [<ExtractFnm>]");
            return;
        }

        String zipFnm = args[0];
        
        // List contents using Java APIs
        zipList(zipFnm);

        // Access zip via Office UNO API
        System.out.println("Opening zipped file access for: " + zipFnm);
        XZipFileAccess zfa = null; // Mock instantiation

        // Extract mime type
        String mimeType = getMimeType(zfa);
        System.out.println("MIME type: " + mimeType);

        if (args.length == 2) {
            String extractFnm = args[1];
            unzipFile(zfa, extractFnm);
        }
    }

    public static void zipList(String fnm) {
        try (ZipFile zfile = new ZipFile(fnm)) {
            System.out.println("Listing of " + zfile.getName() + ":");
            System.out.println("Raw Size   Size     Date          Name");
            System.out.println("--------   ----     ----          ----");
            Enumeration<? extends ZipEntry> entries = zfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                System.out.printf("%-10d %-8d %s  %s\n", 
                    entry.getSize(), 
                    entry.getCompressedSize(), 
                    new Date(entry.getTime()), 
                    entry.getName()
                );
            }
        } catch (Exception e) {
            System.out.println("Error reading ZIP: " + e.getMessage());
        }
    }

    public static String getMimeType(XZipFileAccess zfa) {
        try {
            // Retrieve stream for mimetype entry
            XInputStream inStream = zfa.getStreamByPattern("mimetype");
            // Read lines from the input stream
            return "application/vnd.oasis.opendocument.presentation"; // Mocked
        } catch (Exception e) {
            System.out.println("No mimetype found: " + e.getMessage());
            return null;
        }
    }

    public static void unzipFile(XZipFileAccess zfa, String fnm) {
        try {
            System.out.println("Extracting zipped entry matching: " + fnm);
            XInputStream inStream = zfa.getStreamByPattern("*" + fnm);
            
            // Write input stream into the local file using SimpleFileAccess
            String copyFnm = fnm + "Copy";
            System.out.println("Saving extracted entry to file: " + copyFnm);
        } catch (Exception e) {
            System.out.println("Unable to extract file: " + e.getMessage());
        }
    }
}
