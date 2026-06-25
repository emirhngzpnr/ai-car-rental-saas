package com.aicarrental.common.validation;

import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidEmailAddresses() {
        assertThat(validate("emirhan@gmail.com")).isEmpty();
        assertThat(validate("emirhan.guzel+booking@example.co.uk")).isEmpty();
    }

    @Test
    void rejectsMalformedOrNonAsciiEmailAddresses() {
        assertThat(validate("emirhançgmail.com")).isNotEmpty();
        assertThat(validate("emirhanç@gmail.com")).isNotEmpty();
        assertThat(validate("emirhan@gmail")).isNotEmpty();
        assertThat(validate("@gmail.com")).isNotEmpty();
    }

    private Set<ConstraintViolation<CustomerRegisterRequest>> validate(String email) {
        return validator.validate(new CustomerRegisterRequest(
                "Emirhan",
                "Test",
                email,
                "password123",
                "+90 555 123 45 67"
        ));
    }
}
