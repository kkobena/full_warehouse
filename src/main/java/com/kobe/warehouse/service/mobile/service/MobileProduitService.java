package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.dto.ProduitDTO;
import java.util.List;

public interface MobileProduitService {
    List<ProduitDTO> searchProduits(String searchTerm);
}
