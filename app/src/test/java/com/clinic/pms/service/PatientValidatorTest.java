package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PatientValidatorTest {

    private static final LocalDate VALID_DOB = LocalDate.now().minusYears(1).minusDays(1);

    @Test
    void acceptsValidPatientDetails() {
        assertDoesNotThrow(() -> PatientValidator.validateForCreate(data("Male", "5551234567", "a@b.com", VALID_DOB)));
    }

    @Test
    void requiresExactlyTenDigitsForPhone() {
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForCreate(data("Male", "555123456", null, VALID_DOB)));
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForCreate(data("Male", "55512345678", null, VALID_DOB)));
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForCreate(data("Male", "555-123456", null, VALID_DOB)));
    }

    @Test
    void allowsBlankEmailButRejectsInvalidEmail() {
        assertDoesNotThrow(() -> PatientValidator.validateForCreate(data("Female", "5551234567", null, VALID_DOB)));
        assertDoesNotThrow(() -> PatientValidator.validateForCreate(data("Female", "5551234567", "", VALID_DOB)));
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForCreate(data("Female", "5551234567", "invalid", VALID_DOB)));
    }

    @Test
    void requiresBirthDateToBeMoreThanOneYearAgo() {
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForCreate(data("Undisclosed", "5551234567", null,
                        LocalDate.now().minusYears(1))));
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForCreate(data("Undisclosed", "5551234567", null,
                        LocalDate.now())));
        assertDoesNotThrow(() -> PatientValidator.validateForCreate(data("Undisclosed", "5551234567", null,
                VALID_DOB)));
    }

    @Test
    void restrictsGenderForNewPatients() {
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForCreate(data("Other", "5551234567", null, VALID_DOB)));
    }

    @Test
    void permitsUnchangedLegacyGenderOnUpdate() {
        var legacy = data("Other", "5551234567", null, VALID_DOB);
        assertDoesNotThrow(() -> PatientValidator.validateForUpdate(legacy, "Other"));
        assertThrows(RuntimeException.class,
                () -> PatientValidator.validateForUpdate(data("Other", "5551234567", null, VALID_DOB), "Male"));
    }

    private static PatientService.PatientData data(
            String gender, String phone, String email, LocalDate dateOfBirth) {
        return new PatientService.PatientData("Test Patient", dateOfBirth, gender,
                null, null, phone, email);
    }
}