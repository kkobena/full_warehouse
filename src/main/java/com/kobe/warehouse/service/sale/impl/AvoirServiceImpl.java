package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.Avoir;
import com.kobe.warehouse.domain.LigneAvoir;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.AvoirRepository;
import com.kobe.warehouse.service.sale.AvoirService;
import org.springframework.stereotype.Service;

@Service
public class AvoirServiceImpl implements AvoirService {

    private final AvoirRepository avoirRepository;

    public AvoirServiceImpl(AvoirRepository avoirRepository) {
        this.avoirRepository = avoirRepository;
    }

    @Override
    public void save(Sales sales) {
        this.avoirRepository.save(build(sales));
    }

    private Avoir build(Sales sales) {
        Avoir avoir = new Avoir();
        avoir.setUser(sales.getUser());
        sales.getSalesLines().forEach(salesLine -> buildLigneAvoir(salesLine, avoir));
        sales.setAvoir(avoir);

        return avoir;
    }

    private void buildLigneAvoir(SalesLine salesLine, Avoir avoir) {
        LigneAvoir ligneAvoir = new LigneAvoir();
        ligneAvoir.setAvoir(avoir);
        ligneAvoir.setQuantite(salesLine.getQuantityAvoir());
        ligneAvoir.setProduit(salesLine.getProduit());
        avoir.getLigneAvoirs().add(ligneAvoir);
    }
}
