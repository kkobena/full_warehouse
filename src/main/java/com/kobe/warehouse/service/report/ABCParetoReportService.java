package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.ClassePareto;
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import java.util.List;

public interface ABCParetoReportService {
    /**
     * Get all ABC Pareto analysis data
     *
     * @return List of ABC Pareto records
     */
    List<ABCParetoDTO> getAllABCParetoAnalysis();

    /**
     * Get ABC Pareto analysis filtered by product category
     *
     * @param categorie The category name to filter by
     * @return List of ABC Pareto records for the category
     */
    List<ABCParetoDTO> getABCParetoByCategory(String categorie);

    /**
     * Get ABC Pareto analysis filtered by Pareto class
     *
     * @param classePareto The Pareto classification to filter by (A, B, C)
     * @return List of ABC Pareto records for the class
     */
    List<ABCParetoDTO> getABCParetoByClass(ClassePareto classePareto);

    /**
     * Get top N products by revenue contribution
     *
     * @param limit Number of products to return
     * @return List of top revenue contributors
     */
    List<ABCParetoDTO> getTopRevenueContributors(int limit);

    /**
     * Get aggregated ABC Pareto summary
     *
     * @return ABC Pareto summary with class distribution
     */
    ABCParetoSummaryDTO getABCParetoSummary();

    /**
     * Get ABC Pareto data with pagination
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of ABC Pareto records for the page
     */
    List<ABCParetoDTO> getABCParetoPaginated(int page, int size);

    /**
     * Get total count of ABC Pareto records
     *
     * @return Total count
     */
    long getABCParetoCount();

    /**
     * Get ABC Pareto by class with pagination
     *
     * @param classePareto The Pareto class to filter by
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of ABC Pareto records for the page
     */
    List<ABCParetoDTO> getABCParetoByClassPaginated(ClassePareto classePareto, int page, int size);

    /**
     * Get count of ABC Pareto records by class
     *
     * @param classePareto The Pareto class to count
     * @return Count of records
     */
    long getABCParetoCountByClass(ClassePareto classePareto);
}
