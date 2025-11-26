package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@Transactional(readOnly = true)
public class SupplierPerformanceReportServiceImpl implements SupplierPerformanceReportService {

    @PersistenceContext
    private EntityManager entityManager;

    private final SpringTemplateEngine templateEngine;

    public SupplierPerformanceReportServiceImpl(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    @Cacheable(value = "supplierPerformance", key = "'all'")
    public List<SupplierPerformanceDTO> getAllSupplierPerformance() {
        String sql =
            "SELECT " +
            "fournisseur_id, " +
            "fournisseur_name, " +
            "fournisseur_code, " +
            "phone, " +
            "mobile, " +
            "nb_orders_last_30_days, " +
            "purchase_amount_last_30_days, " +
            "nb_orders_last_12_months, " +
            "purchase_amount_last_12_months, " +
            "avg_delivery_days, " +
            "min_delivery_days, " +
            "max_delivery_days, " +
            "conformity_rate_pct, " +
            "performance_score " +
            "FROM mv_supplier_performance " +
            "ORDER BY performance_score DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "supplierPerformance", key = "'supplier:' + #fournisseurId")
    public SupplierPerformanceDTO getSupplierPerformance(Integer fournisseurId) {
        String sql =
            "SELECT " +
            "fournisseur_id, " +
            "fournisseur_name, " +
            "fournisseur_code, " +
            "phone, " +
            "mobile, " +
            "nb_orders_last_30_days, " +
            "purchase_amount_last_30_days, " +
            "nb_orders_last_12_months, " +
            "purchase_amount_last_12_months, " +
            "avg_delivery_days, " +
            "min_delivery_days, " +
            "max_delivery_days, " +
            "conformity_rate_pct, " +
            "performance_score " +
            "FROM mv_supplier_performance " +
            "WHERE fournisseur_id = :fournisseurId";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fournisseurId", fournisseurId);

        try {
            Object[] result = (Object[]) query.getSingleResult();
            return mapRowToDTO(result);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @Cacheable(value = "supplierPerformance", key = "'top:' + #limit")
    public List<SupplierPerformanceDTO> getTopSuppliersByVolume(Integer limit) {
        String sql =
            "SELECT " +
            "fournisseur_id, " +
            "fournisseur_name, " +
            "fournisseur_code, " +
            "phone, " +
            "mobile, " +
            "nb_orders_last_30_days, " +
            "purchase_amount_last_30_days, " +
            "nb_orders_last_12_months, " +
            "purchase_amount_last_12_months, " +
            "avg_delivery_days, " +
            "min_delivery_days, " +
            "max_delivery_days, " +
            "conformity_rate_pct, " +
            "performance_score " +
            "FROM mv_supplier_performance " +
            "ORDER BY purchase_amount_last_12_months DESC " +
            "LIMIT :limit";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "supplierPerformance", key = "'score:' + #minScore")
    public List<SupplierPerformanceDTO> getSuppliersByPerformanceScore(Double minScore) {
        String sql =
            "SELECT " +
            "fournisseur_id, " +
            "fournisseur_name, " +
            "fournisseur_code, " +
            "phone, " +
            "mobile, " +
            "nb_orders_last_30_days, " +
            "purchase_amount_last_30_days, " +
            "nb_orders_last_12_months, " +
            "purchase_amount_last_12_months, " +
            "avg_delivery_days, " +
            "min_delivery_days, " +
            "max_delivery_days, " +
            "conformity_rate_pct, " +
            "performance_score " +
            "FROM mv_supplier_performance " +
            "WHERE performance_score >= :minScore " +
            "ORDER BY performance_score DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("minScore", minScore);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "supplierPerformance", key = "'delivery-issues'")
    public List<SupplierPerformanceDTO> getSuppliersWithDeliveryIssues() {
        String sql =
            "SELECT " +
            "fournisseur_id, " +
            "fournisseur_name, " +
            "fournisseur_code, " +
            "phone, " +
            "mobile, " +
            "nb_orders_last_30_days, " +
            "purchase_amount_last_30_days, " +
            "nb_orders_last_12_months, " +
            "purchase_amount_last_12_months, " +
            "avg_delivery_days, " +
            "min_delivery_days, " +
            "max_delivery_days, " +
            "conformity_rate_pct, " +
            "performance_score " +
            "FROM mv_supplier_performance " +
            "WHERE conformity_rate_pct < 95 OR avg_delivery_days > 7 " +
            "ORDER BY conformity_rate_pct ASC, avg_delivery_days DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "supplierPerformance", key = "'summary'")
    public SupplierPerformanceSummaryDTO getSupplierPerformanceSummary() {
        String sql =
            "SELECT " +
            "COUNT(*) as total_suppliers, " +
            "SUM(purchase_amount_last_12_months) as total_purchase_12m, " +
            "SUM(purchase_amount_last_30_days) as total_purchase_30d, " +
            "SUM(nb_orders_last_12_months) as total_orders_12m, " +
            "SUM(nb_orders_last_30_days) as total_orders_30d, " +
            "AVG(avg_delivery_days) as avg_delivery, " +
            "AVG(conformity_rate_pct) as avg_conformity, " +
            "COUNT(*) FILTER (WHERE performance_score >= 70) as good_performers, " +
            "COUNT(*) FILTER (WHERE performance_score >= 50 AND performance_score < 70) as avg_performers, " +
            "COUNT(*) FILTER (WHERE performance_score < 50) as poor_performers " +
            "FROM mv_supplier_performance";

        Query query = entityManager.createNativeQuery(sql);
        Object[] result = (Object[]) query.getSingleResult();

        Integer totalSuppliers = result[0] != null ? ((Number) result[0]).intValue() : 0;
        Long totalPurchase12m = result[1] != null ? ((Number) result[1]).longValue() : 0L;
        Long totalPurchase30d = result[2] != null ? ((Number) result[2]).longValue() : 0L;
        Integer totalOrders12m = result[3] != null ? ((Number) result[3]).intValue() : 0;
        Integer totalOrders30d = result[4] != null ? ((Number) result[4]).intValue() : 0;
        BigDecimal avgDelivery = result[5] != null ? new BigDecimal(result[5].toString()) : BigDecimal.ZERO;
        BigDecimal avgConformity = result[6] != null ? new BigDecimal(result[6].toString()) : BigDecimal.ZERO;
        Integer goodPerformers = result[7] != null ? ((Number) result[7]).intValue() : 0;
        Integer avgPerformers = result[8] != null ? ((Number) result[8]).intValue() : 0;
        Integer poorPerformers = result[9] != null ? ((Number) result[9]).intValue() : 0;

        return new SupplierPerformanceSummaryDTO(
            totalSuppliers,
            totalPurchase12m,
            totalPurchase30d,
            totalOrders12m,
            totalOrders30d,
            avgDelivery,
            avgConformity,
            goodPerformers,
            avgPerformers,
            poorPerformers
        );
    }

    @Override
    public byte[] exportSupplierPerformanceToPdf() {
        // Get the performance data
        List<SupplierPerformanceDTO> suppliers = getAllSupplierPerformance();
        SupplierPerformanceSummaryDTO summary = getSupplierPerformanceSummary();

        // Prepare Thymeleaf context
        Context context = new Context();
        context.setVariable("suppliers", suppliers);
        context.setVariable("summary", summary);
        context.setVariable("reportTitle", "Rapport de Performance Fournisseurs");
        context.setVariable("page_count", "1/1");

        // Generate HTML from template
        String htmlContent = templateEngine.process("reports/supplier-performance/main", context);

        // Convert HTML to PDF
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private List<SupplierPerformanceDTO> mapResultsToDTO(List<Object[]> results) {
        return results.stream().map(this::mapRowToDTO).toList();
    }

    private SupplierPerformanceDTO mapRowToDTO(Object[] row) {
        Integer fournisseurId = row[0] != null ? ((Number) row[0]).intValue() : null;
        String fournisseurName = (String) row[1];
        String fournisseurCode = (String) row[2];
        String phone = (String) row[3];
        String mobile = (String) row[4];
        Integer nbOrders30d = row[5] != null ? ((Number) row[5]).intValue() : 0;
        Long purchaseAmount30d = row[6] != null ? ((Number) row[6]).longValue() : 0L;
        Integer nbOrders12m = row[7] != null ? ((Number) row[7]).intValue() : 0;
        Long purchaseAmount12m = row[8] != null ? ((Number) row[8]).longValue() : 0L;
        Integer avgDeliveryDays = row[9] != null ? ((Number) row[9]).intValue() : 0;
        Integer minDeliveryDays = row[10] != null ? ((Number) row[10]).intValue() : 0;
        Integer maxDeliveryDays = row[11] != null ? ((Number) row[11]).intValue() : 0;
        BigDecimal conformityRate = row[12] != null ? new BigDecimal(row[12].toString()) : BigDecimal.ZERO;
        BigDecimal performanceScore = row[13] != null ? new BigDecimal(row[13].toString()) : BigDecimal.ZERO;

        return new SupplierPerformanceDTO(
            fournisseurId,
            fournisseurName,
            fournisseurCode,
            phone,
            mobile,
            nbOrders30d,
            purchaseAmount30d,
            nbOrders12m,
            purchaseAmount12m,
            avgDeliveryDays,
            minDeliveryDays,
            maxDeliveryDays,
            conformityRate,
            performanceScore
        );
    }
}
