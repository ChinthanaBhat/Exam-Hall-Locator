package com.exam.locator;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.List;
import java.util.Properties;

public class EmailUtil {

    public static void sendAllocationEmails(List<Student> studentList, Properties mailConfig) {
        if (studentList == null || studentList.isEmpty()) {
            ProgressEndpoint.sendProgressUpdate("ERROR|No records found to notify.");
            return;
        }

        String senderEmail = cleanConfigValue(mailConfig.getProperty("mail.sender.email", ""));
        String appPassword = cleanConfigValue(mailConfig.getProperty("mail.sender.password", ""));
        String smtpHost = cleanConfigValue(mailConfig.getProperty("mail.smtp.host", "smtp.gmail.com"));
        String smtpPort = cleanConfigValue(mailConfig.getProperty("mail.smtp.port", "587"));

        if (senderEmail.isEmpty() || appPassword.isEmpty()) {
            ProgressEndpoint.sendProgressUpdate("ERROR|SMTP sender email or app password is missing.");
            return;
        }

        new Thread(() -> sendInBackground(studentList, senderEmail, appPassword, smtpHost, smtpPort),
                "allocation-email-sender").start();
    }

    private static void sendInBackground(
            List<Student> studentList,
            String senderEmail,
            String appPassword,
            String smtpHost,
            String smtpPort
    ) {
        System.out.println("Starting SMTP email broadcast.");

        for (Student student : studentList) {
            String emailStr = normalEmail(student);
            ProgressEndpoint.sendProgressUpdate("PENDING|" + student.getUsn() + "|" + student.getName() + "|" + emailStr);
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });

        int totalStudents = studentList.size();
        int successCount = 0;

        for (Student student : studentList) {
            String emailStr = normalEmail(student);

            if (isMissingSendData(student)) {
                ProgressEndpoint.sendProgressUpdate("FAILED|" + student.getUsn() + "|" + student.getName()
                        + "|" + emailStr + "|Missing allocation data or email address");
                continue;
            }

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(student.getEmail()));
                message.setSubject("IMPORTANT: Exam Hall Seating Allocation Details");
                message.setText(buildEmailBody(student));

                Transport.send(message);

                successCount++;
                ProgressEndpoint.sendProgressUpdate("SUCCESS|" + student.getUsn() + "|" + student.getName()
                        + "|" + emailStr + "|" + successCount + "/" + totalStudents);

                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("Delivery failure for USN: " + student.getUsn() + " -> " + e.getMessage());
                ProgressEndpoint.sendProgressUpdate("FAILED|" + student.getUsn() + "|" + student.getName()
                        + "|" + emailStr + "|" + e.getMessage());
            }
        }

        ProgressEndpoint.sendProgressUpdate("COMPLETE|Broadcast finished.");
    }

    private static boolean isMissingSendData(Student student) {
        return student.getAllocatedRoom() == null
                || student.getAllocatedRoom().contains("Not Allocated")
                || student.getEmail() == null
                || student.getEmail().trim().isEmpty()
                || "No Email".equalsIgnoreCase(student.getEmail().trim())
                || "No Email Provided".equalsIgnoreCase(student.getEmail().trim());
    }

    private static String normalEmail(Student student) {
        return student.getEmail() == null || student.getEmail().trim().isEmpty()
                ? "No Email"
                : student.getEmail().trim();
    }

    private static String buildEmailBody(Student student) {
        return "Dear " + student.getName() + ",\n\n"
                + "Your seating arrangement details are generated successfully:\n\n"
                + "USN: " + student.getUsn() + "\n"
                + "Subject: " + cleanText(student.getExamSubject()) + "\n"
                + "Exam Date: " + cleanText(student.getExamDate()) + "\n"
                + "Exam Time: " + cleanText(student.getExamTime()) + "\n"
                + "Allocated Room: " + student.getAllocatedRoom() + "\n"
                + "Bench Number: " + student.getSeatNumber() + "\n"
                + "Seat Position: " + student.getSeatPosition() + "\n\n"
                + "Best regards,\n"
                + "Exam Coordination Cell";
    }

    private static String cleanText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private static String cleanConfigValue(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }
}
