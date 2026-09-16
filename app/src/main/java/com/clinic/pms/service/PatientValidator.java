package com.clinic.pms.service;

import com.clinic.pms.exception.BadRequestException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class PatientValidator {

    private static final Set<String> GENDERS = Set.of("Male", "Female", "Undisclosed");
    private static final Pattern PHONE_PATTERN = Pattern.compile("[0-9]{10}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private PatientValidator() {
    }

    public static Map<String, String> errorsForCreate(PatientService.PatientData data) {
        return errors(data, null, false);
    }

    public static Map<String, String> errorsForUpdate(PatientService.PatientData data, String existingGender) {
        return errors(data, existingGender, true);
    }

    static void validateForCreate(PatientService.PatientData data) {
        throwFirstError(errorsForCreate(data));
    }

    static void validateForUpdate(PatientService.PatientData data, String existingGender) {
        throwFirstError(errorsForUpdate(data, existingGender));
    }

    private static Map<String, String> errors(
            PatientService.PatientData data, String existingGender, boolean update) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (data == null) {
            errors.put("form", "Patient details are required");
            return errors;
        }
        if (data.fullName() == null || data.fullName().isBlank()) {
            errors.put("fullName", "Full name is required");
        }
        if (data.phoneNumber() == null || !PHONE_PATTERN.matcher(data.phoneNumber()).matches()) {
            errors.put("phone", "Phone number must contain exactly 10 digits");
        }
        if (data.email() != null && !data.email().isBlank()
                && !EMAIL_PATTERN.matcher(data.email()).matches()) {
            errors.put("email", "Enter a valid email address");
        }
        if (data.dateOfBirth() == null
                || !data.dateOfBirth().isBefore(LocalDate.now().minusYears(1))) {
            errors.put("dateOfBirth", "Date of birth must be more than one year ago");
        }
        if (!update || !isLegacyGender(existingGender) || !existingGender.equals(data.gender())) {
            if (!GENDERS.contains(data.gender())) {
                errors.put("gender", "Select Male, Female, or Undisclosed");
            }
        }
        return errors;
    }

    private static void throwFirstError(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new BadRequestException(errors.values().iterator().next());
        }
    }

    private static boolean isLegacyGender(String gender) {
        return gender != null && !GENDERS.contains(gender);
    }
}