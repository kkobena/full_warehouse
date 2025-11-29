package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TiersPayantReportServiceImpl implements TiersPayantReportService {


    private final EntityManager entityManager;

    public TiersPayantReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public List<TiersPayantInvoiceDTO> getUnpaidInvoices(
        Integer groupeTiersPayantId,
        TiersPayantInvoiceDTO.AgeCategory ageCategory
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("f.id, ");
        sql.append("f.num_facture, ");
        sql.append("f.invoice_date, ");
        sql.append("tp.name as tiers_payant_libelle, ");
        sql.append("gtp.name as groupe_tiers_payant_libelle, ");
        sql.append("SUM(fi.montant) as montant_facture, ");
        sql.append("f.montant_regle as montant_paye, ");
        sql.append("SUM(fi.montant) - f.montant_regle as montant_restant, ");
        sql.append("f.statut, ");
        sql.append("CAST(CURRENT_DATE - f.invoice_date AS INTEGER) as days_since_invoice ");
        sql.append("FROM facture_tiers_payant f ");
        sql.append("INNER JOIN groupe_tiers_payant gtp ON f.groupe_tiers_payant_id = gtp.id ");
        sql.append("LEFT JOIN tiers_payant tp ON f.tiers_payant_id = tp.id ");
        sql.append("LEFT JOIN facture_item fi ON f.id = fi.facture_id AND f.invoice_date = fi.facture_invoice_date ");
        sql.append("WHERE f.statut IN ('NOT_PAID', 'PARTIALLY_PAID') ");

        if (groupeTiersPayantId != null) {
            sql.append("AND f.groupe_tiers_payant_id = :groupeTiersPayantId ");
        }

        if (ageCategory != null) {
            sql.append("AND ");
            switch (ageCategory) {
                case LESS_THAN_30:
                    sql.append("(CURRENT_DATE - f.invoice_date) < 30 ");
                    break;
                case BETWEEN_30_60:
                    sql.append("(CURRENT_DATE - f.invoice_date) BETWEEN 30 AND 60 ");
                    break;
                case BETWEEN_60_90:
                    sql.append("(CURRENT_DATE - f.invoice_date) BETWEEN 60 AND 90 ");
                    break;
                case MORE_THAN_90:
                    sql.append("(CURRENT_DATE - f.invoice_date) > 90 ");
                    break;
            }
        }

        sql.append("GROUP BY f.id, f.num_facture, f.invoice_date, tp.name, gtp.name, f.montant_regle, f.statut ");
        sql.append("ORDER BY f.invoice_date DESC");

        Query query = entityManager.createNativeQuery(sql.toString());

        if (groupeTiersPayantId != null) {
            query.setParameter("groupeTiersPayantId", groupeTiersPayantId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                Long factureId = row[0] != null ? ((Number) row[0]).longValue() : null;
                String numeroFacture = (String) row[1];
                LocalDate dateFacture = row[2] != null ? ((Date) row[2]).toLocalDate() : null;
                String tiersPayantLibelle = (String) row[3];
                String groupeTiersPayantLibelle = (String) row[4];
                Integer montantFacture = row[5] != null ? ((Number) row[5]).intValue() : 0;
                Integer montantPaye = row[6] != null ? ((Number) row[6]).intValue() : 0;
                Integer montantRestant = row[7] != null ? ((Number) row[7]).intValue() : 0;
                String statutStr = (String) row[8];
                Integer daysSinceInvoice = row[9] != null ? ((Number) row[9]).intValue() : 0;

                TiersPayantInvoiceDTO.InvoiceStatus statut = mapInvoiceStatus(statutStr);
                TiersPayantInvoiceDTO.AgeCategory ageCat = calculateAgeCategory(daysSinceInvoice);

                return new TiersPayantInvoiceDTO(
                    factureId,
                    numeroFacture,
                    dateFacture,
                    tiersPayantLibelle,
                    groupeTiersPayantLibelle,
                    montantFacture,
                    montantPaye,
                    montantRestant,
                    statut,
                    daysSinceInvoice,
                    ageCat
                );
            })
            .toList();
    }

    @Override
    @Cacheable(value = "tiersPayantCreances", key = "'summary'")
    public List<TiersPayantCreancesSummaryDTO> getCreancesSummary() {
        String sql =
            "SELECT " +
                "gtp.id, " +
                "gtp.name as groupe_tiers_payant_libelle, " +
                "COUNT(f.id) as nombre_factures, " +
                "SUM(fi.montant) as montant_total, " +
                "SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) < 30 THEN fi.montant - f.montant_regle ELSE 0 END) as moins_30j, " +
                "SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) BETWEEN 30 AND 60 THEN fi.montant - f.montant_regle ELSE 0 END) as entre_30_60j, " +
                "SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) BETWEEN 60 AND 90 THEN fi.montant - f.montant_regle ELSE 0 END) as entre_60_90j, " +
                "SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) > 90 THEN fi.montant - f.montant_regle ELSE 0 END) as plus_90j " +
                "FROM groupe_tiers_payant gtp " +
                "INNER JOIN facture_tiers_payant f ON gtp.id = f.groupe_tiers_payant_id " +
                "LEFT JOIN facture_item fi ON f.id = fi.facture_id AND f.invoice_date = fi.facture_invoice_date " +
                "WHERE f.statut IN ('NOT_PAID', 'PARTIALLY_PAID') " +
                "GROUP BY gtp.id, gtp.name " +
                "ORDER BY montant_total DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                Integer groupeTiersPayantId = (Integer) row[0];
                String groupeTiersPayantLibelle = (String) row[1];
                Integer nombreFactures = row[2] != null ? ((Number) row[2]).intValue() : 0;
                Integer montantTotal = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer montantMoinsDe30Jours = row[4] != null ? ((Number) row[4]).intValue() : 0;
                Integer montantEntre30Et60Jours = row[5] != null ? ((Number) row[5]).intValue() : 0;
                Integer montantEntre60Et90Jours = row[6] != null ? ((Number) row[6]).intValue() : 0;
                Integer montantPlusDe90Jours = row[7] != null ? ((Number) row[7]).intValue() : 0;

                return new TiersPayantCreancesSummaryDTO(
                    groupeTiersPayantId,
                    groupeTiersPayantLibelle,
                    nombreFactures,
                    montantTotal,
                    montantMoinsDe30Jours,
                    montantEntre30Et60Jours,
                    montantEntre60Et90Jours,
                    montantPlusDe90Jours
                );
            })
            .toList();
    }

    @Override
    public List<TiersPayantInvoiceDTO> getPaymentHistory(Integer groupeTiersPayantId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("f.id, ");
        sql.append("f.num_facture, ");
        sql.append("f.invoice_date, ");
        sql.append("tp.name as tiers_payant_libelle, ");
        sql.append("gtp.name as groupe_tiers_payant_libelle, ");
        sql.append("SUM(fi.montant) as montant_facture, ");
        sql.append("f.montant_regle as montant_paye, ");
        sql.append("0 as montant_restant, ");
        sql.append("f.statut ");
        sql.append("FROM facture_tiers_payant f ");
        sql.append("INNER JOIN groupe_tiers_payant gtp ON f.groupe_tiers_payant_id = gtp.id ");
        sql.append("LEFT JOIN tiers_payant tp ON f.tiers_payant_id = tp.id ");
        sql.append("LEFT JOIN facture_item fi ON f.id = fi.facture_id AND f.invoice_date = fi.facture_invoice_date ");
        sql.append("WHERE f.statut = 'PAID' ");
        sql.append("AND f.invoice_date BETWEEN :startDate AND :endDate ");

        if (groupeTiersPayantId != null) {
            sql.append("AND f.groupe_tiers_payant_id = :groupeTiersPayantId ");
        }

        sql.append("GROUP BY f.id, f.num_facture, f.invoice_date, tp.name, gtp.name, f.montant_regle, f.statut ");
        sql.append("ORDER BY f.invoice_date DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        if (groupeTiersPayantId != null) {
            query.setParameter("groupeTiersPayantId", groupeTiersPayantId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                Long factureId = row[0] != null ? ((Number) row[0]).longValue() : null;
                String numeroFacture = (String) row[1];
                LocalDate dateFacture = row[2] != null ? ((Date) row[2]).toLocalDate() : null;
                String tiersPayantLibelle = (String) row[3];
                String groupeTiersPayantLibelle = (String) row[4];
                Integer montantFacture = row[5] != null ? ((Number) row[5]).intValue() : 0;
                Integer montantPaye = row[6] != null ? ((Number) row[6]).intValue() : 0;
                Integer montantRestant = 0;
                String statutStr = (String) row[8];

                return new TiersPayantInvoiceDTO(
                    factureId,
                    numeroFacture,
                    dateFacture,
                    tiersPayantLibelle,
                    groupeTiersPayantLibelle,
                    montantFacture,
                    montantPaye,
                    montantRestant,
                    TiersPayantInvoiceDTO.InvoiceStatus.PAID,
                    0,
                    TiersPayantInvoiceDTO.AgeCategory.LESS_THAN_30
                );
            })
            .toList();
    }


    private TiersPayantInvoiceDTO.InvoiceStatus mapInvoiceStatus(String statut) {
        if (statut == null) return TiersPayantInvoiceDTO.InvoiceStatus.UNPAID;
        return switch (statut) {
            case "PAID" -> TiersPayantInvoiceDTO.InvoiceStatus.PAID;
            case "PARTIALLY_PAID" -> TiersPayantInvoiceDTO.InvoiceStatus.PARTIAL;
            default -> TiersPayantInvoiceDTO.InvoiceStatus.UNPAID;
        };
    }

    private TiersPayantInvoiceDTO.AgeCategory calculateAgeCategory(Integer daysSinceInvoice) {
        if (daysSinceInvoice == null || daysSinceInvoice < 30) {
            return TiersPayantInvoiceDTO.AgeCategory.LESS_THAN_30;
        } else if (daysSinceInvoice < 60) {
            return TiersPayantInvoiceDTO.AgeCategory.BETWEEN_30_60;
        } else if (daysSinceInvoice < 90) {
            return TiersPayantInvoiceDTO.AgeCategory.BETWEEN_60_90;
        } else {
            return TiersPayantInvoiceDTO.AgeCategory.MORE_THAN_90;
        }
    }
}
