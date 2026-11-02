package com.expensesplitter;

import com.expensesplitter.util.ValidationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Contact Validation Tests - Email and E.164 International Phone Numbers")
public class NotificationValidationTest {

    @Test
    @DisplayName("Should accept valid RFC 5322 email formats")
    public void testValidEmailFormats() {
        List<String> validEmails = List.of(
                "alex@example.com",
                "john.doe@company.org",
                "support+finance@domain.co.in",
                "dev_user123@sub.domain.edu",
                "rahul.sharma@gmail.com"
        );
        for (String email : validEmails) {
            assertTrue(ValidationUtils.isValidEmail(email), "Expected valid email: " + email);
        }
    }

    @Test
    @DisplayName("Should reject invalid email formats")
    public void testInvalidEmailFormats() {
        List<String> invalidEmails = List.of(
                "",
                "   ",
                "plainaddress",
                "@missingusername.com",
                "username@.com",
                "username@com",
                "user name@domain.com",
                "user@domain..com"
        );
        for (String email : invalidEmails) {
            assertFalse(ValidationUtils.isValidEmail(email), "Expected invalid email: " + email);
        }
    }

    @Test
    @DisplayName("Should reject null email")
    public void testNullEmail() {
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    @DisplayName("Should accept valid international E.164 phone numbers with country code")
    public void testValidE164PhoneNumbers() {
        List<String> validPhones = List.of(
                "+919876543210", // India (+91)
                "+14155552671",  // USA (+1)
                "+447911123456", // UK (+44)
                "+491512345678", // Germany (+49)
                "+819012345678", // Japan (+81)
                "+61412345678"   // Australia (+61)
        );
        for (String phone : validPhones) {
            assertTrue(ValidationUtils.isValidPhoneNumber(phone), "Expected valid phone: " + phone);
        }
    }

    @Test
    @DisplayName("Should normalize phone numbers with whitespace, hyphens, and brackets")
    public void testPhoneNormalization() {
        List<String> rawPhones = List.of(
                "+91 98765 43210",
                "+1 (415) 555-2671",
                "+44-7911-123456"
        );
        for (String raw : rawPhones) {
            String normalized = ValidationUtils.normalizePhoneNumber(raw);
            assertTrue(ValidationUtils.isValidPhoneNumber(normalized), "Expected valid normalized phone for: " + raw);
        }
    }

    @Test
    @DisplayName("Should reject invalid phone numbers lacking valid country code or formatting")
    public void testInvalidPhoneNumbers() {
        List<String> invalidPhones = List.of(
                "",
                "   ",
                "9876543210",          // Missing '+' country code
                "09876543210",         // Leading zero without country code
                "+0123456789",         // Invalid country code starting with 0
                "+12",                 // Too short (< 7 digits subscriber)
                "+123456789012345678", // Too long (> 15 digits total)
                "phone-number",
                "+91-ABCD-1234"
        );
        for (String phone : invalidPhones) {
            assertFalse(ValidationUtils.isValidPhoneNumber(phone), "Expected invalid phone: " + phone);
        }
    }

    @Test
    @DisplayName("Should validate correct contact details tuple without exception")
    public void testValidateContactDetailsSuccess() {
        assertDoesNotThrow(() ->
                ValidationUtils.validateContactDetails("Rahul Sharma", "rahul@example.com", "+919876543210"));
        // Optional phone can be null or empty
        assertDoesNotThrow(() ->
                ValidationUtils.validateContactDetails("Aman Verma", "aman@example.com", null));
        assertDoesNotThrow(() ->
                ValidationUtils.validateContactDetails("Aman Verma", "aman@example.com", ""));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when contact name is blank")
    public void testValidateContactDetailsBlankName() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.validateContactDetails("   ", "valid@example.com", "+919876543210"));
        assertTrue(ex.getMessage().contains("Full name is required"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when contact email is invalid")
    public void testValidateContactDetailsInvalidEmail() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.validateContactDetails("Rahul Sharma", "invalid-email", "+919876543210"));
        assertTrue(ex.getMessage().contains("Invalid email address"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when phone number lacks country code")
    public void testValidateContactDetailsInvalidPhone() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.validateContactDetails("Rahul Sharma", "rahul@example.com", "9876543210"));
        assertTrue(ex.getMessage().contains("country code"));
    }
}
