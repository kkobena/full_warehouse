package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.dto.SaleDTO;
import java.util.List;
import java.util.Set;

public interface PaymentService {
    void clonePayment(Payment payment, List<Ticket> tickets, Sales copy);

    List<Payment> findAllBySalesId(Long id);

    Set<Payment> buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO, User user);

    void buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO, Ticket ticket, User user);

    void delete(Payment payment);
}
