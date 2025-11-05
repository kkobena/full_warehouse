package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.stock.GestionStockDepotService;
import com.kobe.warehouse.service.stock.ProduitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GestionStockDepotImpl implements GestionStockDepotService {

    private final ProduitService produitService;

    public GestionStockDepotImpl(ProduitService produitService) {
        this.produitService = produitService;
    }

    @Override
    public Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) {
        return produitService.findAll(produitCriteria, pageable);
    }
}
