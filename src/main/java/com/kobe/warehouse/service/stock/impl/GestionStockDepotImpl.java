package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.service.stock.GestionStockDepotService;
import com.kobe.warehouse.service.stock.ProduitService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GestionStockDepotImpl implements GestionStockDepotService {

    private final ProduitService produitService;
    private final SaleDataService saleDataService;

    public GestionStockDepotImpl(ProduitService produitService, SaleDataService saleDataService) {
        this.produitService = produitService;
        this.saleDataService = saleDataService;
    }

    @Override
    public Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) {
        return produitService.findAll(produitCriteria.setDepot(true), pageable);
    }

    @Override
    public Page<DepotExtensionSaleDTO> getVenteDepot(
        PaymentStatus paymentStatus,
        Integer depotId,
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Integer userId,
        Pageable pageable
    ) {
        return saleDataService.fetchVenteDepot(search, fromDate, toDate, userId, paymentStatus, depotId, pageable);
    }
}
