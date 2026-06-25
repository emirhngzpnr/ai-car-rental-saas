package com.aicarrental.common.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNumberValidatorTests {
    private final PhoneNumberValidator validator = new PhoneNumberValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "+90 (555) 123-45-67",
            "0555 123 45 67",
            "5551234567",
            "(212) 555-1234"
    })
    void acceptsSupportedPhoneFormats(String phoneNumber) {
        assertTrue(validator.isValid(phoneNumber, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456",
            "phone-number",
            "555.123.4567",
            "++905551234567",
            "+90 555 123 45 67 9999",
            "1234567890123456"
    })
    void rejectsInvalidPhoneFormats(String phoneNumber) {
        assertFalse(validator.isValid(phoneNumber, null));
    }
}
