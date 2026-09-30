package com.kobe.warehouse.service.customer;

import java.time.LocalDate;
import java.util.List;

/**
 * Un achat de l'attestation.
 *
 * @param beneficiaire l'ayant droit s'il y en a un, sinon le client
 * @param montant      montant de la vente, remise déduite
 */
public record DepenseDTO(
    LocalDate date,
    String reference,
    String beneficiaire,
    List<ProduitDepenseDTO> produits,
    long montant,
    long partTiersPayant,
    long partClient
) {
    public record ProduitDepenseDTO(String libelle, int quantite, long montant) {}
}
