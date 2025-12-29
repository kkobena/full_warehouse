package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.StockValuationDTO;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import jakarta.persistence.EntityManager;
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
public class StockValuationReportServiceImpl implements StockValuationReportService {


    private final EntityManager entityManager;

    public StockValuationReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    @Cacheable(value = "stockValuation", key = "'all'")
    public List<StockValuationDTO> getAllStockValuation() {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "storage_location, " +
            "stock_quantity, " +
            "purchase_price, " +
            "sales_price, " +
            "total_purchase_value, " +
            "total_sales_value, " +
            "potential_margin, " +
            "margin_percentage " +
            "FROM mv_stock_valuation " +
            "ORDER BY total_sales_value DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "stockValuation", key = "'category:' + #categorie")
    public List<StockValuationDTO> getStockValuationByCategory(String categorie) {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "storage_location, " +
            "stock_quantity, " +
            "purchase_price, " +
            "sales_price, " +
            "total_purchase_value, " +
            "total_sales_value, " +
            "potential_margin, " +
            "margin_percentage " +
            "FROM mv_stock_valuation " +
            "WHERE categorie = :categorie " +
            "ORDER BY total_sales_value DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categorie", categorie);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "stockValuation", key = "'storage:' + #storageLocation")
    public List<StockValuationDTO> getStockValuationByStorage(String storageLocation) {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "storage_location, " +
            "stock_quantity, " +
            "purchase_price, " +
            "sales_price, " +
            "total_purchase_value, " +
            "total_sales_value, " +
            "potential_margin, " +
            "margin_percentage " +
            "FROM mv_stock_valuation " +
            "WHERE storage_location = :storageLocation " +
            "ORDER BY total_sales_value DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("storageLocation", storageLocation);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    public List<StockValuationDTO> getStockValuationPaginated(int page, int size) {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "storage_location, " +
            "stock_quantity, " +
            "purchase_price, " +
            "sales_price, " +
            "total_purchase_value, " +
            "total_sales_value, " +
            "potential_margin, " +
            "margin_percentage " +
            "FROM mv_stock_valuation " +
            "ORDER BY total_sales_value DESC " +
            "LIMIT :size OFFSET :offset";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("size", size);
        query.setParameter("offset", page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "stockValuation", key = "'count'")
    public long getStockValuationCount() {
        String sql = "SELECT COUNT(*) FROM mv_stock_valuation";
        Query query = entityManager.createNativeQuery(sql);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    @Cacheable(value = "stockValuation", key = "'summary'")
    public StockValuationSummaryDTO getStockValuationSummary() {
        String sql =
            "SELECT " +
            "SUM(total_purchase_value) as total_purchase, " +
            "SUM(total_sales_value) as total_sales, " +
            "SUM(potential_margin) as total_margin, " +
            "AVG(margin_percentage) as avg_margin_pct, " +
            "COUNT(*) as total_products, " +
            "SUM(stock_quantity) as total_quantity " +
            "FROM mv_stock_valuation";

        Query query = entityManager.createNativeQuery(sql);
        Object[] result = (Object[]) query.getSingleResult();

        Long totalPurchaseValue = result[0] != null ? ((Number) result[0]).longValue() : 0L;
        Long totalSalesValue = result[1] != null ? ((Number) result[1]).longValue() : 0L;
        Long totalPotentialMargin = result[2] != null ? ((Number) result[2]).longValue() : 0L;
        BigDecimal avgMarginPercentage = result[3] != null ? new BigDecimal(result[3].toString()) : BigDecimal.ZERO;
        Integer totalProducts = result[4] != null ? ((Number) result[4]).intValue() : 0;
        Integer totalQuantity = result[5] != null ? ((Number) result[5]).intValue() : 0;

        return new StockValuationSummaryDTO(
            totalPurchaseValue,
            totalSalesValue,
            totalPotentialMargin,
            avgMarginPercentage,
            totalProducts,
            totalQuantity
        );
    }



    private List<StockValuationDTO> mapResultsToDTO(List<Object[]> results) {
        return results.stream().map(this::mapRowToDTO).toList();
    }

    private StockValuationDTO mapRowToDTO(Object[] row) {
        Integer produitId = row[0] != null ? ((Number) row[0]).intValue() : null;
        String libelle = (String) row[1];
        String codeCip = (String) row[2];
        String categorie = (String) row[3];
        String storageLocation = (String) row[4];
        Integer stockQuantity = row[5] != null ? ((Number) row[5]).intValue() : 0;
        Integer purchasePrice = row[6] != null ? ((Number) row[6]).intValue() : 0;
        Integer salesPrice = row[7] != null ? ((Number) row[7]).intValue() : 0;
        Long totalPurchaseValue = row[8] != null ? ((Number) row[8]).longValue() : 0L;
        Long totalSalesValue = row[9] != null ? ((Number) row[9]).longValue() : 0L;
        Long potentialMargin = row[10] != null ? ((Number) row[10]).longValue() : 0L;
        BigDecimal marginPercentage = row[11] != null ? new BigDecimal(row[11].toString()) : BigDecimal.ZERO;

        return new StockValuationDTO(
            produitId,
            libelle,
            codeCip,
            categorie,
            storageLocation,
            stockQuantity,
            purchasePrice,
            salesPrice,
            totalPurchaseValue,
            totalSalesValue,
            potentialMargin,
            marginPercentage
        );
    }
}
