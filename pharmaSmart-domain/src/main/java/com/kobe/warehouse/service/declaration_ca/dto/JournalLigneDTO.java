package com.kobe.warehouse.service.declaration_ca.dto;

import java.time.LocalDate;

/**
 * Une ligne de vente sortie, en tout ou partie, du chiffre d'affaires à déclarer.
 *
 * <p>Sert les trois journaux : lignes d'unités gratuites, lignes de rayon exclu, et détail d'une
 * vente tiers-payant. Les champs sans objet pour un journal donné restent nuls — {@code rayon} pour
 * les unités gratuites, {@code quantiteUg} pour un rayon exclu — plutôt que d'imposer trois formes
 * de tableau à l'écran.
 *
 * @param valeurTtc montant réel de la ligne, celui qu'elle aurait pesé sans retraitement
 * @param montantExclu part retirée du chiffre d'affaires à déclarer : tout pour un rayon exclu ou un
 *     tiers-payant exclu, la seule valeur des unités gratuites sinon
 * @param marge marge brute de la ligne, calculée sur le montant <strong>réel</strong> : la
 *     marchandise a bien été vendue et payée, seul son affichage comptable change
 *
 * <p>Les montants sont des entiers et non des {@code long} : ils viennent des colonnes de
 * {@code sales_line}, qui sont elles-mêmes des entiers. Les cumuls, eux, sont portés par
 * {@link JournalKpiDTO}.
 */
public record JournalLigneDTO(
    Long saleId,
    LocalDate saleDate,
    String numeroTransaction,
    String codeProduit,
    String libelleProduit,
    String rayon,
    Integer quantite,
    Integer quantiteUg,
    Integer valeurTtc,
    Integer montantExclu,
    Integer marge
) {}
