package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.facturation.dto.DossierFactureDto;
import com.kobe.warehouse.service.facturation.dto.DossierFactureProjection;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FacturationDossier;
import com.kobe.warehouse.service.facturation.dto.FacturationGroupeDossier;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import com.kobe.warehouse.service.facturation.dto.FactureDtoWrapper;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.InvoiceSearchParams;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.dto.TiersPayantDossierFactureDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public interface EditionDataService {


    Page<DossierFactureDto> getSales(EditionSearchParams editionSearchParams, Pageable pageable);

    Page<TiersPayantDossierFactureDto> getEditionData(EditionSearchParams editionSearchParams, Pageable pageable);

    Page<FactureDto> getInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable);

    Page<FactureDto> getGroupInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable);

    void deleteFacture(Set<Long> ids);

    void deleteFacture(Long id);

    Optional<FactureDtoWrapper> getFacture(Long id);

    default String buildQuery(EditionSearchParams editionSearchParams, String query) {
        {
            query = query
                .concat(buildPeriod(editionSearchParams))
                .concat(" AND s.statut IN(")
                .concat(buildStatus(Set.of(SalesStatut.CLOSED)))
                .concat(") AND s.canceled_sale_id IS NULL AND s.canceled IS FALSE ");

            if (
                !CollectionUtils.isEmpty(editionSearchParams.tiersPayantIds()) && editionSearchParams.modeEdition() != ModeEditionEnum.GROUP
            ) {
                query = query.concat(" AND t.id IN (").concat(buildIds(editionSearchParams.tiersPayantIds())).concat(") ");
            }
            if (!CollectionUtils.isEmpty(editionSearchParams.groupIds()) && editionSearchParams.modeEdition() == ModeEditionEnum.GROUP) {
                query = query.concat(" AND t.groupe_tiers_payant_id IN (").concat(buildIds(editionSearchParams.groupIds())).concat(") ");
            }
            if (editionSearchParams.factureProvisoire()) {
                query = query.concat(" AND sl.facture_tiers_payant_id IS NULL ");
            } else {
                query = query.concat(" AND (sl.facture_tiers_payant_id IS NULL OR ftp.facture_provisoire IS TRUE) ");
            }
            if (!CollectionUtils.isEmpty(editionSearchParams.categorieTiersPayants())) {
                query = query
                    .concat(" AND t.categorie IN (")
                    .concat(buildCategorieTiersPayants(editionSearchParams.categorieTiersPayants()))
                    .concat(") ");
            }
        }
        return query;
    }

    default String buildCategorieTiersPayants(Set<TiersPayantCategorie> tiersPayantCategories) {
        return tiersPayantCategories.stream().map(e -> "'" + e.name() + "'").collect(Collectors.joining(","));
    }

    default String buildStatus(Set<SalesStatut> salesStatuts) {
        return salesStatuts.stream().map(e -> "'" + e.name() + "'").collect(Collectors.joining(","));
    }

    default String buildIds(Set<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    default String buildPeriod(EditionSearchParams editionSearchParams) {
        return " WHERE DATE(s.updated_at) BETWEEN ".concat("'" + editionSearchParams.startDate() + "'")
            .concat(" AND ")
            .concat("'" + editionSearchParams.endDate() + "'");
    }





    default String buildSearchQuery(InvoiceSearchParams invoiceSearchParams, String appendAnd, boolean isGroup) {
        String query = "";
        if (StringUtils.hasText(invoiceSearchParams.search())) {
            if (isGroup) {
                query = query
                    .concat(appendAnd)
                    .concat(" (f.num_facture LIKE '%")
                    .concat(invoiceSearchParams.search())
                    .concat("%' OR gtp.name LIKE '%")
                    .concat(invoiceSearchParams.search())
                    .concat("%' )");
            } else {
                query = query
                    .concat(appendAnd)
                    .concat(" (f.num_facture LIKE '%")
                    .concat(invoiceSearchParams.search())
                    .concat("%' OR tp.full_name LIKE '%")
                    .concat(invoiceSearchParams.search())
                    .concat("%' )");
            }
        }
        return query;
    }

    default String buildTiersPayantIds(Set<Long> tiersPayantIds) {
        if (CollectionUtils.isEmpty(tiersPayantIds)) {
            return "";
        }
        return " AND f.tiers_payant_id IN (".concat(buildIds(tiersPayantIds)).concat(") ");
    }



    default String buildSearchPeriod(InvoiceSearchParams invoiceSearchParams, String appendAnd, boolean isGroup) {
        String query = buildSearchQuery(invoiceSearchParams, appendAnd, isGroup);
        if (StringUtils.hasText(query)) {
            return query;
        }

        LocalDate dtStart = invoiceSearchParams.startDate();
        if (dtStart == null) {
            dtStart = LocalDate.now().minusMonths(1);
        }
        LocalDate end = invoiceSearchParams.endDate();
        if (end == null) {
            end = LocalDate.now();
        }
        return appendAnd.concat(" DATE(f.created)  BETWEEN ").concat("'" + dtStart + "'").concat(" AND ").concat("'" + end + "'");
    }

    default String isFactureProvisoire(InvoiceSearchParams invoiceSearchParams, String appendAnd) {
        String close = invoiceSearchParams.factureProvisoire() ? "TRUE" : "FALSE";
        return appendAnd.concat(" f.facture_provisoire IS ").concat(close);
    }

    default String buildStatus(InvoiceSearchParams invoiceSearchParams, String appendAnd) {
        if (CollectionUtils.isEmpty(invoiceSearchParams.statuts())) {
            return "";
        }
        return appendAnd.concat(" f.statut IN (").concat(buildInvoiceStatut(invoiceSearchParams.statuts())).concat(") ");
    }

    default String buildInvoiceStatut(Set<InvoiceStatut> invoiceStatuts) {
        return invoiceStatuts.stream().map(e -> "'" + e.name() + "'").collect(Collectors.joining(","));
    }








    FactureTiersPayant getFactureTiersPayant(Long id);

    List<FactureTiersPayant> getFactureTiersPayant(LocalDateTime created, boolean isGroup);

    Resource printToPdf(FactureEditionResponse factureEditionResponse);

    Resource printToPdf(Long id);

    Page<FacturationGroupeDossier> findGroupeFactureReglementData(Long id, Pageable pageable);

    Page<FacturationDossier> findFactureReglementData(Long id, Pageable pageable);

    DossierFactureProjection findDossierFacture(Long id, boolean isGroup);
}
