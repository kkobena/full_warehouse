package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.SaleForRetourDTO;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RetourClientService {

    SaleForRetourDTO findSaleByRef(String numberTransaction);

    RetourClientDTO validerRetour(RetourClientRequest request);

    Page<RetourClientDTO> findAll(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}
