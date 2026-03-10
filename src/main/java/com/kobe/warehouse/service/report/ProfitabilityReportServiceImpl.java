package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.BCGCategory;
import com.kobe.warehouse.service.dto.report.ProductProfitabilityDTO;
import com.kobe.warehouse.service.dto.report.ProfitabilitySummaryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

//Service à supprimer
@Service
@Transactional(readOnly = true)
public class ProfitabilityReportServiceImpl implements ProfitabilityReportService {
    private final EntityManager entityManager;

    public ProfitabilityReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Cacheable(value = "profitability", key = "'all'")
    public List<ProductProfitabilityDTO> getAllProductProfitability() {
        String sql =
            "SELECT " +
                "produit_id, libelle, code_cip, categorie, nb_ventes, qte_vendue, " +
                "ca_total, cout_achat_total, marge_brute, taux_marge_pct, " +
                "prix_vente_moyen, prix_achat_moyen, stock_quantity, " +
                "prix_achat_unitaire, prix_vente_unitaire, taux_rotation_annuel, bcg_category " +
                "FROM mv_product_profitability " +
                "ORDER BY marge_brute DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }


    @Override
    public ProfitabilitySummaryDTO getProfitabilitySummary() {
        String sql =
            "SELECT " +
                "total_produits, ca_total_global, cout_achat_global, marge_brute_globale, taux_marge_moyen, " +
                "nb_stars, nb_cash_cows, nb_question_marks, nb_dogs, " +
                "ca_stars, ca_cash_cows, ca_question_marks, ca_dogs, " +
                "marge_stars, marge_cash_cows, marge_question_marks, marge_dogs " +
                "FROM mv_profitability_summary";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return new ProfitabilitySummaryDTO(0, 0L, 0L, 0L, BigDecimal.ZERO, 0, 0, 0, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        Object[] row = results.getFirst();

        return new ProfitabilitySummaryDTO(
            row[0] != null ? ((Number) row[0]).intValue() : 0,
            row[1] != null ? ((Number) row[1]).longValue() : 0L,
            row[2] != null ? ((Number) row[2]).longValue() : 0L,
            row[3] != null ? ((Number) row[3]).longValue() : 0L,
            row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO,
            row[5] != null ? ((Number) row[5]).intValue() : 0,
            row[6] != null ? ((Number) row[6]).intValue() : 0,
            row[7] != null ? ((Number) row[7]).intValue() : 0,
            row[8] != null ? ((Number) row[8]).intValue() : 0,
            row[9] != null ? ((Number) row[9]).longValue() : 0L,
            row[10] != null ? ((Number) row[10]).longValue() : 0L,
            row[11] != null ? ((Number) row[11]).longValue() : 0L,
            row[12] != null ? ((Number) row[12]).longValue() : 0L,
            row[13] != null ? ((Number) row[13]).longValue() : 0L,
            row[14] != null ? ((Number) row[14]).longValue() : 0L,
            row[15] != null ? ((Number) row[15]).longValue() : 0L,
            row[16] != null ? ((Number) row[16]).longValue() : 0L
        );
    }


    private List<ProductProfitabilityDTO> mapResultsToDTO(List<Object[]> results) {
        return results
            .stream()
            .map(row -> {
                Integer produitId = row[0] != null ? ((Number) row[0]).intValue() : null;
                String libelle = (String) row[1];
                String codeCip = (String) row[2];
                String categorie = (String) row[3];
                Integer nbVentes = row[4] != null ? ((Number) row[4]).intValue() : 0;
                Integer qteVendue = row[5] != null ? ((Number) row[5]).intValue() : 0;
                Integer caTotal = row[6] != null ? ((Number) row[6]).intValue() : 0;
                Integer coutAchatTotal = row[7] != null ? ((Number) row[7]).intValue() : 0;
                Integer margeBrute = row[8] != null ? ((Number) row[8]).intValue() : 0;
                BigDecimal tauxMargePct = row[9] != null ? new BigDecimal(row[9].toString()) : BigDecimal.ZERO;
                Integer prixVenteMoyen = row[10] != null ? ((Number) row[10]).intValue() : 0;
                Integer prixAchatMoyen = row[11] != null ? ((Number) row[11]).intValue() : 0;
                Integer stockQuantity = row[12] != null ? ((Number) row[12]).intValue() : 0;
                Integer prixAchatUnitaire = row[13] != null ? ((Number) row[13]).intValue() : 0;
                Integer prixVenteUnitaire = row[14] != null ? ((Number) row[14]).intValue() : 0;
                BigDecimal tauxRotationAnnuel = row[15] != null ? new BigDecimal(row[15].toString()) : BigDecimal.ZERO;
                String bcgCategoryStr = (String) row[16];

                BCGCategory bcgCategory = bcgCategoryStr != null ? BCGCategory.valueOf(bcgCategoryStr) : BCGCategory.UNDEFINED;

                return new ProductProfitabilityDTO(
                    produitId,
                    libelle,
                    codeCip,
                    categorie,
                    nbVentes,
                    qteVendue,
                    caTotal,
                    coutAchatTotal,
                    margeBrute,
                    tauxMargePct,
                    prixVenteMoyen,
                    prixAchatMoyen,
                    stockQuantity,
                    prixAchatUnitaire,
                    prixVenteUnitaire,
                    tauxRotationAnnuel,
                    bcgCategory
                );
            })
            .toList();
    }
}
