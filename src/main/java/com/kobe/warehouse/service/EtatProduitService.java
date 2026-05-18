package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.EtatProduit;
import java.util.Collection;
import java.util.Set;

public interface EtatProduitService {
    EtatProduit getEtatProduit(Integer idProduit, int currentStock);

    EtatProduit getEtatProduit(Produit produit);

    boolean canSuggere(Integer idProduit);

    /**
     * Variante batch de {@link #canSuggere} : retourne, parmi les produits donnés, ceux qui
     * NE peuvent PAS être suggérés car ils ont déjà une commande REQUESTED ou RECEIVED dans la
     * période de rétention. Un seul appel SQL pour toute la liste.
     *
     * @param produitIds IDs des produits à évaluer
     * @return IDs des produits non suggérables
     */
    Set<Integer> produitsNonSuggerables(Collection<Integer> produitIds);
}
