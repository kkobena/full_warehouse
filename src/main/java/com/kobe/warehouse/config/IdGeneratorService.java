package com.kobe.warehouse.config;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class IdGeneratorService {

  private final EntityManager entityManager;

    public IdGeneratorService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long nextId() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('id_generator_seq')").getSingleResult()).longValue();
        // Using UUID to generate a unique value and then taking the most significant bits.
        // We also ensure the value is positive.
       // return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }
}
