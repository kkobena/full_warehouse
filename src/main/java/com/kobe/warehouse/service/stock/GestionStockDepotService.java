package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GestionStockDepotService {
    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable);
}
