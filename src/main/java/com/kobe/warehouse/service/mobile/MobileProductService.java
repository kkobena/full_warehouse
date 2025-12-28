package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.repository.MobileProductRepository;
import com.kobe.warehouse.repository.MobileProductRepository.LotInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.PriceInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.ProductBasicInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.ProductSalesStatsProjection;
import com.kobe.warehouse.repository.MobileProductRepository.StockInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.StorageStockProjection;
import com.kobe.warehouse.service.dto.mobile.ExpiryStatus;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.LotInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.PriceInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.SalesStats;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.StockInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.StorageStock;
import com.kobe.warehouse.service.dto.mobile.StockStatus;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile product quick info.
 * Uses MobileProductRepository for data access.
 */
@Service
@Transactional(readOnly = true)
public class MobileProductService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileProductService.class);
    private static final int STOCK_LOOKBACK_DAYS = 30;

    private final MobileProductRepository productRepository;
    private final ProduitService produitService;

    public MobileProductService(MobileProductRepository productRepository, ProduitService produitService) {
        this.productRepository = productRepository;
        this.produitService = produitService;
    }

    /**
     * Get quick product information for mobile modal.
     *
     * @param productId Product ID
     * @return Product quick info DTO
     */
    public MobileProductQuickInfoDTO getProductQuickInfo(Integer productId) {
        LOG.debug("Getting product quick info for ID: {}", productId);

        // Get basic product info
        ProductBasicInfoProjection basicInfo = productRepository.getBasicProductInfo(productId);
        if (basicInfo == null) {
            return null;
        }

        // Get stock info
        StockInfoProjection stockInfoProjection = productRepository.getStockInfo(productId);
        StockInfo stockInfo = mapStockInfo(productId, stockInfoProjection);

        // Get price info
        PriceInfoProjection priceInfoProjection = productRepository.getPriceInfo(productId);
        PriceInfo priceInfo = mapPriceInfo(priceInfoProjection);

        // Get lots info
        List<LotInfoProjection> lotProjections = productRepository.getLotInfo(productId);
        List<LotInfo> lots = mapLotInfo(lotProjections);

        // Get sales stats
        LocalDate today = LocalDate.now();
        ProductSalesStatsProjection salesStatsProjection = productRepository.getSalesStats(
            productId, today, today.minusDays(7), today.minusDays(30)
        );
        SalesStats salesStats = mapSalesStats(salesStatsProjection);

        return MobileProductQuickInfoDTO.builder()
            .id(basicInfo.id())
            .name(basicInfo.name())
            .codeCip(basicInfo.codeCip())
            .codeEan(basicInfo.codeEan())
            .stock(stockInfo)
            .price(priceInfo)
            .lots(lots)
            .salesStats(salesStats)
            .supplierName(basicInfo.supplierName())
            .supplierId(basicInfo.supplierId())
            .familyName(basicInfo.familyName())
            .familyId(basicInfo.familyId())
            .build();
    }

    /**
     * Search products by name or code using optimized PostgreSQL function.
     *
     * @param query Search query
     * @param limit Max results
     * @return List of matching products
     */
    public List<ProduitSearch> searchProducts(String query, int limit) {
        return searchProducts(query, 1, limit); // Default magasin = 1
    }

    /**
     * Search products by name or code using optimized PostgreSQL function.
     *
     * @param query Search query
     * @param magasinId Magasin ID for stock lookup
     * @param limit Max results
     * @return List of matching products
     */
    public List<ProduitSearch> searchProducts(String query, int magasinId, int limit) {
        return produitService.searchProducts(query, magasinId, Pageable.ofSize(limit));
    }

    /**
     * Map stock info projection to DTO.
     */
    private StockInfo mapStockInfo(Integer productId, StockInfoProjection projection) {
        // Determine status based on total quantity using enum
        StockStatus stockStatus = StockStatus.fromQuantity(projection.totalQuantity(), projection.minThreshold());

        // Calculate days of stock based on average daily sales
        int daysOfStock = calculateDaysOfStock(productId, projection.totalQuantity());

        // Map storage stocks
        List<StorageStock> storageStocks = projection.storageStocks().stream()
            .map(s -> new StorageStock(
                s.storageId(),
                s.storageName(),
                s.storageType(),
                s.qtyStock(),
                s.qtyUg(),
                s.total()
            ))
            .toList();

        return new StockInfo(
            projection.totalQuantity(),
            projection.totalQtyStock(),
            projection.totalQtyUg(),
            projection.minThreshold(),
            projection.maxThreshold(),
            stockStatus.getCode(),
            stockStatus.getColor(),
            daysOfStock,
            storageStocks
        );
    }

    /**
     * Map price info projection to DTO.
     */
    private PriceInfo mapPriceInfo(PriceInfoProjection projection) {
        return new PriceInfo(
            projection.purchasePrice(),
            projection.sellingPrice(),
            projection.marginPercent(),
            projection.vatRate()
        );
    }

    /**
     * Map lot info projections to DTOs.
     */
    private List<LotInfo> mapLotInfo(List<LotInfoProjection> projections) {
        return projections.stream()
            .map(p -> {
                // Determine expiry status using enum
                ExpiryStatus expiryStatus = ExpiryStatus.fromDaysUntilExpiry(p.daysUntilExpiry());

                return new LotInfo(
                    p.lotId(),
                    p.numLot(),
                    p.expiryDate(),
                    p.quantityReceived(),
                    p.daysUntilExpiry(),
                    expiryStatus.getCode()
                );
            })
            .toList();
    }

    /**
     * Map sales stats projection to DTO.
     */
    private SalesStats mapSalesStats(ProductSalesStatsProjection projection) {
        return new SalesStats(
            projection.todayQty(),
            projection.todayAmount(),
            projection.weekQty(),
            projection.weekAmount(),
            projection.monthQty(),
            projection.monthAmount(),
            projection.avgDailyQty()
        );
    }

    /**
     * Calculate estimated days of stock.
     */
    private int calculateDaysOfStock(Integer productId, int currentStock) {
        if (currentStock == 0) {
            return 0;
        }

        double avgDailyQty = productRepository.getAverageDailyQuantitySold(productId, STOCK_LOOKBACK_DAYS);

        if (avgDailyQty <= 0) {
            return 999; // Essentially infinite
        }

        return (int) Math.floor(currentStock / avgDailyQty);
    }
}
