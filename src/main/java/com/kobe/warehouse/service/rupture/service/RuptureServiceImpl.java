package com.kobe.warehouse.service.rupture.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rupture;
import com.kobe.warehouse.repository.RuptureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RuptureServiceImpl implements RuptureService {

    private final RuptureRepository ruptureRepository;

    public RuptureServiceImpl(RuptureRepository ruptureRepository) {
        this.ruptureRepository = ruptureRepository;
    }

    @Override
    public void createRupture(Produit produit, Fournisseur fournisseur, int qty) {
        ruptureRepository.save(new Rupture().setProduit(produit).setFournisseur(fournisseur).setQty(qty));
    }
}
