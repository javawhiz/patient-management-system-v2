package com.clinic.pms.repository;

import com.clinic.pms.entity.Patient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PatientRepository {

    private final EntityManager em;

    public PatientRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<Patient> findById(Long id) {
        return Optional.ofNullable(em.find(Patient.class, id));
    }

    /**
     * Partial-name/phone match with an optional date-range filter against the
     * patient's medical history entries (FR-003). Always excludes
     * soft-deleted patients.
     */
    public List<Patient> search(String query, String phone, LocalDate fromDate, LocalDate toDate) {
        StringBuilder jpql = new StringBuilder(
            "SELECT p FROM Patient p WHERE p.isDeleted = false");
        if (query != null && !query.isBlank()) {
            jpql.append(" AND LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%'))");
        }
        if (phone != null && !phone.isBlank()) {
            jpql.append(" AND p.phoneNumber LIKE CONCAT('%', :phone, '%')");
        }
        if (fromDate != null || toDate != null) {
            // Match patients with a visit in range, plus patients who have no visits at all so
            // newly-added patients are never hidden by the landing page's default range.
            StringBuilder inRange = new StringBuilder(
                "SELECT 1 FROM MedicalHistoryEntry h WHERE h.patientId = p.id AND h.deleted = false");
            if (fromDate != null) {
                inRange.append(" AND h.entryDate >= :fromDate");
            }
            if (toDate != null) {
                inRange.append(" AND h.entryDate <= :toDate");
            }
            jpql.append(" AND (EXISTS (").append(inRange).append(")")
                .append(" OR NOT EXISTS (SELECT 1 FROM MedicalHistoryEntry h2 "
                    + "WHERE h2.patientId = p.id AND h2.deleted = false))");
        }
        jpql.append(" ORDER BY p.fullName");

        TypedQuery<Patient> typedQuery = em.createQuery(jpql.toString(), Patient.class);
        if (query != null && !query.isBlank()) typedQuery.setParameter("query", query);
        if (phone != null && !phone.isBlank()) typedQuery.setParameter("phone", phone);
        if (fromDate != null) typedQuery.setParameter("fromDate", fromDate);
        if (toDate != null) typedQuery.setParameter("toDate", toDate);
        return typedQuery.getResultList();
    }

    public List<Patient> findDeleted() {
        return em.createQuery("SELECT p FROM Patient p WHERE p.isDeleted = true ORDER BY p.fullName", Patient.class)
                .getResultList();
    }

    public Patient save(Patient patient) {
        em.getTransaction().begin();
        Patient saved;
        if (patient.getId() == null) {
            em.persist(patient);
            saved = patient;
        } else {
            saved = em.merge(patient);
        }
        em.getTransaction().commit();
        return saved;
    }
}
