package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public abstract class AbstractIdGeneratorService {

    private final EntityManager entityManager;

    protected AbstractIdGeneratorService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected abstract String getSequenceName();

    public Number getNextId() {
        return (
            (Number) entityManager
                .createNativeQuery("SELECT nextval(:seqName)")
                .setParameter("seqName", getSequenceName())
                .getSingleResult()
        );
    }

    public long nextId() {
        return getNextId().longValue();
    }

    public int getNextIdAsInt() {
        return getNextId().intValue();
    }

    public String getNextIdAsString() {
        return getNextId().intValue() + "";
    }
}
