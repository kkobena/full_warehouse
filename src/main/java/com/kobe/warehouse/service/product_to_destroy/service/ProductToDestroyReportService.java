package com.kobe.warehouse.service.product_to_destroy.service;

import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface ProductToDestroyReportService {
    ResponseEntity<byte[]> generatePdf(
        List<ProductToDestroyDTO> productToDestroys,
        ProductToDestroySumDTO sum,
        LocalDate from,
        LocalDate to
    );
}
