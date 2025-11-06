package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface GestionStockDepotService {
    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable);

    Page<SaleDTO> getVenteDepot(PaymentStatus paymentStatus,
                                Long depotId,
                                String search,
                                LocalDate fromDate,
                                LocalDate toDate,
                                String fromHour,
                                String toHour,
                                Boolean global,
                                Long userId,
                                Pageable pageable
    );
}
