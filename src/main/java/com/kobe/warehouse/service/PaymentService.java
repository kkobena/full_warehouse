package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.service.dto.SaleDTO;
import java.util.List;
import java.util.Set;

public interface PaymentService {
    void clonePayment(SalePayment payment, Sales copy);

    List<SalePayment> findAllBySalesId(Long id);

    Set<SalePayment> buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO, AppUser user);

    void buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO);

    void delete(SalePayment payment);

    List<SalePayment> findAllBySales(SaleId id);
}
