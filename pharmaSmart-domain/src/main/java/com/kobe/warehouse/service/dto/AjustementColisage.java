package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ContrainteColisage;

/**
 * Ce qu'une contrainte fournisseur a changé à une quantité saisie.
 *
 * <p>Le pharmacien tape 7, le colis est de 12, la ligne part à 12 : il doit savoir pourquoi, sans
 * quoi le logiciel paraît corriger au hasard. Ce report n'est produit que lorsque la quantité a
 * effectivement bougé.
 */
public record AjustementColisage(
        String produitLibelle,
        int quantiteDemandee,
        int quantiteRetenue,
        int qteColis,
        int qteMinimaleCommande,
        ContrainteColisage contrainte) {}
