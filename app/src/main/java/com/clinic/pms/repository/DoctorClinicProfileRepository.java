package com.clinic.pms.repository;

import com.clinic.pms.entity.DoctorClinicProfile;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class DoctorClinicProfileRepository {

    private final EntityManager em;

    public DoctorClinicProfileRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<DoctorClinicProfile> findFirst() {
        List<DoctorClinicProfile> results = em.createQuery(
                        "SELECT p FROM DoctorClinicProfile p", DoctorClinicProfile.class)
                .setMaxResults(1)
                .getResultList();
        return results.stream().findFirst();
    }

    public DoctorClinicProfile save(DoctorClinicProfile profile) {
        em.getTransaction().begin();
        DoctorClinicProfile saved;
        if (profile.getId() == null) {
            em.persist(profile);
            saved = profile;
        } else {
            saved = em.merge(profile);
        }
        em.getTransaction().commit();
        return saved;
    }
}
