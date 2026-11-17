package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.ClientsEnAttenteDTO;
import com.kobe.warehouse.service.dto.ProduitDuNonCommandeDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ce que l'officine doit à ses clients, vu depuis la commande. */
public interface ProduitDuService {
    /** Produits promis qu'aucune commande partie chez le grossiste ne couvre, du plus ancien au plus récent. */
    List<ProduitDuNonCommandeDTO> lireProduitsDusNonCommandes();

    /** Clients en attente des produits affichés, indexés par identifiant de produit. */
    Map<Integer, ClientsEnAttenteDTO> lireClientsEnAttente(Set<Integer> produitIds);
}
