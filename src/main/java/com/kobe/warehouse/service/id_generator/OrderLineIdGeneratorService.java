package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class OrderLineIdGeneratorService extends AbstractIdGeneratorService {

    public OrderLineIdGeneratorService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_order_line_seq";
    }
}
