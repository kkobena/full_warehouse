package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientResultDTO;
import com.kobe.warehouse.service.sale.dto.SaleForRetourDTO;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RetourClientService {

    SaleForRetourDTO findSaleByRef(String numberTransaction);

    SaleForRetourDTO findSaleById(Long id, LocalDate saleDate);

    RetourClientResultDTO validerRetour(RetourClientRequest request);

    RetourClientDTO findById(Integer id);

    Page<RetourClientDTO> findAll(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    RetourClientDTO lierVenteEchange(Integer retourId, String saleRef);
}
