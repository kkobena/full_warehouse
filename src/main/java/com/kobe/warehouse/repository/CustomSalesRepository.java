package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.service.reglement.differe.dto.Differe;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.Solde;
import com.kobe.warehouse.service.tiketz.dto.TicketZCreditProjection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface CustomSalesRepository {
    Page<DiffereItem> getDiffereItems(Specification<Sales> specification, Pageable pageable);

    Page<Differe> getDiffere(Specification<Sales> specification, Pageable pageable);

    DiffereSummary getDiffereSummary(Specification<Sales> specification);

    Solde getSolde(Specification<Sales> specification);

    List<TicketZCreditProjection> getTicketZDifferes(Specification<Sales> specification);
}
