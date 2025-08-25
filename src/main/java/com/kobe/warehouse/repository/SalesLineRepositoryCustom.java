package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.service.dto.OrderBy;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SalesLineRepositoryCustom {
    Page<ProductStatRecord> fetchProductStat(Specification<SalesLine> specification, Long fournisseurId, OrderBy order, Pageable pageable);
}
