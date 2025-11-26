package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO;
import java.util.List;
import java.util.Map;

public interface CustomerSegmentationReportService {
    /**
     * Get all customer segmentation data using RFM analysis
     *
     * @return List of customer segmentation records
     */
    List<CustomerSegmentationDTO> getAllCustomerSegmentation();

    /**
     * Get customers filtered by classification
     *
     * @param classification The customer classification to filter by
     * @return List of customers for the classification
     */
    List<CustomerSegmentationDTO> getCustomersByClassification(CustomerSegmentationDTO.CustomerClassification classification);

    /**
     * Get champion customers (highest RFM scores)
     *
     * @return List of champion customers
     */
    List<CustomerSegmentationDTO> getChampionCustomers();

    /**
     * Get at-risk customers (need attention)
     *
     * @return List of at-risk customers
     */
    List<CustomerSegmentationDTO> getAtRiskCustomers();

    /**
     * Get count of customers by classification
     *
     * @return Map of classification to count
     */
    Map<CustomerSegmentationDTO.CustomerClassification, Long> getCustomerCountByClassification();

    /**
     * Get customer segmentation for a specific customer
     *
     * @param customerId The customer ID
     * @return Customer segmentation data
     */
    CustomerSegmentationDTO getCustomerSegmentation(Integer customerId);

    /**
     * Export customer segmentation report as PDF
     *
     * @return PDF bytes
     */
    byte[] exportCustomerSegmentationToPdf();
}
