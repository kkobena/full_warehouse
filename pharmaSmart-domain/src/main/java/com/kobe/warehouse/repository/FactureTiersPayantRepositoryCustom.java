package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.FacturationKpiRow;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface FactureTiersPayantRepositoryCustom {
    Page<FactureDto> fetchInvoices(Specification<FactureTiersPayant> specification, Pageable pageable);

    Page<FactureDto> fetchGroupedInvoices(Specification<FactureTiersPayant> specification, Pageable pageable);

    /**
     * Calcule les KPI de facturation sur une période donnée.
     *
     * <p>Règles de filtrage selon {@code typeFacture} :</p>
     * <ul>
     *   <li>{@link TypeFacture#INDIVIDUAL} — factures simples uniquement
     *       ({@code groupe_tiers_payant_id IS NULL} et {@code groupe_facture_tiers_payant_id IS NULL}).
     *       {@code organismeId} peut restreindre à un tiers-payant précis.</li>
     *   <li>{@link TypeFacture#GROUPED} — factures groupées parentes uniquement
     *       ({@code groupe_tiers_payant_id IS NOT NULL} et {@code groupe_facture_tiers_payant_id IS NULL}).
     *       {@code groupeId} peut restreindre à un groupe précis.</li>
     *   <li>{@link TypeFacture#ALL} — toutes les factures racines
     *       ({@code groupe_facture_tiers_payant_id IS NULL}, évite le double-comptage).
     *       {@code organismeId} et/ou {@code groupeId} sont applicables.</li>
     * </ul>
     *
     * @param fromDate             début de période (inclusif)
     * @param toDate               fin de période (inclusif)
     * @param organismeId          tiers-payant (optionnel, pertinent pour INDIVIDUAL et ALL)
     * @param groupeId             groupe de tiers-payants (optionnel, pertinent pour GROUPED et ALL)
     * @param typeFacture          type de facture à inclure dans le calcul
     * @param delaiReglementDefaut délai de règlement par défaut en jours (ex : 30)
     * @return KPI agrégés, ou {@link Optional#empty()} si aucune donnée
     */
    Optional<FacturationKpiRow> getKpiData(
        LocalDate fromDate,
        LocalDate toDate,
        Integer organismeId,
        Integer groupeId,
        TypeFacture typeFacture,
        int delaiReglementDefaut
    );
}
