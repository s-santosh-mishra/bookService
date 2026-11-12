package com.servicehub.serv.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RegistrationRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void customerRegistrationRejectsMissingAndInvalidRequiredFields() {
        assertInvalid(RegisterReqDto.class, "fullName", null);
        assertInvalid(RegisterReqDto.class, "email", null);
        assertInvalid(RegisterReqDto.class, "email", "not-an-email");
        assertInvalid(RegisterReqDto.class, "age", null);
        assertInvalid(RegisterReqDto.class, "age", 17);
        assertInvalid(RegisterReqDto.class, "phone", "123");
        assertInvalid(RegisterReqDto.class, "password", "short");
        assertInvalid(RegisterReqDto.class, "termsAccepted", false);
    }

    @Test
    void workerRegistrationRejectsMissingAndInvalidRequiredFields() {
        assertInvalid(WorkerRegisterReqDto.class, "fullName", null);
        assertInvalid(WorkerRegisterReqDto.class, "email", "not-an-email");
        assertInvalid(WorkerRegisterReqDto.class, "age", 101);
        assertInvalid(WorkerRegisterReqDto.class, "phone", null);
        assertInvalid(WorkerRegisterReqDto.class, "bankAccountNumber", null);
        assertInvalid(WorkerRegisterReqDto.class, "bankIfscCode", "invalid");
        assertInvalid(WorkerRegisterReqDto.class, "experienceYears", -1);
        assertInvalid(WorkerRegisterReqDto.class, "serviceIds", List.of());
        assertInvalid(WorkerRegisterReqDto.class, "termsAccepted", false);
    }

    @Test
    void loginRejectsMissingOrMalformedCredentials() {
        assertInvalid(LoginReqDto.class, "email", null);
        assertInvalid(LoginReqDto.class, "email", "not-an-email");
        assertInvalid(LoginReqDto.class, "password", " ");
    }

    private void assertInvalid(Class<?> dtoType, String property, Object value) {
        assertFalse(
                VALIDATOR.validateValue(dtoType, property, value).isEmpty(),
                dtoType.getSimpleName() + "." + property + " should be invalid");
    }
}
