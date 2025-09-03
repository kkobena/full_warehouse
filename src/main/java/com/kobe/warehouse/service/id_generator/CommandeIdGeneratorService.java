package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class CommandeIdGeneratorService extends AbstractIdGeneratorService {


    public CommandeIdGeneratorService(EntityManager entityManager) {

        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_commande_seq";
    }


}
