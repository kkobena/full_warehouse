package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.service.dto.enumeration.StatGroupBy;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
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

    VenteRecord fetchVenteRecord(Specification<Sales> specification);
    List<VentePeriodeRecord> fetchVentePeriodeRecords(Specification<Sales> specification, StatGroupBy statGroupBy);
    List<VenteByTypeRecord> fetchVenteByTypeRecords(Specification<Sales> specification);
    List<VenteModePaimentRecord> fetchVenteModePaimentRecords(Specification<Sales> specification);
}
