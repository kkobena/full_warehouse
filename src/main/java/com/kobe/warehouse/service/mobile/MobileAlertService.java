package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.dto.mobile.AlertSeverity;
import com.kobe.warehouse.service.dto.mobile.AlertType;
import com.kobe.warehouse.service.dto.mobile.MobileAlertDetailDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile alerts and notifications.
 */
@Service
@Transactional(readOnly = true)
public class MobileAlertService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileAlertService.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get summary of all alert types for dashboard.
     *
     * @return List of alert summaries with counts
     */
    public List<MobileAlertDTO> getAlertsSummary() {
        List<MobileAlertDTO> alerts = new ArrayList<>();

        // 1. Stock ruptures (critical)
        int stockRuptureCount = getStockRuptureCount();
        if (stockRuptureCount > 0) {
            AlertType type = AlertType.STOCK_RUPTURE;
            AlertSeverity severity = AlertSeverity.CRITICAL;
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                stockRuptureCount + " ruptures de stock",
                stockRuptureCount,
                type.getIcon(),
                severity.getColor()
            ));
        }

        // 2. Expiring products (< 30 days)
        int expiryCount = getExpiringProductsCount(30);
        if (expiryCount > 0) {
            AlertType type = AlertType.EXPIRY;
            AlertSeverity severity = AlertSeverity.WARNING;
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                expiryCount + " peremptions < 30j",
                expiryCount,
                type.getIcon(),
                severity.getColor()
            ));
        }

        // 3. Cash discrepancy (today)
        CashDiscrepancy cashDiscrepancy = getCashDiscrepancy(LocalDate.now());
        if (cashDiscrepancy.hasDiscrepancy()) {
            AlertType type = AlertType.CASH_DISCREPANCY;
            AlertSeverity severity = AlertSeverity.fromCriticalThreshold(cashDiscrepancy.amount() > 10000);
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                "Ecart caisse: " + formatAmount(cashDiscrepancy.amount()) + " F",
                1,
                type.getIcon(),
                severity.getColor()
            ));
        }

        // 4. Overdue invoices (> 90 days)
        int overdueCount = getOverdueInvoicesCount(90);
        if (overdueCount > 0) {
            AlertType type = AlertType.INVOICE_OVERDUE;
            AlertSeverity severity = AlertSeverity.INFO;
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                overdueCount + " factures impayees > 90j",
                overdueCount,
                type.getIcon(),
                severity.getColor()
            ));
        }

        return alerts;
    }

    /**
     * Get detailed list of alerts by type.
     *
     * @param types List of alert types to filter (null for all)
     * @return List of detailed alerts
     */
    public List<MobileAlertDetailDTO> getAlerts(List<String> types) {
        List<MobileAlertDetailDTO> alerts = new ArrayList<>();

        if (types == null || types.isEmpty() || types.contains(AlertType.STOCK_RUPTURE.getCode())) {
            alerts.addAll(getStockRuptureAlerts());
        }

        if (types == null || types.isEmpty() || types.contains(AlertType.EXPIRY.getCode())) {
            alerts.addAll(getExpiryAlerts(30));
        }

        if (types == null || types.isEmpty() || types.contains(AlertType.INVOICE_OVERDUE.getCode())) {
            alerts.addAll(getOverdueInvoiceAlerts(90));
        }

        return alerts;
    }

    /**
     * Get count of products in stock rupture.
     * A product is in rupture when the total stock across all storages is zero.
     * Uses qty_stock and qty_ug columns from stock_produit table.
     */
    private int getStockRuptureCount() {
        String sql = """
            SELECT COUNT(*)
            FROM (
                SELECT p.id
                FROM produit p
                INNER JOIN stock_produit sp ON sp.produit_id = p.id
                WHERE p.status = :status
                GROUP BY p.id
                HAVING COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) = 0
            ) ruptures
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("status", Status.ENABLE.name());
        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Get detailed stock rupture alerts.
     * A product is in rupture when the total stock across all storages is zero.
     * Uses qty_stock and qty_ug columns from stock_produit table.
     */
    private List<MobileAlertDetailDTO> getStockRuptureAlerts() {
        String sql = """
            SELECT
                p.id,
                p.libelle,
                fp.code_cip,
                COALESCE(SUM(sp.qty_stock), 0) as total_qty_stock,
                COALESCE(SUM(sp.qty_ug), 0) as total_qty_ug
            FROM produit p
            INNER JOIN stock_produit sp ON sp.produit_id = p.id
            LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
            WHERE p.status = :status
            GROUP BY p.id, p.libelle, fp.code_cip
            HAVING COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) = 0
            ORDER BY p.libelle
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("status", Status.ENABLE.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        AlertType type = AlertType.STOCK_RUPTURE;
        AlertSeverity severity = AlertSeverity.CRITICAL;

        List<MobileAlertDetailDTO> alerts = new ArrayList<>();
        for (Object[] row : results) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];

            alerts.add(MobileAlertDetailDTO.builder()
                .id(productId)
                .type(type.getCode())
                .severity(severity.getCode())
                .title(type.getLibelle())
                .message(productName + " - Stock epuise")
                .icon(type.getIcon())
                .color(severity.getColor())
                .createdAt(LocalDateTime.now())
                .actionType("VIEW_PRODUCT")
                .actionData(Map.of("productId", productId))
                .relatedEntityId(productId)
                .relatedEntityType("PRODUCT")
                .relatedEntityName(productName)
                .build());
        }

        return alerts;
    }

    /**
     * Get count of products expiring within days.
     * Uses current_quantity column from lot table.
     */
    private int getExpiringProductsCount(int days) {
        String sql = """
            SELECT COUNT(DISTINCT l.produit_id)
            FROM lot l
            INNER JOIN produit p ON l.produit_id = p.id
            WHERE l.expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + :days
              AND l.current_quantity > 0
              AND p.status = :status
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("days", days);
        query.setParameter("status", Status.ENABLE.name());
        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Get detailed expiry alerts.
     * Uses current_quantity column from lot table.
     */
    private List<MobileAlertDetailDTO> getExpiryAlerts(int days) {
        String sql = """
            SELECT
                l.id,
                p.id as produit_id,
                p.libelle,
                l.num_lot,
                l.expiry_date,
                l.current_quantity,
                (l.expiry_date - CURRENT_DATE) as days_until_expiry
            FROM lot l
            INNER JOIN produit p ON l.produit_id = p.id
            WHERE l.expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + :days
              AND l.current_quantity > 0
              AND p.status = :status
            ORDER BY l.expiry_date
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("days", days);
        query.setParameter("status", Status.ENABLE.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        AlertType type = AlertType.EXPIRY;

        List<MobileAlertDetailDTO> alerts = new ArrayList<>();
        for (Object[] row : results) {
            Long lotId = ((Number) row[0]).longValue();
            Long productId = ((Number) row[1]).longValue();
            String productName = (String) row[2];
            int daysUntilExpiry = ((Number) row[6]).intValue();

            AlertSeverity severity = AlertSeverity.fromCriticalThreshold(daysUntilExpiry <= 7);

            alerts.add(MobileAlertDetailDTO.builder()
                .id(lotId)
                .type(type.getCode())
                .severity(severity.getCode())
                .title(type.getLibelle())
                .message(productName + " - Expire dans " + daysUntilExpiry + " jours")
                .icon(type.getIcon())
                .color(severity.getColor())
                .createdAt(LocalDateTime.now())
                .actionType("VIEW_PRODUCT")
                .actionData(Map.of("productId", productId, "lotId", lotId))
                .relatedEntityId(productId)
                .relatedEntityType("PRODUCT")
                .relatedEntityName(productName)
                .build());
        }

        return alerts;
    }

    /**
     * Get cash discrepancy for a date.
     * Calculates difference between ticketing total and expected cash from sales.
     */
    private CashDiscrepancy getCashDiscrepancy(LocalDate date) {
        String sql = """
            SELECT COALESCE(SUM(ABS(
                t.total_amount - COALESCE(cr.final_amount, 0)
            )), 0)
            FROM cash_register cr
            INNER JOIN ticketing t ON t.cash_register_id = cr.id
            WHERE DATE(cr.end_time) = :date
              AND cr.statut = 'CLOSED'
              AND ABS(t.total_amount - COALESCE(cr.final_amount, 0)) > 0
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);

        long amount = ((Number) query.getSingleResult()).longValue();
        return new CashDiscrepancy(amount);
    }

    /**
     * Get count of overdue invoices.
     * Uses third_party_sale_line to calculate invoice amounts.
     */
    private int getOverdueInvoicesCount(int days) {
        String sql = """
            SELECT COUNT(DISTINCT ftp.id)
            FROM facture_tiers_payant ftp
            INNER JOIN third_party_sale_line tpsl ON tpsl.facture_tiers_payant_id = ftp.id
                AND tpsl.invoice_date = ftp.invoice_date
            WHERE ftp.statut IN (:notPaid, :partiallyPaid)
              AND ftp.created < CURRENT_TIMESTAMP - INTERVAL ':days days'
            GROUP BY ftp.id, ftp.invoice_date, ftp.montant_regle
            HAVING COALESCE(SUM(tpsl.montant), 0) > COALESCE(ftp.montant_regle, 0)
            """;

        // Simpler approach: count factures with remaining balance
        String simpleSql = """
            SELECT COUNT(*)
            FROM (
                SELECT ftp.id
                FROM facture_tiers_payant ftp
                INNER JOIN third_party_sale_line tpsl ON tpsl.facture_tiers_payant_id = ftp.id
                    AND tpsl.invoice_date = ftp.invoice_date
                WHERE ftp.statut IN (:notPaid, :partiallyPaid)
                  AND ftp.created < CURRENT_DATE - :days
                GROUP BY ftp.id, ftp.invoice_date, ftp.montant_regle
                HAVING COALESCE(SUM(tpsl.montant), 0) > COALESCE(ftp.montant_regle, 0)
            ) overdue
            """;

        Query query = entityManager.createNativeQuery(simpleSql);
        query.setParameter("notPaid", InvoiceStatut.NOT_PAID.name());
        query.setParameter("partiallyPaid", InvoiceStatut.PARTIALLY_PAID.name());
        query.setParameter("days", days);
        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Get detailed overdue invoice alerts.
     * Uses third_party_sale_line to calculate invoice amounts.
     */
    private List<MobileAlertDetailDTO> getOverdueInvoiceAlerts(int days) {
        String sql = """
            SELECT
                ftp.id,
                ftp.invoice_date,
                gtp.name as tiers_payant_name,
                gtp.telephone,
                COALESCE(SUM(tpsl.montant), 0) as montant_facture,
                COALESCE(ftp.montant_regle, 0) as montant_regle,
                (CURRENT_DATE - DATE(ftp.created)) as days_overdue
            FROM facture_tiers_payant ftp
            INNER JOIN groupe_tiers_payant gtp ON ftp.groupe_tiers_payant_id = gtp.id
            INNER JOIN third_party_sale_line tpsl ON tpsl.facture_tiers_payant_id = ftp.id
                AND tpsl.invoice_date = ftp.invoice_date
            WHERE ftp.statut IN (:notPaid, :partiallyPaid)
              AND ftp.created < CURRENT_DATE - :days
            GROUP BY ftp.id, ftp.invoice_date, gtp.name, gtp.telephone, ftp.montant_regle, ftp.created
            HAVING COALESCE(SUM(tpsl.montant), 0) > COALESCE(ftp.montant_regle, 0)
            ORDER BY days_overdue DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("notPaid", InvoiceStatut.NOT_PAID.name());
        query.setParameter("partiallyPaid", InvoiceStatut.PARTIALLY_PAID.name());
        query.setParameter("days", days);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        AlertType type = AlertType.INVOICE_OVERDUE;

        List<MobileAlertDetailDTO> alerts = new ArrayList<>();
        for (Object[] row : results) {
            Long invoiceId = ((Number) row[0]).longValue();
            String tiersPayantName = (String) row[2];
            String phone = (String) row[3];
            long montantFacture = ((Number) row[4]).longValue();
            long montantRegle = ((Number) row[5]).longValue();
            int daysOverdue = ((Number) row[6]).intValue();

            long montantRestant = montantFacture - montantRegle;

            AlertSeverity severity = AlertSeverity.fromCriticalThreshold(daysOverdue > 180);

            alerts.add(MobileAlertDetailDTO.builder()
                .id(invoiceId)
                .type(type.getCode())
                .severity(severity.getCode())
                .title(type.getLibelle())
                .message(tiersPayantName + " - " + formatAmount(montantRestant) + " F depuis " + daysOverdue + "j")
                .icon(type.getIcon())
                .color(severity.getColor())
                .createdAt(LocalDateTime.now())
                .actionType("CALL_CLIENT")
                .actionData(Map.of("invoiceId", invoiceId, "phone", phone != null ? phone : ""))
                .relatedEntityId(invoiceId)
                .relatedEntityType("INVOICE")
                .relatedEntityName(tiersPayantName)
                .build());
        }

        return alerts;
    }

    private String formatAmount(long amount) {
        return String.format("%,d", amount).replace(",", " ");
    }

    /**
     * Internal record for cash discrepancy.
     */
    private record CashDiscrepancy(long amount) {
        boolean hasDiscrepancy() {
            return amount > 0;
        }
    }
}
