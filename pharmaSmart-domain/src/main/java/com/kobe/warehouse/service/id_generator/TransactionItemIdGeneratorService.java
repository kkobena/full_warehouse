package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class TransactionItemIdGeneratorService extends AbstractIdGeneratorService {

    public TransactionItemIdGeneratorService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_transaction_item_seq";
    }
}
