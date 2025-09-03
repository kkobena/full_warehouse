package com.kobe.warehouse.service.id_generator;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class MvtProduitIdGeneratorService extends AbstractIdGeneratorService {


    public MvtProduitIdGeneratorService(EntityManager entityManager) {

        super(entityManager);
    }

    @Override
    public String getSequenceName() {
        return "id_mvt_produit_seq";
    }


}
