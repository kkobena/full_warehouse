package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.tiketz.dto.TicketZProjection;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public interface SalePaymentCustomRepository {
    List<TicketZProjection> fetchSalesPayment(Specification<SalePayment> specification);

    List<VenteModePaimentRecord> fetchVenteModePaimentRecords(Specification<SalePayment> specification);
}
