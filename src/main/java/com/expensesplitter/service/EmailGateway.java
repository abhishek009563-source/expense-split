package com.expensesplitter.service;

import com.expensesplitter.util.ValidationUtils;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class EmailGateway {

    /**
     * Sends an email to the specified recipient.
     * Uses real SMTP if credentials are configured in environment or settings.
     * Otherwise runs in safe, clearly-labelled SIMULATION/TEST mode.
     */
    public DeliveryResult sendEmail(String toEmail, String recipientName, String subject, String bodyText, String bodyHtml) {
        if (!ValidationUtils.isValidEmail(toEmail)) {
            return DeliveryResult.failure("Invalid recipient email address: '" + toEmail + "'");
        }

        boolean liveMode = NotificationConfig.isLiveMode();
        boolean smtpConfigured = NotificationConfig.isSmtpConfigured();

        if (liveMode && smtpConfigured) {
            try {
                return sendViaSmtp(toEmail, recipientName, subject, bodyText, bodyHtml);
            } catch (Exception e) {
                System.err.println("[EmailGateway Error] Failed to send live email to " + toEmail + ": " + e.getMessage());
                return DeliveryResult.failure("SMTP Error: " + e.getMessage());
            }
        }

        // Safe Test/Simulation Mode
        String mockId = "SIM-EML-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("====================================================================");
        System.out.println(" 📧 [EMAIL DISPATCH - SIMULATION MODE]");
        System.out.println(" To:      " + recipientName + " <" + toEmail + ">");
        System.out.println(" Subject: " + subject);
        System.out.println(" Status:  SIMULATED SUCCESS (ID: " + mockId + ")");
        System.out.println(" Body:\n" + bodyText);
        System.out.println("====================================================================");

        return DeliveryResult.success(mockId, true);
    }

    private DeliveryResult sendViaSmtp(String toEmail, String recipientName, String subject, String bodyText, String bodyHtml) throws Exception {
        String host = NotificationConfig.get("SMTP_HOST");
        int port = Integer.parseInt(NotificationConfig.getOrDefault("SMTP_PORT", "587"));
        String username = NotificationConfig.get("SMTP_USERNAME");
        String password = NotificationConfig.get("SMTP_PASSWORD");
        String fromEmail = NotificationConfig.getOrDefault("SMTP_FROM_EMAIL", username);

        Socket socket;
        if (port == 465) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = factory.createSocket(host, port);
        } else {
            socket = new Socket(host, port);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            readResponse(reader, "220");
            sendLine(writer, "EHLO " + host);
            readEhloResponse(reader);

            if (port == 587) {
                sendLine(writer, "STARTTLS");
                readResponse(reader, "220");
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, host, port, true);
                sslSocket.startHandshake();

                BufferedReader sslReader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter sslWriter = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8));

                sendLine(sslWriter, "EHLO " + host);
                readEhloResponse(sslReader);

                // Authenticate
                sendLine(sslWriter, "AUTH LOGIN");
                readResponse(sslReader, "334");
                sendLine(sslWriter, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
                readResponse(sslReader, "334");
                sendLine(sslWriter, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
                readResponse(sslReader, "235");

                // Mail From & Rcpt To
                sendLine(sslWriter, "MAIL FROM:<" + fromEmail + ">");
                readResponse(sslReader, "250");
                sendLine(sslWriter, "RCPT TO:<" + toEmail + ">");
                readResponse(sslReader, "250");

                sendLine(sslWriter, "DATA");
                readResponse(sslReader, "354");

                // Payload
                sslWriter.write("From: Expense Splitter <" + fromEmail + ">\r\n");
                sslWriter.write("To: " + recipientName + " <" + toEmail + ">\r\n");
                sslWriter.write("Subject: " + subject + "\r\n");
                sslWriter.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                sslWriter.write(bodyText + "\r\n.\r\n");
                sslWriter.flush();
                readResponse(sslReader, "250");

                sendLine(sslWriter, "QUIT");
                return DeliveryResult.success("SMTP-" + UUID.randomUUID().toString().substring(0, 8), false);
            } else {
                sendLine(writer, "AUTH LOGIN");
                readResponse(reader, "334");
                sendLine(writer, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
                readResponse(reader, "334");
                sendLine(writer, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
                readResponse(reader, "235");

                sendLine(writer, "MAIL FROM:<" + fromEmail + ">");
                readResponse(reader, "250");
                sendLine(writer, "RCPT TO:<" + toEmail + ">");
                readResponse(reader, "250");
                sendLine(writer, "DATA");
                readResponse(reader, "354");

                writer.write("From: Expense Splitter <" + fromEmail + ">\r\n");
                writer.write("To: " + recipientName + " <" + toEmail + ">\r\n");
                writer.write("Subject: " + subject + "\r\n");
                writer.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                writer.write(bodyText + "\r\n.\r\n");
                writer.flush();
                readResponse(reader, "250");
                sendLine(writer, "QUIT");
                return DeliveryResult.success("SMTP-" + UUID.randomUUID().toString().substring(0, 8), false);
            }
        }
    }

    private void sendLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line + "\r\n");
        writer.flush();
    }

    private void readResponse(BufferedReader reader, String expectedCode) throws IOException {
        String line = reader.readLine();
        if (line == null || !line.startsWith(expectedCode)) {
            throw new IOException("Unexpected SMTP response: " + line + " (expected " + expectedCode + ")");
        }
    }

    private void readEhloResponse(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("250 ")) break; // last line
            if (!line.startsWith("250-")) throw new IOException("Bad EHLO line: " + line);
        }
    }
}

