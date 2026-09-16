package com.clinic.pms.repository;

import com.clinic.pms.entity.DoctorCredential;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class DoctorCredentialRepository {

    private final EntityManager em;

    public DoctorCredentialRepository(EntityManager em) {
        this.em = em;
    }

    public long count() {
        return em.createQuery("SELECT COUNT(c) FROM DoctorCredential c", Long.class).getSingleResult();
    }

    public Optional<DoctorCredential> findByUsername(String username) {
        List<DoctorCredential> results = em.createQuery(
                        "SELECT c FROM DoctorCredential c WHERE c.username = :username", DoctorCredential.class)
                .setParameter("username", username)
                .getResultList();
        return results.stream().findFirst();
    }

    public DoctorCredential save(DoctorCredential credential) {
        em.getTransaction().begin();
        DoctorCredential saved;
        if (credential.getId() == null) {
            em.persist(credential);
            saved = credential;
        } else {
            saved = em.merge(credential);
        }
        em.getTransaction().commit();
        return saved;
    }
}
