package com.clinic.pms.service;

import com.clinic.pms.entity.Patient;
import com.clinic.pms.exception.NotFoundException;
import com.clinic.pms.repository.MedicalHistoryRepository;
import com.clinic.pms.repository.PatientRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PatientService {

    private final PatientRepository patientRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;

    public PatientService(PatientRepository patientRepository,
                          MedicalHistoryRepository medicalHistoryRepository) {
        this.patientRepository = patientRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
    }

    public Patient create(PatientData data) {
        PatientValidator.validateForCreate(data);
        Patient patient = new Patient();
        apply(patient, data);
        LocalDateTime now = LocalDateTime.now();
        patient.setCreatedAt(now);
        patient.setUpdatedAt(now);
        return patientRepository.save(patient);
    }

    public Patient getById(long id) {
        return patientRepository.findById(id).orElseThrow(() -> new NotFoundException("Patient not found"));
    }

    public Patient update(long id, PatientData data) {
        Patient patient = getById(id);
        PatientValidator.validateForUpdate(data, patient.getGender());
        apply(patient, data);
        patient.setUpdatedAt(LocalDateTime.now());
        return patientRepository.save(patient);
    }

    public List<Patient> search(String query, String phone, LocalDate fromDate, LocalDate toDate) {
        List<Patient> patients = patientRepository.search(query, phone, fromDate, toDate);
        Map<Long, LocalDate> lastAppointments = medicalHistoryRepository.findLastEntryDates(
                patients.stream().map(Patient::getId).toList());
        patients.forEach(p -> p.setLastAppointmentDate(lastAppointments.get(p.getId())));
        return patients;
    }

    /** Soft delete only — never physically removes the patient or its data (FR-020). */
    public void softDelete(long id) {
        Patient patient = getById(id);
        patient.setDeleted(true);
        patient.setDeletedAt(LocalDateTime.now());
        patientRepository.save(patient);
    }

    public List<Patient> listDeleted() {
        return patientRepository.findDeleted();
    }

    /** Restores a soft-deleted patient to full visibility (FR-020a). */
    public Patient restore(long id) {
        Patient patient = getById(id);
        if (!patient.isDeleted()) {
            throw new NotFoundException("Patient is not currently deleted");
        }
        patient.setDeleted(false);
        patient.setDeletedAt(null);
        return patientRepository.save(patient);
    }

    private void apply(Patient patient, PatientData data) {
        patient.setFullName(data.fullName());
        patient.setDateOfBirth(data.dateOfBirth());
        patient.setGender(data.gender());
        patient.setAddress(data.address());
        patient.setEmergencyContact(data.emergencyContact());
        patient.setPhoneNumber(data.phoneNumber());
        patient.setEmail(data.email());
    }

    public record PatientData(
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String emergencyContact,
            String phoneNumber,
            String email) {
    }
}
