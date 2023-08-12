package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.ProductActivityDTO;
import java.time.LocalDate;
import java.util.List;

public interface ProductActivityService {

  List<ProductActivityDTO> getProductActivity(Long produitId, LocalDate fromDate, LocalDate toDate);
}
