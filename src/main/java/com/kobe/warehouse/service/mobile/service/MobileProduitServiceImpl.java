package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@Transactional(readOnly = true)
public class MobileProduitServiceImpl implements MobileProduitService {
    private final ProduitService produitService;

    public MobileProduitServiceImpl(ProduitService produitService) {
        this.produitService = produitService;
    }

    @Override
    public List<ProduitDTO> searchProduits(String searchTerm) {
        List<ProduitDTO> produits = produitService.productsLiteList(
            new ProduitCriteria().setSearch(searchTerm), Pageable.unpaged());
        System.err.println("size of produits: " + produits.size());
        return produits;
       /* return  produitService.productsLiteList( new ProduitCriteria()
            .setSearch(searchTerm), Pageable.unpaged());*/
    }


}
