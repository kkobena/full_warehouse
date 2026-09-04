package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;

import java.time.LocalDate;
import java.util.Set;

/**
 * Filtres de recherche des factures tiers-payant.
 *
 * @param factureProvisoire {@code null} = aucun filtre (définitives ET provisoires), sinon on ne
 *                          garde que les factures dont le caractère provisoire vaut la valeur
 *                          donnée. Le tri-état vient du sélecteur « Définitives / Provisoires /
 *                          Toutes » de l'écran de facturation.
 * @param typeFacture       {@link TypeFacture#GROUPED} interroge les factures groupées parentes,
 *                          toute autre valeur les factures individuelles.
 */
public record InvoiceSearchParams(
    LocalDate startDate,
    LocalDate endDate,
    Set<Integer> groupIds,
    Set<Integer> tiersPayantIds,
    Boolean factureProvisoire,
    Set<InvoiceStatut> statuts,
    String search,
    TypeFacture typeFacture
) {}
