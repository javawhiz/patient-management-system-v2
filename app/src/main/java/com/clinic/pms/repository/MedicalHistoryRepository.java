package com.clinic.pms.repository;

import com.clinic.pms.entity.MedicalHistoryEntry;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MedicalHistoryRepository {

    private final EntityManager em;

    public MedicalHistoryRepository(EntityManager em) {
        this.em = em;
    }

    public List<MedicalHistoryEntry> findByPatientId(Long patientId) {
        return em.createQuery(
                "SELECT h FROM MedicalHistoryEntry h WHERE h.patientId = :patientId "
                    + "AND h.deleted = false ORDER BY h.entryDate DESC, h.id DESC",
                        MedicalHistoryEntry.class)
                .setParameter("patientId", patientId)
                .getResultList();
    }

        public Optional<MedicalHistoryEntry> findActiveByPatientAndId(Long patientId, Long entryId) {
        List<MedicalHistoryEntry> results = em.createQuery(
                "SELECT h FROM MedicalHistoryEntry h WHERE h.patientId = :patientId "
                    + "AND h.id = :entryId AND h.deleted = false",
                MedicalHistoryEntry.class)
            .setParameter("patientId", patientId)
            .setParameter("entryId", entryId)
            .getResultList();
        return results.stream().findFirst();
        }

    public List<MedicalHistoryEntry> findDeletedByPatientId(Long patientId) {
        return em.createQuery(
                "SELECT h FROM MedicalHistoryEntry h WHERE h.patientId = :patientId "
                    + "AND h.deleted = true ORDER BY h.deletedAt DESC",
                        MedicalHistoryEntry.class)
                .setParameter("patientId", patientId)
                .getResultList();
    }

    public Optional<MedicalHistoryEntry> findDeletedByPatientAndId(Long patientId, Long entryId) {
        List<MedicalHistoryEntry> results = em.createQuery(
                "SELECT h FROM MedicalHistoryEntry h WHERE h.patientId = :patientId "
                    + "AND h.id = :entryId AND h.deleted = true",
                MedicalHistoryEntry.class)
            .setParameter("patientId", patientId)
            .setParameter("entryId", entryId)
            .getResultList();
        return results.stream().findFirst();
    }

        public Optional<String> findCurrentPrescriptionText(Long patientId) {
        List<String> results = em.createQuery(
                "SELECT h.prescription FROM MedicalHistoryEntry h WHERE h.patientId = :patientId "
                    + "AND h.deleted = false AND h.prescription IS NOT NULL "
                    + "ORDER BY h.entryDate DESC, h.id DESC",
                String.class)
            .setParameter("patientId", patientId)
            .getResultList();
            return results.stream().filter(text -> !text.isBlank()).findFirst();
        }

    /** Most recent non-deleted entry date per patient — the patient's last appointment. */
    public Map<Long, LocalDate> findLastEntryDates(Collection<Long> patientIds) {
        Map<Long, LocalDate> lastDates = new HashMap<>();
        if (patientIds == null || patientIds.isEmpty()) {
            return lastDates;
        }
        List<Object[]> rows = em.createQuery(
                "SELECT h.patientId, MAX(h.entryDate) FROM MedicalHistoryEntry h "
                    + "WHERE h.deleted = false AND h.patientId IN :patientIds GROUP BY h.patientId",
                Object[].class)
            .setParameter("patientIds", patientIds)
            .getResultList();
        for (Object[] row : rows) {
            lastDates.put((Long) row[0], (LocalDate) row[1]);
        }
        return lastDates;
    }

    public MedicalHistoryEntry save(MedicalHistoryEntry entry) {
        em.getTransaction().begin();
        if (entry.getId() == null) {
            em.persist(entry);
        } else {
            entry = em.merge(entry);
        }
        em.getTransaction().commit();
        return entry;
    }
}
