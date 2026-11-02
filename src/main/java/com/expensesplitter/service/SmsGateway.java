package com.expensesplitter.service;

import com.expensesplitter.util.ValidationUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

public class SmsGateway {

    private final HttpClient httpClient;

    public SmsGateway() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends an SMS message to a mobile number with country code.
     * Integrates with Twilio or MSG91 gateways via secure REST APIs.
     * When credentials are not configured, runs safely in SIMULATION mode with detailed audit trace.
     */
    public DeliveryResult sendSms(String toPhoneNumber, String message) {
        String normalizedPhone = ValidationUtils.normalizePhoneNumber(toPhoneNumber);

        if (!ValidationUtils.isValidPhoneNumber(normalizedPhone)) {
            return DeliveryResult.failure("Phone number '" + toPhoneNumber + "' is invalid. Must include country code (e.g. +919876543210).");
        }

        boolean liveMode = NotificationConfig.isLiveMode();
        String provider = NotificationConfig.getOrDefault("SMS_GATEWAY_PROVIDER", "TWILIO").toUpperCase();

        if (liveMode) {
            if ("TWILIO".equalsIgnoreCase(provider) && NotificationConfig.isTwilioConfigured()) {
                return sendViaTwilio(normalizedPhone, message);
            } else if ("MSG91".equalsIgnoreCase(provider) && NotificationConfig.isMsg91Configured()) {
                return sendViaMsg91(normalizedPhone, message);
            }
        }

        // Safe Test/Simulation Mode
        String mockId = "SIM-SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("====================================================================");
        System.out.println(" 📱 [SMS DISPATCH - SIMULATION MODE]");
        System.out.println(" To Phone: " + normalizedPhone);
        System.out.println(" Provider: " + provider + " (Simulation)");
        System.out.println(" Status:   SIMULATED SUCCESS (ID: " + mockId + ")");
        System.out.println(" Content:  " + message);
        System.out.println("====================================================================");

        return DeliveryResult.success(mockId, true);
    }

    private DeliveryResult sendViaTwilio(String toPhone, String message) {
        try {
            String accountSid = NotificationConfig.get("TWILIO_ACCOUNT_SID");
            String authToken = NotificationConfig.get("TWILIO_AUTH_TOKEN");
            String fromPhone = NotificationConfig.get("TWILIO_PHONE_NUMBER");

            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            String formData = "To=" + URLEncoder.encode(toPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromPhone, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String authHeader = "Basic " + Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return DeliveryResult.success("TWILIO-" + response.statusCode(), false);
            } else {
                return DeliveryResult.failure("Twilio Gateway Error (HTTP " + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[SmsGateway Twilio Error] " + e.getMessage());
            return DeliveryResult.failure("Twilio Connection Exception: " + e.getMessage());
        }
    }

    private DeliveryResult sendViaMsg91(String toPhone, String message) {
        try {
            String authKey = NotificationConfig.get("MSG91_AUTH_KEY");
            String senderId = NotificationConfig.getOrDefault("MSG91_SENDER_ID", "EXPSPL");

            // Strip '+' for MSG91 format
            String plainPhone = toPhone.startsWith("+") ? toPhone.substring(1) : toPhone;

            String url = "https://api.msg91.com/api/v2/sendsms";
            String jsonPayload = String.format("""
                {
                  "sender": "%s",
                  "route": "4",
                  "country": "91",
                  "sms": [
                    {
                      "message": "%s",
                      "to": ["%s"]
                    }
                  ]
                }
                """, senderId, message.replace("\"", "\\\""), plainPhone);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("authkey", authKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return DeliveryResult.success("MSG91-" + response.statusCode(), false);
            } else {
                return DeliveryResult.failure("MSG91 Gateway Error (HTTP " + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[SmsGateway MSG91 Error] " + e.getMessage());
            return DeliveryResult.failure("MSG91 Connection Exception: " + e.getMessage());
        }
    }
}

