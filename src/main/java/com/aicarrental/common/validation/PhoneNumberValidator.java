package com.aicarrental.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    static final int MIN_LENGTH = 7;
    static final int MAX_LENGTH = 20;
    static final int MIN_DIGITS = 7;
    static final int MAX_DIGITS = 15;

    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("\\+?[0-9() -]+");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String phoneNumber = value.trim();
        if (phoneNumber.length() < MIN_LENGTH || phoneNumber.length() > MAX_LENGTH) {
            return false;
        }

        if (!ALLOWED_CHARACTERS.matcher(phoneNumber).matches()) {
            return false;
        }

        long digitCount = phoneNumber.chars().filter(Character::isDigit).count();
        return digitCount >= MIN_DIGITS && digitCount <= MAX_DIGITS;
    }
}
