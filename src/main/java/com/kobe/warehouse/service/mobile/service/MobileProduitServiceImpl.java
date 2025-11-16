package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.stock.ProduitService;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MobileProduitServiceImpl implements MobileProduitService {

    private final ProduitService produitService;

    public MobileProduitServiceImpl(ProduitService produitService) {
        this.produitService = produitService;
    }

    @Override
    public List<ProduitDTO> searchProduits(String searchTerm) {
        List<ProduitDTO> produits = produitService.findAll(new ProduitCriteria().setSearch(searchTerm), Pageable.unpaged()).getContent();
        return produits;
    }
}
