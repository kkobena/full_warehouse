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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        String sql =
            "SELECT " +
                "produit_id, " +
                "libelle, " +
                "code_cip, " +
                "categorie, " +
                "stock_quantity, " +
                "purchase_price, " +
                "sales_price, " +
                "total_purchase_value, " +
                "total_sales_value, " +
                "potential_margin, " +
                "margin_percentage " +
                "FROM mv_stock_valuation " +
                "ORDER BY total_sales_value DESC";
        return mvStockValuationViewRepository.findAllByOrderByTotalSalesValueDesc().stream()
            .map(mv -> (StockValuationView) mv).toList();
    }


    @Cacheable(value = "stockValuation", key = "'familleProduitId:' + #familleProduitId")
    public List<StockValuationView> getStockValuationByCategory(Integer familleProduitId) {
        String sql =
            "SELECT " +
                "produit_id, " +
                "libelle, " +
                "code_cip, " +
                "categorie, " +
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


        return mvStockValuationViewRepository.findAll(Specification.where(mvStockValuationViewRepository.filterByFamilleProduitId(familleProduitId)),
                Sort.by(Sort.Direction.DESC, "totalSalesValue")).stream()
            .map(mv -> (StockValuationView) mv).toList();
    }


    @Cacheable(value = "stockValuation", key = "'rayon:' + #rayonId")
    public List<StockValuationView> getStockValuationByRayon(Integer rayonId) {
        String sql =
            "SELECT " +
                "produit_id, " +
                "libelle, " +
                "code_cip, " +
                "categorie, " +
                "rayon, " +
                "stock_quantity, " +
                "purchase_price, " +
                "sales_price, " +
                "total_purchase_value, " +
                "total_sales_value, " +
                "potential_margin, " +
                "margin_percentage " +
                "FROM mv_stock_valuation_by_rayon " +
                "WHERE rayonId = :rayonId " +
                "ORDER BY total_sales_value DESC";


        return mvStockValuationRayonViewRepository.findAll(Specification.where(mvStockValuationRayonViewRepository.filterByRayonId(rayonId)),
                Sort.by(Sort.Direction.DESC, "totalSalesValue")).stream()
            .map(mv -> (StockValuationView) mv).toList();
    }


    public Page<StockValuationView> getStockValuationPaginated(Pageable pageable) {
        String sql =
            "SELECT " +
                "produit_id, " +
                "libelle, " +
                "code_cip, " +
                "categorie, " +
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


        return mvStockValuationViewRepository.findAllByOrderByTotalSalesValueDesc(pageable)
            .map(mv -> (StockValuationView) mv);
    }


    public Page<StockValuationView> getStockValuationByRayonPaginated(Pageable pageable) {
        String sql =
            "SELECT " +
                "produit_id, " +
                "libelle, " +
                "code_cip, " +
                "categorie, " +
                "rayon, " +
                "stock_quantity, " +
                "purchase_price, " +
                "sales_price, " +
                "total_purchase_value, " +
                "total_sales_value, " +
                "potential_margin, " +
                "margin_percentage " +
                "FROM mv_stock_valuation_by_rayon " +
                "ORDER BY total_sales_value DESC " +
                "LIMIT :size OFFSET :offset";


        return mvStockValuationRayonViewRepository.findAllByOrderByTotalSalesValueDesc(pageable)
            .map(mv -> (StockValuationView) mv);
    }

    @Override
    @Cacheable(value = "stockValuation", key = "'count'")
    public long getStockValuationCount() {
        String sql = "SELECT COUNT(distinct produit_id) FROM mv_stock_valuation";
        Query query = entityManager.createNativeQuery(sql);
        return ((Number) query.getSingleResult()).longValue();
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
    public List<StockValuationView> getStockValuation(Integer familleProduitId, Integer rayonId){
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
