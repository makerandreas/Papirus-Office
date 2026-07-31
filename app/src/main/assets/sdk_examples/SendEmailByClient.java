/*
 * LibreOffice SDK Chapter 42 Example - Sending Email via SimpleSystemMail / SimpleCommandMail
 * Copyright (c) The Document Foundation / Papirus Office
 */

import com.sun.star.lang.XComponent;
import com.sun.star.system.XSimpleMailClientSupplier;
import com.sun.star.system.XSimpleMailClient;
import com.sun.star.system.XSimpleMailMessage;
import com.sun.star.system.XSimpleMailMessage2;
import com.sun.star.system.SimpleMailClientFlags;
import com.sun.star.uno.UnoRuntime;

public class SendEmailByClient {

    public static void sendEmailByClient(String to, String subject, String body, String attachmentPath) {
        System.out.println("Initiating email dispatch via system default client...");
        try {
            // Attempt to create Windows SimpleSystemMail supplier
            XSimpleMailClientSupplier mcSupp = Lo.createInstanceMCF(
                XSimpleMailClientSupplier.class,
                "com.sun.star.system.SimpleSystemMail"
            );

            // Fallback to SimpleCommandMail for Linux / macOS / Android
            if (mcSupp == null) {
                mcSupp = Lo.createInstanceMCF(
                    XSimpleMailClientSupplier.class,
                    "com.sun.star.system.SimpleCommandMail"
                );
            }

            if (mcSupp == null) {
                System.err.println("Error: Unable to instantiate mail client supplier.");
                return;
            }

            XSimpleMailClient mc = mcSupp.querySimpleMailClient();
            if (mc == null) {
                System.err.println("Error: No default email client found on system.");
                return;
            }

            XSimpleMailMessage msg = mc.createSimpleMailMessage();
            msg.setRecipient(to);
            msg.setSubject(subject);

            XSimpleMailMessage2 msg2 = UnoRuntime.queryInterface(XSimpleMailMessage2.class, msg);
            if (msg2 != null) {
                msg2.setBody(body);
            }

            if (attachmentPath != null && !attachmentPath.isEmpty()) {
                String[] attachments = new String[] { attachmentPath };
                msg.setAttachement(attachments);
            }

            // Send using NO_USER_INTERFACE flag (shows confirm dialog or passes to OS handler)
            mc.sendSimpleMailMessage(msg, SimpleMailClientFlags.NO_USER_INTERFACE);
            System.out.println("Email successfully dispatched to system spooler!");

        } catch (Exception e) {
            System.err.println("Exception sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        sendEmailByClient(
            "recipient@example.com",
            "Papirus Office Document Export",
            "Please find the requested document attached.",
            "/sdcard/Documents/Report.odt"
        );
    }
}
