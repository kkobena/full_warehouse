package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class CaPonctionIdGeneratorService extends AbstractIdGeneratorService {

    public CaPonctionIdGeneratorService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_ca_ponction_seq";
    }
}
