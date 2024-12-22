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
    String QUERY =
        """
          SELECT t.id AS tiersPayantId,t.full_name AS tiersPayantName,SUM(sl.montant) AS totalAmount,COUNT(sl.id) AS factureItemCount FROM third_party_sale_line sl JOIN sales s ON sl.sale_id = s.id JOIN client_tiers_payant  c ON sl.client_tiers_payant_id = c.id
         JOIN tiers_payant t ON c.tiers_payant_id = t.id  LEFT JOIN facture_tiers_payant ftp ON sl.facture_tiers_payant_id = ftp.id
        """;
    String GROUP_QUERY =
        """
                SELECT gtp.id as tiersPayantId,gtp.name as tiersPayantName,SUM(sl.montant) as totalAmount,COUNT(sl.id) AS factureItemCount FROM third_party_sale_line sl JOIN sales s ON sl.sale_id = s.id JOIN client_tiers_payant  c ON sl.client_tiers_payant_id = c.id JOIN tiers_payant t ON c.tiers_payant_id = t.id
        LEFT JOIN facture_tiers_payant ftp ON sl.facture_tiers_payant_id = ftp.id JOIN groupe_tiers_payant gtp ON t.groupe_tiers_payant_id = gtp.id
        """;

    String QUERY_COUNT =
        """
          SELECT COUNT(distinct t.id) as factureItemCount FROM third_party_sale_line sl JOIN sales s ON sl.sale_id = s.id JOIN client_tiers_payant  c ON sl.client_tiers_payant_id = c.id
         JOIN tiers_payant t ON c.tiers_payant_id = t.id  LEFT JOIN facture_tiers_payant ftp ON sl.facture_tiers_payant_id = ftp.id
        """;
    String GROUP_COUNT =
        """
                SELECT COUNT(distinct gtp.id) as factureItemCount FROM third_party_sale_line sl JOIN sales s ON sl.sale_id = s.id JOIN client_tiers_payant  c ON sl.client_tiers_payant_id = c.id JOIN tiers_payant t ON c.tiers_payant_id = t.id
        LEFT JOIN facture_tiers_payant ftp ON sl.facture_tiers_payant_id = ftp.id JOIN groupe_tiers_payant gtp ON t.groupe_tiers_payant_id = gtp.id
        """;
    String FACTURES_UNITAIRE_QUERY =
        """
          SELECT f.created,  f.id as factureId,gf.id as groupeFactureId,f.montant_regle as montantRegle,f.remise_forfetaire as remiseForfetaire,f.debut_periode as debutPeriode,f.statut  as statut,
          f.fin_periode as finPeriode,f.facture_provisoire as factureProvisoire,f.num_facture as numFacture,gf.num_facture as groupeNumFacture,tp.full_name  as tiersPayantName,th.montantVente AS montantVente,th.montantRemise AS montantRemise,
          th.itemMontantRegle,th.itemsCount AS itemsCount,th.montantTiersPayant AS montantAttendu FROM  facture_tiers_payant f  JOIN (SELECT th.facture_tiers_payant_id,COUNT(th.id) as itemsCount,SUM(th.montant_regle)  as itemMontantRegle,SUM(th.montant) as montantTiersPayant,SUM(s.sales_amount) AS montantVente,SUM(s.discount_amount) AS montantRemise  FROM third_party_sale_line th
        JOIN sales s ON  th.sale_id=s.id GROUP  BY th.facture_tiers_payant_id) AS  th ON f.id=th.facture_tiers_payant_id LEFT JOIN facture_tiers_payant gf ON f.groupe_facture_tiers_payant_id=gf.id  JOIN tiers_payant tp ON f.tiers_payant_id=tp.id
          WHERE f.id NOT IN (SELECT fd.groupe_facture_tiers_payant_id FROM facture_tiers_payant fd WHERE fd.groupe_facture_tiers_payant_id IS NOT NULL)
        """;

    String FACTURES_UNITAIRE_COUNT_QUERY =
        """
        SELECT COUNT( f.id) as itemsCount FROM  facture_tiers_payant f  LEFT JOIN facture_tiers_payant gf ON f.groupe_facture_tiers_payant_id=gf.id  JOIN tiers_payant tp ON f.tiers_payant_id=tp.id
        WHERE f.id NOT IN (SELECT fd.groupe_facture_tiers_payant_id FROM facture_tiers_payant fd WHERE fd.groupe_facture_tiers_payant_id IS NOT NULL)
        """;

    String GROUPE_FACTURE_QUERY =
        """
        SELECT f.created,  f.id as factureId,f.montant_regle as montantRegle,f.debut_periode as debutPeriode,f.statut  as statut,f.fin_periode as finPeriode,f.facture_provisoire as factureProvisoire,f.num_facture as numFacture,gtp.name as tiersPayantName,
         fd.montantTiersPayant as montantAttendu,fd.itemsCount AS itemsCount,fd.montantVente AS montantVente,fd.montantRemise AS montantRemise FROM  facture_tiers_payant f JOIN groupe_tiers_payant gtp ON f.groupe_tiers_payant_id=gtp.id
         JOIN (SELECT i.groupe_facture_tiers_payant_id,COUNT(distinct i.id) as itemsCount,SUM(it.montant) as montantTiersPayant,SUM(s.sales_amount) AS montantVente,SUM(s.discount_amount) AS montantRemise   FROM facture_tiers_payant i JOIN third_party_sale_line it ON i.id = it.facture_tiers_payant_id JOIN sales s ON  it.sale_id=s.id  GROUP BY i.groupe_facture_tiers_payant_id )
          as fd ON f.id=fd.groupe_facture_tiers_payant_id

        """;
    String GROUPE_FACTURE_COUNT_QUERY =
        """
        SELECT COUNT( f.id) as itemsCount FROM  facture_tiers_payant f JOIN groupe_tiers_payant gtp ON f.groupe_tiers_payant_id=gtp.id JOIN (SELECT i.groupe_facture_tiers_payant_id   FROM facture_tiers_payant i  ) as fd ON f.id=fd.groupe_facture_tiers_payant_id
        """;

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

    default String buildFinalQuery(EditionSearchParams editionSearchParams) {
        if (editionSearchParams.modeEdition() == ModeEditionEnum.GROUP) {
            return buildQuery(editionSearchParams, GROUP_QUERY).concat(" GROUP BY gtp.id ORDER BY gtp.name ASC ");
        }
        return buildQuery(editionSearchParams, QUERY).concat(" GROUP BY t.id ORDER BY t.full_name ASC ");
    }

    default String buildFinalCountQuery(EditionSearchParams editionSearchParams) {
        if (editionSearchParams.modeEdition() == ModeEditionEnum.GROUP) {
            return buildQuery(editionSearchParams, GROUP_COUNT);
        }
        return buildQuery(editionSearchParams, QUERY_COUNT);
    }

    default String buildInvoiceQuery(InvoiceSearchParams invoiceSearchParams, String query) {
        return query
            .concat(buildSearchPeriod(invoiceSearchParams, " AND ", false))
            .concat(buildTiersPayantIds(invoiceSearchParams.tiersPayantIds()))
            .concat(isFactureProvisoire(invoiceSearchParams, " AND "))
            .concat(buildStatus(invoiceSearchParams, " AND "));
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

    default String buildGroupTiersPayantIds(Set<Long> groupIds, String appendAnd) {
        if (CollectionUtils.isEmpty(groupIds)) {
            return "";
        }
        return appendAnd + "  f.groupe_tiers_payant_id IN (".concat(buildIds(groupIds)).concat(") ");
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

    default String buildFinalInvoiceQuery(InvoiceSearchParams invoiceSearchParams) {
        return buildInvoiceQuery(invoiceSearchParams, FACTURES_UNITAIRE_QUERY).concat(" ORDER BY f.created DESC,tp.full_name ASC ");
    }

    default String buildGroupInvoiceQuery(InvoiceSearchParams invoiceSearchParams, String query) {
        return query
            .concat(buildSearchPeriod(invoiceSearchParams, " WHERE ", true))
            .concat(buildGroupTiersPayantIds(invoiceSearchParams.groupIds(), " AND "))
            .concat(isFactureProvisoire(invoiceSearchParams, " AND "))
            .concat(buildStatus(invoiceSearchParams, " AND "));
    }

    default String buildFinalGroupInvoiceQuery(InvoiceSearchParams invoiceSearchParams) {
        return buildGroupInvoiceQuery(invoiceSearchParams, GROUPE_FACTURE_QUERY).concat(" ORDER BY f.created DESC,gtp.name ASC ");
    }

    default String buildCountFinalInvoiceQuery(InvoiceSearchParams invoiceSearchParams) {
        return buildInvoiceQuery(invoiceSearchParams, FACTURES_UNITAIRE_COUNT_QUERY);
    }

    default String buildCountFinalGroupInvoiceQuery(InvoiceSearchParams invoiceSearchParams) {
        return buildGroupInvoiceQuery(invoiceSearchParams, GROUPE_FACTURE_COUNT_QUERY);
    }

    FactureTiersPayant getFactureTiersPayant(Long id);

    List<FactureTiersPayant> getFactureTiersPayant(LocalDateTime created, boolean isGroup);

    Resource printToPdf(FactureEditionResponse factureEditionResponse);

    Resource printToPdf(Long id);

    Page<FacturationGroupeDossier> findGroupeFactureReglementData(Long id, Pageable pageable);

    Page<FacturationDossier> findFactureReglementData(Long id, Pageable pageable);

    DossierFactureProjection findDossierFacture(Long id, boolean isGroup);
}
