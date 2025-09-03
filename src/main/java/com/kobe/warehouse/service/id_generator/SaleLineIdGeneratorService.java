package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class SaleLineIdGeneratorService extends AbstractIdGeneratorService {


    public SaleLineIdGeneratorService(EntityManager entityManager) {

        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_sale_item_seq";
    }


}
