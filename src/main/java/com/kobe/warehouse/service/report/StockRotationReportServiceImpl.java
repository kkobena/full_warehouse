package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@Transactional(readOnly = true)
public class StockRotationReportServiceImpl implements StockRotationReportService {


    private final EntityManager entityManager;

    public StockRotationReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'all'")
    public List<StockRotationDTO> getAllStockRotation() {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "stock_quantity, " +
            "unit_cost, " +
            "stock_value, " +
            "ca_last_30_days, " +
            "qty_sold_last_30_days, " +
            "nb_sales_last_30_days, " +
            "ca_last_12_months, " +
            "qty_sold_last_12_months, " +
            "rotation_rate_annual, " +
            "avg_days_in_stock, " +
            "categorie_abc " +
            "FROM mv_stock_rotation " +
            "ORDER BY rotation_rate_annual DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'category:' + #categorie")
    public List<StockRotationDTO> getStockRotationByCategory(String categorie) {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "stock_quantity, " +
            "unit_cost, " +
            "stock_value, " +
            "ca_last_30_days, " +
            "qty_sold_last_30_days, " +
            "nb_sales_last_30_days, " +
            "ca_last_12_months, " +
            "qty_sold_last_12_months, " +
            "rotation_rate_annual, " +
            "avg_days_in_stock, " +
            "categorie_abc " +
            "FROM mv_stock_rotation " +
            "WHERE categorie = :categorie " +
            "ORDER BY rotation_rate_annual DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categorie", categorie);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'abc:' + #categorieABC")
    public List<StockRotationDTO> getStockRotationByABCClassification(CategorieABC categorieABC) {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "stock_quantity, " +
            "unit_cost, " +
            "stock_value, " +
            "ca_last_30_days, " +
            "qty_sold_last_30_days, " +
            "nb_sales_last_30_days, " +
            "ca_last_12_months, " +
            "qty_sold_last_12_months, " +
            "rotation_rate_annual, " +
            "avg_days_in_stock, " +
            "categorie_abc " +
            "FROM mv_stock_rotation " +
            "WHERE categorie_abc = :categorieABC " +
            "ORDER BY rotation_rate_annual DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categorieABC", categorieABC.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    public Map<CategorieABC, Long> getStockRotationCountByABCClassification() {
        String countQuery =
            "SELECT categorie_abc, COUNT(*) as count " + "FROM mv_stock_rotation " + "GROUP BY categorie_abc";

        Query query = entityManager.createNativeQuery(countQuery);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<CategorieABC, Long> counts = new EnumMap<>(CategorieABC.class);

        // Initialize with zeros
        counts.put(CategorieABC.A, 0L);
        counts.put(CategorieABC.B, 0L);
        counts.put(CategorieABC.C, 0L);

        // Fill with actual counts
        for (Object[] row : results) {
            String classificationStr = (String) row[0];
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            try {
                CategorieABC classification = CategorieABC.valueOf(classificationStr);
                counts.put(classification, count);
            } catch (IllegalArgumentException e) {
                // Ignore invalid classifications
            }
        }

        return counts;
    }

    @Override
    public List<StockRotationDTO> getStockRotationPaginated(int page, int size) {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "stock_quantity, " +
            "unit_cost, " +
            "stock_value, " +
            "ca_last_30_days, " +
            "qty_sold_last_30_days, " +
            "nb_sales_last_30_days, " +
            "ca_last_12_months, " +
            "qty_sold_last_12_months, " +
            "rotation_rate_annual, " +
            "avg_days_in_stock, " +
            "categorie_abc " +
            "FROM mv_stock_rotation " +
            "ORDER BY rotation_rate_annual DESC " +
            "LIMIT :size OFFSET :offset";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("size", size);
        query.setParameter("offset", page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'count'")
    public long getStockRotationCount() {
        String sql = "SELECT COUNT(*) FROM mv_stock_rotation";
        Query query = entityManager.createNativeQuery(sql);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<StockRotationDTO> getStockRotationByABCPaginated(CategorieABC categorieABC, int page, int size) {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "stock_quantity, " +
            "unit_cost, " +
            "stock_value, " +
            "ca_last_30_days, " +
            "qty_sold_last_30_days, " +
            "nb_sales_last_30_days, " +
            "ca_last_12_months, " +
            "qty_sold_last_12_months, " +
            "rotation_rate_annual, " +
            "avg_days_in_stock, " +
            "categorie_abc " +
            "FROM mv_stock_rotation " +
            "WHERE categorie_abc = :categorieABC " +
            "ORDER BY rotation_rate_annual DESC " +
            "LIMIT :size OFFSET :offset";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categorieABC", categorieABC.name());
        query.setParameter("size", size);
        query.setParameter("offset", page * size);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    public long getStockRotationCountByABC(CategorieABC categorieABC) {
        String sql = "SELECT COUNT(*) FROM mv_stock_rotation WHERE categorie_abc = :categorieABC";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categorieABC", categorieABC.name());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'slow'")
    public List<StockRotationDTO> getSlowMovingProducts() {
        String sql =
            "SELECT " +
            "produit_id, " +
            "libelle, " +
            "code_cip, " +
            "categorie, " +
            "stock_quantity, " +
            "unit_cost, " +
            "stock_value, " +
            "ca_last_30_days, " +
            "qty_sold_last_30_days, " +
            "nb_sales_last_30_days, " +
            "ca_last_12_months, " +
            "qty_sold_last_12_months, " +
            "rotation_rate_annual, " +
            "avg_days_in_stock, " +
            "categorie_abc " +
            "FROM mv_stock_rotation " +
            "WHERE categorie_abc = 'C' " +
            "ORDER BY stock_value DESC";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }


    private List<StockRotationDTO> mapResultsToDTO(List<Object[]> results) {
        return results
            .stream()
            .map(row -> {
                Integer produitId = row[0] != null ? ((Number) row[0]).intValue() : null;
                String libelle = (String) row[1];
                String codeCip = (String) row[2];
                String categorie = (String) row[3];
                Integer stockQuantity = row[4] != null ? ((Number) row[4]).intValue() : 0;
                Integer unitCost = row[5] != null ? ((Number) row[5]).intValue() : 0;
                Long stockValue = row[6] != null ? ((Number) row[6]).longValue() : 0L;
                Integer caLast30Days = row[7] != null ? ((Number) row[7]).intValue() : 0;
                Integer qtySoldLast30Days = row[8] != null ? ((Number) row[8]).intValue() : 0;
                Integer nbSalesLast30Days = row[9] != null ? ((Number) row[9]).intValue() : 0;
                Integer caLast12Months = row[10] != null ? ((Number) row[10]).intValue() : 0;
                Integer qtySoldLast12Months = row[11] != null ? ((Number) row[11]).intValue() : 0;
                BigDecimal rotationRateAnnual = row[12] != null ? new BigDecimal(row[12].toString()) : BigDecimal.ZERO;
                Integer avgDaysInStock = row[13] != null ? ((Number) row[13]).intValue() : 999;
                String categorieABCStr = (String) row[14];

                CategorieABC categorieABC = categorieABCStr != null
                    ? CategorieABC.valueOf(categorieABCStr)
                    : CategorieABC.C;

                return new StockRotationDTO(
                    produitId,
                    libelle,
                    codeCip,
                    categorie,
                    stockQuantity,
                    unitCost,
                    stockValue,
                    caLast30Days,
                    qtySoldLast30Days,
                    nbSalesLast30Days,
                    caLast12Months,
                    qtySoldLast12Months,
                    rotationRateAnnual,
                    avgDaysInStock,
                    categorieABC
                );
            })
            .toList();
    }
}
