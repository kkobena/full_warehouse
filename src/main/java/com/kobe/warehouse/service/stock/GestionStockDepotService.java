package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.excel.model.ExportFormat;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GestionStockDepotService {
    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable);

    Page<DepotExtensionSaleDTO> getVenteDepot(
        PaymentStatus paymentStatus,
        Long depotId,
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Long userId,
        Pageable pageabl
    );

    void export(HttpServletResponse response, ExportFormat type, SaleId saleId) throws IOException;
}
