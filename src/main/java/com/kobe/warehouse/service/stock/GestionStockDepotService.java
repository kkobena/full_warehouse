package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GestionStockDepotService {
    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable);

    Page<DepotExtensionSaleDTO> getVenteDepot(
        PaymentStatus paymentStatus,
        Integer depotId,
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Integer userId,
        Pageable pageabl
    );
}
