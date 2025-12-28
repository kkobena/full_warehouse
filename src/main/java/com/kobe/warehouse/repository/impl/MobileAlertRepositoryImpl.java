package com.kobe.warehouse.repository.impl;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.repository.MobileAlertRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Implementation of MobileAlertRepository using native SQL queries.
 */
@Repository
public class MobileAlertRepositoryImpl implements MobileAlertRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int getStockRuptureCount() {
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

    @Override
    public List<StockRuptureProjection> getStockRuptureAlerts() {
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

        List<StockRuptureProjection> projections = new ArrayList<>();
        for (Object[] row : results) {
            projections.add(new StockRuptureProjection(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).intValue(),
                ((Number) row[4]).intValue()
            ));
        }

        return projections;
    }

    @Override
    public int getExpiringProductsCount(int days) {
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

    @Override
    public List<ExpiryAlertProjection> getExpiryAlerts(int days) {
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

        List<ExpiryAlertProjection> projections = new ArrayList<>();
        for (Object[] row : results) {
            projections.add(new ExpiryAlertProjection(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                (String) row[2],
                (String) row[3],
                ((java.sql.Date) row[4]).toLocalDate(),
                ((Number) row[5]).intValue(),
                ((Number) row[6]).intValue()
            ));
        }

        return projections;
    }

    @Override
    public long getCashDiscrepancyAmount(LocalDate date) {
        String sql = """
            SELECT COALESCE(SUM(ABS(
                t.totalamount - COALESCE(cr.final_amount, 0)
            )), 0)
            FROM cash_register cr
            INNER JOIN ticketing t ON t.cash_register_id = cr.id
            WHERE DATE(cr.end_time) = :date
              AND cr.statut = 'CLOSED'
              AND ABS(t.totalamount - COALESCE(cr.final_amount, 0)) > 0
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public int getOverdueInvoicesCount(int days) {
        String sql = """
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

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("notPaid", InvoiceStatut.NOT_PAID.name());
        query.setParameter("partiallyPaid", InvoiceStatut.PARTIALLY_PAID.name());
        query.setParameter("days", days);
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<OverdueInvoiceProjection> getOverdueInvoiceAlerts(int days) {
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

        List<OverdueInvoiceProjection> projections = new ArrayList<>();
        for (Object[] row : results) {
            projections.add(new OverdueInvoiceProjection(
                ((Number) row[0]).longValue(),
                ((java.sql.Date) row[1]).toLocalDate(),
                (String) row[2],
                (String) row[3],
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).intValue()
            ));
        }

        return projections;
    }
}
