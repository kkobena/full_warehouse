package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class AssuranceItemIdGeneratorService extends AbstractIdGeneratorService {

    public AssuranceItemIdGeneratorService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_sale_assurance_item_seq";
    }
}
