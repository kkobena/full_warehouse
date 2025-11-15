package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class InvoiceGenerationCodeGeneratorService extends AbstractIdGeneratorService {

    public InvoiceGenerationCodeGeneratorService(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "invoice_generation_code_seq";
    }
}
