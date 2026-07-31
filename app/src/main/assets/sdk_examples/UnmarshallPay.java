package sdk_examples;

import java.io.File;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

// Simulating Pay package import
import Pay.Payments;
import Pay.Payment;

/**
 * LibreOffice SDK Guide Chapter 50
 * UnmarshallPay.java - JAXB unmarshalling of pay.xml to typed Payments.
 */
public class UnmarshallPay {
    public static void main(String[] args) {
        try {
            // Initialize the JAXB context for the compiled target root class
            JAXBContext jaxbContext = JAXBContext.newInstance(Payments.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            
            // Unmarshall XML stream into Java entities
            Payments pays = (Payments) jaxbUnmarshaller.unmarshal(new File("pay.xml"));
            List<Payment> payList = pays.getPayment();

            System.out.println("Payments Unmarshalled successfully:");
            for (Payment p : payList) {
                System.out.println("  - " + p.getPurpose() + ": $" + p.getAmount() + " (Tax: " + p.getTax() + "%, Maturity: " + p.getMaturity() + ")");
            }
        } catch (Exception e) {
            System.err.println("JAXB Exception: " + e.getMessage());
        }
    }
}
