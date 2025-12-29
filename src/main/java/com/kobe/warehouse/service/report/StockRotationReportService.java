package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import java.util.List;
import java.util.Map;

public interface StockRotationReportService {
    /**
     * Get all stock rotation data
     *
     * @return List of stock rotation records
     */
    List<StockRotationDTO> getAllStockRotation();

    /**
     * Get stock rotation filtered by product category (famille)
     *
     * @param categorie The category name to filter by
     * @return List of stock rotation records for the category
     */
    List<StockRotationDTO> getStockRotationByCategory(String categorie);

    /**
     * Get stock rotation filtered by ABC classification
     *
     * @param categorieABC The ABC classification to filter by (A, B, C)
     * @return List of stock rotation records for the ABC classification
     */
    List<StockRotationDTO> getStockRotationByABCClassification(CategorieABC categorieABC);

    /**
     * Get count of products by ABC classification
     *
     * @return Map of ABC classification to count
     */
    Map<CategorieABC, Long> getStockRotationCountByABCClassification();

    /**
     * Get slow moving products (classification = C)
     *
     * @return List of slow moving products
     */
    List<StockRotationDTO> getSlowMovingProducts();

    /**
     * Get stock rotation data with pagination
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of stock rotation records for the page
     */
    List<StockRotationDTO> getStockRotationPaginated(int page, int size);

    /**
     * Get total count of stock rotation records
     *
     * @return Total count
     */
    long getStockRotationCount();

    /**
     * Get stock rotation by ABC classification with pagination
     *
     * @param categorieABC The ABC classification to filter by
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of stock rotation records for the page
     */
    List<StockRotationDTO> getStockRotationByABCPaginated(CategorieABC categorieABC, int page, int size);

    /**
     * Get count of stock rotation records by ABC classification
     *
     * @param categorieABC The ABC classification to count
     * @return Count of records
     */
    long getStockRotationCountByABC(CategorieABC categorieABC);
}
