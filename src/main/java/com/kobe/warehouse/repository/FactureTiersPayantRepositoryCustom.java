package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import com.kobe.warehouse.service.facturation.dto.InvoiceSearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface FactureTiersPayantRepositoryCustom {
    Page<FactureDto> fetchInvoices(Specification<FactureTiersPayant> specification, Pageable pageable);

    Page<FactureDto> fetchGroupedInvoices(Specification<FactureTiersPayant> specification, Pageable pageable);
}
