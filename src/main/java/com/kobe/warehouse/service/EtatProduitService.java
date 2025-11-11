package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.EtatProduit;

public interface EtatProduitService {
    EtatProduit getEtatProduit(Integer idProduit, int currentStock);

    EtatProduit getEtatProduit(Produit produit);

    boolean canSuggere(Integer idProduit);
}
