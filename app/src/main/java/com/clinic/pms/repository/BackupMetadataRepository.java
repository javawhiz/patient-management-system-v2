package com.clinic.pms.repository;

import com.clinic.pms.entity.BackupMetadata;
import jakarta.persistence.EntityManager;
import java.util.Optional;

public class BackupMetadataRepository {

    private final EntityManager em;

    public BackupMetadataRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<BackupMetadata> find() {
        return Optional.ofNullable(em.find(BackupMetadata.class, 1L));
    }

    public void save(BackupMetadata metadata) {
        em.getTransaction().begin();
        em.merge(metadata);
        em.getTransaction().commit();
    }
}
