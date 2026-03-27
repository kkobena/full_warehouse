package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.dto.enumeration.Mois;
import java.time.LocalDateTime;
import java.util.Map;

public record SuggestionLineDTO(
    Integer id,
    int quantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String fournisseurProduitLibelle,
    String fournisseurProduitCip,
    String fournisseurProduitCodeEan,
    Integer produitId,
    Integer fournisseurProduitId,
    int currentStock,
    EtatProduit etatProduit,
    int prixAchat,
    int prixVente,
    Map<Mois, Integer> consommationMensuelle,
    String niveauUrgence,
    Integer joursRestants,
    String sourceCalcul,
    boolean quantiteModifieeManuel,
    //Nombre d'unités par colis (conditionnement fournisseur). 1 = pas de contrainte.
    Integer qteColis,
    //Quantité minimale de commande (en unités). 0 = pas de minimum.
    Integer qteMinimaleCommande
) {

}
