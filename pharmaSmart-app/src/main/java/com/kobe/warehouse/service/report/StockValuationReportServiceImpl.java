package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.MvStockValuationByRayonView;
import com.kobe.warehouse.domain.MvStockValuationView;
import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.repository.MvStockValuationRayonViewRepository;
import com.kobe.warehouse.repository.MvStockValuationViewRepository;
import com.kobe.warehouse.service.UserService;
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
    private final UserService userService;

    public StockValuationReportServiceImpl(
        EntityManager entityManager,
        MvStockValuationViewRepository mvStockValuationViewRepository,
        MvStockValuationRayonViewRepository mvStockValuationRayonViewRepository,
        UserService userService
    ) {
        this.entityManager = entityManager;
        this.mvStockValuationViewRepository = mvStockValuationViewRepository;
        this.mvStockValuationRayonViewRepository = mvStockValuationRayonViewRepository;
        this.userService = userService;
    }

    public Integer currentMagasinId() {
        return userService.getUser().getMagasin().getId();
    }

    @Override
    public Page<StockValuationView> getStockValuationPaginated(Integer familleProduitId, Integer rayonId, Pageable pageable) {
        Integer magasinId = currentMagasinId();
        boolean filterByFamille = Objects.nonNull(familleProduitId) && familleProduitId != 0;
        Pageable pagedBySalesValue = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "totalSalesValue"));

        if (Objects.nonNull(rayonId) && rayonId != 0) {
            Specification<MvStockValuationByRayonView> spec = Specification
                .where(mvStockValuationRayonViewRepository.filterByMagasinId(magasinId))
                .and(mvStockValuationRayonViewRepository.filterByRayonId(rayonId));
            if (filterByFamille) {
                spec = spec.and(mvStockValuationRayonViewRepository.filterByFamilleProduitId(familleProduitId));
            }
            return mvStockValuationRayonViewRepository.findAll(spec, pagedBySalesValue)
                .map(mv -> (StockValuationView) mv);
        }

        Specification<MvStockValuationView> spec = Specification
            .where(mvStockValuationViewRepository.filterByMagasinId(magasinId));
        if (filterByFamille) {
            spec = spec.and(mvStockValuationViewRepository.filterByFamilleProduitId(familleProduitId));
        }
        return mvStockValuationViewRepository.findAll(spec, pagedBySalesValue)
            .map(mv -> (StockValuationView) mv);
    }

    @Override
    public List<StockValuationView> getStockValuation(Integer familleProduitId, Integer rayonId) {
        Integer magasinId = currentMagasinId();
        boolean filterByFamille = Objects.nonNull(familleProduitId) && familleProduitId != 0;
        Sort sortBy = Sort.by(Sort.Direction.DESC, "totalSalesValue");

        if (Objects.nonNull(rayonId) && rayonId != 0) {
            Specification<MvStockValuationByRayonView> spec = Specification
                .where(mvStockValuationRayonViewRepository.filterByMagasinId(magasinId))
                .and(mvStockValuationRayonViewRepository.filterByRayonId(rayonId));
            if (filterByFamille) {
                spec = spec.and(mvStockValuationRayonViewRepository.filterByFamilleProduitId(familleProduitId));
            }
            return mvStockValuationRayonViewRepository.findAll(spec, sortBy)
                .stream().map(mv -> (StockValuationView) mv).toList();
        }

        Specification<MvStockValuationView> spec = Specification
            .where(mvStockValuationViewRepository.filterByMagasinId(magasinId));
        if (filterByFamille) {
            spec = spec.and(mvStockValuationViewRepository.filterByFamilleProduitId(familleProduitId));
        }
        return mvStockValuationViewRepository.findAll(spec, sortBy)
            .stream().map(mv -> (StockValuationView) mv).toList();
    }

    @Override
    @Cacheable(value = "stockValuation", key = "'summary_' + #root.target.currentMagasinId() + '_' + #familleProduitId + '_' + #rayonId")
    public StockValuationSummaryDTO getStockValuationSummary(Integer familleProduitId, Integer rayonId) {
        Integer magasinId = currentMagasinId();
        boolean filterByFamille = Objects.nonNull(familleProduitId) && familleProduitId != 0;
        boolean filterByRayon = Objects.nonNull(rayonId) && rayonId != 0;

        String table = filterByRayon ? "mv_stock_valuation_by_rayon" : "mv_stock_valuation";

        StringBuilder sql = new StringBuilder(
            "SELECT SUM(total_purchase_value), SUM(total_sales_value), SUM(potential_margin), " +
            "AVG(margin_percentage), COUNT(*), SUM(stock_quantity) FROM " + table +
            " WHERE magasin_id = :magasinId"
        );
        if (filterByRayon) sql.append(" AND rayon_id = :rayonId");
        if (filterByFamille) sql.append(" AND categorie_id = :familleProduitId");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("magasinId", magasinId);
        if (filterByRayon) query.setParameter("rayonId", rayonId);
        if (filterByFamille) query.setParameter("familleProduitId", familleProduitId);

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
    @Cacheable(value = "stockValuation", key = "'summary_' + #root.target.currentMagasinId()")
    public StockValuationSummaryDTO getStockValuationSummary() {
        Integer magasinId = currentMagasinId();
        String sql =
            "SELECT SUM(total_purchase_value), SUM(total_sales_value), SUM(potential_margin), " +
            "AVG(margin_percentage), COUNT(*), SUM(stock_quantity) " +
            "FROM mv_stock_valuation WHERE magasin_id = :magasinId";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("magasinId", magasinId);
        Object[] result = (Object[]) query.getSingleResult();

        return new StockValuationSummaryDTO(
            result[0] != null ? ((Number) result[0]).longValue() : 0L,
            result[1] != null ? ((Number) result[1]).longValue() : 0L,
            result[2] != null ? ((Number) result[2]).longValue() : 0L,
            result[3] != null ? new BigDecimal(result[3].toString()) : BigDecimal.ZERO,
            result[4] != null ? ((Number) result[4]).intValue() : 0,
            result[5] != null ? ((Number) result[5]).intValue() : 0
        );
    }
}
