package com.kobe.warehouse.service.rupture.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;

public interface RuptureService {
    void createRupture(Produit produit, Fournisseur fournisseur, int qty);
}
