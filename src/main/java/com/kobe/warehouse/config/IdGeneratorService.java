package com.kobe.warehouse.config;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class IdGeneratorService {

    private String sequenceName;
    private final EntityManager entityManager;

    public IdGeneratorService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long nextId() {
        return (
            (Number) entityManager
                .createNativeQuery("SELECT nextval(:seqName)")
                .setParameter("seqName", getSequenceName())
                .getSingleResult()
        ).longValue();
        //  return ((Number) entityManager.createNativeQuery("SELECT nextval('id_generator_seq')").getSingleResult()).longValue();
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getSequenceName() {
        return sequenceName;
    }
}
