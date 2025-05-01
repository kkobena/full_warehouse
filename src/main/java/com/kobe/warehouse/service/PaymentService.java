package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.dto.SaleDTO;
import java.util.List;
import java.util.Set;

public interface PaymentService {
    void clonePayment(SalePayment payment, Sales copy);

    List<SalePayment> findAllBySalesId(Long id);

    Set<SalePayment> buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO, User user);

    void buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO);

    void delete(SalePayment payment);
}
