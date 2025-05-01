package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.EtatProduit;

public interface EtatProduitService {
    EtatProduit getEtatProduit(Long idProduit, int currentStock);

    EtatProduit getEtatProduit(Produit produit);

    boolean canSuggere(Long idProduit);
}
