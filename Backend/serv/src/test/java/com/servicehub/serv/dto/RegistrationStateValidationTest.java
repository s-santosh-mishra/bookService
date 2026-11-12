package com.servicehub.serv.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationStateValidationTest {
    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void customerRegistrationAcceptsOnlyOdisha() {
        assertTrue(VALIDATOR.validateValue(RegisterReqDto.class, "state", "Odisha").isEmpty());
        assertFalse(VALIDATOR.validateValue(RegisterReqDto.class, "state", "Bihar").isEmpty());
    }

    @Test
    void workerRegistrationAcceptsOnlyOdisha() {
        assertTrue(VALIDATOR.validateValue(WorkerRegisterReqDto.class, "state", "Odisha").isEmpty());
        assertFalse(VALIDATOR.validateValue(WorkerRegisterReqDto.class, "state", "Bihar").isEmpty());
    }
}
