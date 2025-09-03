package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class FactureIdGeneratorService extends AbstractIdGeneratorService {


    public FactureIdGeneratorService(EntityManager entityManager) {

        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_facture_seq";
    }


}
