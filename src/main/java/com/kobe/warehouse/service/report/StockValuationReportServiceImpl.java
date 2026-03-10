package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.MvStockValuationByRayonView;
import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.repository.MvStockValuationRayonViewRepository;
import com.kobe.warehouse.repository.MvStockValuationViewRepository;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class StockValuationReportServiceImpl implements StockValuationReportService {


    private final EntityManager entityManager;
    private final MvStockValuationViewRepository mvStockValuationViewRepository;
    private final MvStockValuationRayonViewRepository mvStockValuationRayonViewRepository;

    public StockValuationReportServiceImpl(EntityManager entityManager, MvStockValuationViewRepository mvStockValuationViewRepository, MvStockValuationRayonViewRepository mvStockValuationRayonViewRepository) {
        this.entityManager = entityManager;
        this.mvStockValuationViewRepository = mvStockValuationViewRepository;
        this.mvStockValuationRayonViewRepository = mvStockValuationRayonViewRepository;
    }


    @Deprecated(forRemoval = true)

    @Cacheable(value = "stockValuation", key = "'all'")
    public List<StockValuationView> getAllStockValuation() {

        return mvStockValuationViewRepository.findAllByOrderByTotalSalesValueDesc().stream()
            .map(mv -> (StockValuationView) mv).toList();
    }


    @Override
    public Page<StockValuationView> getStockValuationPaginated(Integer familleProduitId, Integer rayonId, Pageable pageable) {
        boolean filterByFamille = Objects.nonNull(familleProduitId) && familleProduitId != 0;
        Pageable pagedBySalesValue = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "totalSalesValue"));
        if (Objects.nonNull(rayonId) && rayonId != 0) {
            Specification<MvStockValuationByRayonView> mvStockValuationByRayonViewSpecification = Specification.where(mvStockValuationRayonViewRepository.filterByRayonId(rayonId));
            if (filterByFamille) {
                mvStockValuationByRayonViewSpecification = mvStockValuationByRayonViewSpecification.and(mvStockValuationRayonViewRepository.filterByFamilleProduitId(familleProduitId));
            }
            return mvStockValuationRayonViewRepository.findAll(mvStockValuationByRayonViewSpecification,
                pagedBySalesValue).map(mv -> (StockValuationView) mv);
        }
        if (filterByFamille) {
            return mvStockValuationViewRepository.findAll(Specification.where(mvStockValuationViewRepository.filterByFamilleProduitId(familleProduitId)),
                pagedBySalesValue).map(mv -> (StockValuationView) mv);
        }
        return mvStockValuationViewRepository.findAll(
            pagedBySalesValue).map(mv -> (StockValuationView) mv);
    }


    @Override
    public List<StockValuationView> getStockValuation(Integer familleProduitId, Integer rayonId) {
        boolean filterByFamille = Objects.nonNull(familleProduitId) && familleProduitId != 0;
        Sort sortBy = Sort.by(Sort.Direction.DESC, "totalSalesValue");
        if (Objects.nonNull(rayonId) && rayonId != 0) {
            Specification<MvStockValuationByRayonView> mvStockValuationByRayonViewSpecification = Specification.where(mvStockValuationRayonViewRepository.filterByRayonId(rayonId));
            if (filterByFamille) {
                mvStockValuationByRayonViewSpecification = mvStockValuationByRayonViewSpecification.and(mvStockValuationRayonViewRepository.filterByFamilleProduitId(familleProduitId));
            }
            return mvStockValuationRayonViewRepository.findAll(mvStockValuationByRayonViewSpecification,
                sortBy).stream().map(mv -> (StockValuationView) mv).toList();
        }
        if (filterByFamille) {
            return mvStockValuationViewRepository.findAll(Specification.where(mvStockValuationViewRepository.filterByFamilleProduitId(familleProduitId)),
                sortBy).stream().map(mv -> (StockValuationView) mv).toList();
        }
        return mvStockValuationViewRepository.findAll(
            sortBy).stream().map(mv -> (StockValuationView) mv).toList();
    }

    @Override
    @Cacheable(value = "stockValuation", key = "'summary_' + #familleProduitId + '_' + #rayonId")
    public StockValuationSummaryDTO getStockValuationSummary(Integer familleProduitId, Integer rayonId) {
        boolean filterByFamille = Objects.nonNull(familleProduitId) && familleProduitId != 0;
        boolean filterByRayon = Objects.nonNull(rayonId) && rayonId != 0;

        String table = filterByRayon ? "mv_stock_valuation_by_rayon" : "mv_stock_valuation";

        StringBuilder sql = new StringBuilder(
            "SELECT " +
                "SUM(total_purchase_value), " +
                "SUM(total_sales_value), " +
                "SUM(potential_margin), " +
                "AVG(margin_percentage), " +
                "COUNT(*), " +
                "SUM(stock_quantity) " +
                "FROM " + table
        );

        if (filterByRayon || filterByFamille) {
            sql.append(" WHERE");
            boolean needAnd = false;
            if (filterByRayon) {
                sql.append(" rayon_id = :rayonId");
                needAnd = true;
            }
            if (filterByFamille) {
                if (needAnd) sql.append(" AND");
                sql.append(" categorie_id = :familleProduitId");
            }
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        if (filterByRayon) {
            query.setParameter("rayonId", rayonId);
        }
        if (filterByFamille) {
            query.setParameter("familleProduitId", familleProduitId);
        }

        Object[] result = (Object[]) query.getSingleResult();

        return new StockValuationSummaryDTO(
            result[0] != null ? ((Number) result[0]).longValue() : 0L,
            result[1] != null ? ((Number) result[1]).longValue() : 0L,
            result[2] != null ? ((Number) result[2]).longValue() : 0L,
            result[3] != null ? new BigDecimal(result[3].toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
            result[4] != null ? ((Number) result[4]).intValue() : 0,
            result[5] != null ? ((Number) result[5]).intValue() : 0
        );
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


}
