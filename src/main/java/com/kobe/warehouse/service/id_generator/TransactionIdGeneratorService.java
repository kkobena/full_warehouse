package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class TransactionIdGeneratorService extends AbstractIdGeneratorService {

    public TransactionIdGeneratorService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_transaction_seq";
    }
}
