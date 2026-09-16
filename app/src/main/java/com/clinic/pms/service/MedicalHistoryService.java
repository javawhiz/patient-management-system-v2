package com.clinic.pms.service;

import com.clinic.pms.entity.MedicalHistoryEntry;
import com.clinic.pms.exception.BadRequestException;
import com.clinic.pms.exception.NotFoundException;
import com.clinic.pms.repository.MedicalHistoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MedicalHistoryService {

    private final MedicalHistoryRepository repository;

    public MedicalHistoryService(MedicalHistoryRepository repository) {
        this.repository = repository;
    }

    public List<MedicalHistoryEntry> listForPatient(long patientId) {
        return repository.findByPatientId(patientId);
    }

    public MedicalHistoryEntry add(long patientId, LocalDate entryDate, String diagnosis, String prescription, String notes) {
        validate(entryDate, diagnosis);
        LocalDateTime now = LocalDateTime.now();
        MedicalHistoryEntry entry = new MedicalHistoryEntry();
        entry.setPatientId(patientId);
        entry.setEntryDate(entryDate);
        entry.setDiagnosis(diagnosis);
        entry.setPrescription(blankToNull(prescription));
        entry.setNotes(notes);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        return repository.save(entry);
    }

    public MedicalHistoryEntry getForEdit(long patientId, long entryId) {
        return repository.findActiveByPatientAndId(patientId, entryId)
                .orElseThrow(() -> new NotFoundException("History entry not found"));
    }

    public MedicalHistoryEntry update(long patientId, long entryId, LocalDate entryDate,
            String diagnosis, String prescription, String notes) {
        validate(entryDate, diagnosis);
        MedicalHistoryEntry entry = getForEdit(patientId, entryId);
        entry.setEntryDate(entryDate);
        entry.setDiagnosis(diagnosis);
        entry.setPrescription(blankToNull(prescription));
        entry.setNotes(notes);
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    public void softDelete(long patientId, long entryId) {
        MedicalHistoryEntry entry = getForEdit(patientId, entryId);
        entry.setDeleted(true);
        entry.setDeletedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        repository.save(entry);
    }

    public List<MedicalHistoryEntry> listDeletedForPatient(long patientId) {
        return repository.findDeletedByPatientId(patientId);
    }

    /** Restores a soft-deleted history entry to full visibility. */
    public MedicalHistoryEntry restore(long patientId, long entryId) {
        MedicalHistoryEntry entry = repository.findDeletedByPatientAndId(patientId, entryId)
                .orElseThrow(() -> new NotFoundException("Deleted history entry not found"));
        entry.setDeleted(false);
        entry.setDeletedAt(null);
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    public String getCurrentPrescriptionText(long patientId) {
        return repository.findCurrentPrescriptionText(patientId).orElse("");
    }

    private void validate(LocalDate entryDate, String diagnosis) {
        if (entryDate == null || diagnosis == null || diagnosis.isBlank()) {
            throw new BadRequestException("Date and diagnosis are required");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
