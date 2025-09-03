package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public abstract class AbstractIdGeneratorService {

    private final EntityManager entityManager;
    protected abstract String getSequenceName();


    protected AbstractIdGeneratorService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long nextId() {
        return (
            (Number) entityManager
                .createNativeQuery("SELECT nextval(:seqName)")
                .setParameter("seqName", getSequenceName())
                .getSingleResult()
        ).longValue();

    }


}
