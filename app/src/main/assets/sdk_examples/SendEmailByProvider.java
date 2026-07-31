/*
 * LibreOffice SDK Chapter 42 Example - Sending Email via MailServiceProvider (SMTP)
 * Copyright (c) The Document Foundation / Papirus Office
 */

import com.sun.star.mail.XMailServiceProvider;
import com.sun.star.mail.XMailService;
import com.sun.star.mail.XMailMessage;
import com.sun.star.mail.MailServiceType;
import com.sun.star.mail.MailAttachment;
import com.sun.star.mail.XSmtpService;
import com.sun.star.mail.MailMessage;
import com.sun.star.datatransfer.XTransferable;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XCurrentContext;
import com.sun.star.security.XAuthenticator;

public class SendEmailByProvider {

    public static void sendEmailBySMTP(
        String host, int port, String username, String password,
        String to, String subject, String body, String attachmentPath
    ) {
        System.out.println("Connecting to SMTP server " + host + ":" + port + "...");
        try {
            XMailServiceProvider msp = Lo.createInstanceMCF(
                XMailServiceProvider.class,
                "com.sun.star.mail.MailServiceProvider"
            );

            if (msp == null) {
                System.err.println("Could not create MailServiceProvider instance.");
                return;
            }

            XMailService service = msp.create(MailServiceType.SMTP);
            if (service == null) {
                System.err.println("Could not create SMTP MailService.");
                return;
            }

            // Connection context & authentication setup
            XCurrentContext context = new XCurrentContext() {
                public Object getValueByName(String name) {
                    if ("ServerName".equals(name)) return host;
                    if ("Port".equals(name)) return port;
                    if ("ConnectionType".equals(name)) return "Ssl"; // or "Insecure" / "STARTTLS"
                    if ("Timeout".equals(name)) return 60;
                    return null;
                }
            };

            XAuthenticator auth = new XAuthenticator() {
                public String getUserName() { return username; }
                public String getPassword() { return password; }
            };

            service.connect(context, auth);
            if (service.isConnected()) {
                System.out.println("Successfully connected to SMTP server.");

                String from = username;
                XMailMessage msg = MailMessage.create(
                    Lo.getContext(), to, from, subject, new TextTransferable(body)
                );

                if (attachmentPath != null && !attachmentPath.isEmpty()) {
                    FileTransferable ft = new FileTransferable(attachmentPath);
                    msg.addAttachment(new MailAttachment(ft, attachmentPath));
                }

                XSmtpService smtp = UnoRuntime.queryInterface(XSmtpService.class, service);
                smtp.sendMailMessage(msg);
                System.out.println("Email sent successfully!");

                service.disconnect();
            } else {
                System.err.println("Failed to establish SMTP connection.");
            }

        } catch (Exception e) {
            System.err.println("SMTP Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        sendEmailBySMTP(
            "smtp.gmail.com", 587,
            "user@gmail.com", "app_password",
            "recipient@example.com",
            "Financial Statement Q1",
            "Please find the spreadsheet attached.",
            "Financials.ods"
        );
    }
}
