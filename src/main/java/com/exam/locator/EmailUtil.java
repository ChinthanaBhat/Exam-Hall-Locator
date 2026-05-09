package com.exam.locator;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailUtil {
    public static void sendEmail(String recipient, String studentName, String hall, int bench) {
        // 1. Credentials
        final String senderEmail = "your.real.email@gmail.com";
        final String appPassword = "YOUR_APP_PASSWORD_HERE";

        // 2. SMTP Server Properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // 3. Create Session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });

        try {
            // 4. Compose Message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("Exam Seat Confirmed: " + studentName);

            String content = "Hello " + studentName + ",\n\n"
                    + "Your exam hall allocation is complete.\n"
                    + "Hall: " + hall + "\n"
                    + "Bench Number: " + bench + "\n\n"
                    + "Please reach the hall 15 minutes early. Good luck!";

            message.setText(content);

            // 5. Send
            Transport.send(message);
            System.out.println("Email successfully sent to: " + recipient);

        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
