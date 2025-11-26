package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO;
import com.kobe.warehouse.service.report.CustomerSegmentationReportService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CustomerSegmentationReportResource {

    private final CustomerSegmentationReportService customerSegmentationReportService;

    public CustomerSegmentationReportResource(CustomerSegmentationReportService customerSegmentationReportService) {
        this.customerSegmentationReportService = customerSegmentationReportService;
    }

    /**
     * GET /customers/segmentation : Get all customer segmentation data using RFM analysis
     *
     * @return List of customer segmentation records
     */
    @GetMapping("/customers/segmentation")
    public ResponseEntity<List<CustomerSegmentationDTO>> getAllCustomerSegmentation() {
        List<CustomerSegmentationDTO> segmentations = customerSegmentationReportService.getAllCustomerSegmentation();
        return ResponseEntity.ok().body(segmentations);
    }

    /**
     * GET /customers/segmentation/classification : Get customers by classification
     *
     * @param classification The customer classification to filter by
     * @return List of customers for the classification
     */
    @GetMapping("/customers/segmentation/classification")
    public ResponseEntity<List<CustomerSegmentationDTO>> getCustomersByClassification(
        @RequestParam CustomerSegmentationDTO.CustomerClassification classification
    ) {
        List<CustomerSegmentationDTO> segmentations = customerSegmentationReportService.getCustomersByClassification(classification);
        return ResponseEntity.ok().body(segmentations);
    }

    /**
     * GET /customers/segmentation/champions : Get champion customers
     *
     * @return List of champion customers
     */
    @GetMapping("/customers/segmentation/champions")
    public ResponseEntity<List<CustomerSegmentationDTO>> getChampionCustomers() {
        List<CustomerSegmentationDTO> champions = customerSegmentationReportService.getChampionCustomers();
        return ResponseEntity.ok().body(champions);
    }

    /**
     * GET /customers/segmentation/at-risk : Get at-risk customers
     *
     * @return List of at-risk customers (AT_RISK and NEED_ATTENTION)
     */
    @GetMapping("/customers/segmentation/at-risk")
    public ResponseEntity<List<CustomerSegmentationDTO>> getAtRiskCustomers() {
        List<CustomerSegmentationDTO> atRisk = customerSegmentationReportService.getAtRiskCustomers();
        return ResponseEntity.ok().body(atRisk);
    }

    /**
     * GET /customers/segmentation/count : Get count of customers by classification
     *
     * @return Map of classification to count
     */
    @GetMapping("/customers/segmentation/count")
    public ResponseEntity<Map<CustomerSegmentationDTO.CustomerClassification, Long>> getCustomerCountByClassification() {
        Map<CustomerSegmentationDTO.CustomerClassification, Long> counts =
            customerSegmentationReportService.getCustomerCountByClassification();
        return ResponseEntity.ok().body(counts);
    }

    /**
     * GET /customers/segmentation/{customerId} : Get customer segmentation for a specific customer
     *
     * @param customerId The customer ID
     * @return Customer segmentation data
     */
    @GetMapping("/customers/segmentation/{customerId}")
    public ResponseEntity<CustomerSegmentationDTO> getCustomerSegmentation(@PathVariable Integer customerId) {
        CustomerSegmentationDTO segmentation = customerSegmentationReportService.getCustomerSegmentation(customerId);
        if (segmentation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(segmentation);
    }

    /**
     * GET /customers/segmentation/export : Export customer segmentation report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/customers/segmentation/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportCustomerSegmentationToPdf() {
        byte[] pdf = customerSegmentationReportService.exportCustomerSegmentationToPdf();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=customer-segmentation.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
