package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.PaymentRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.TicketService;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PaymentServiceImpl implements PaymentService {
  private final PaymentRepository paymentRepository;
  private final TicketService ticketService;
  private final PaymentModeRepository paymentModeRepository;

  public PaymentServiceImpl(
      PaymentRepository paymentRepository,
      TicketService ticketService,
      PaymentModeRepository paymentModeRepository) {
    this.paymentRepository = paymentRepository;
    this.ticketService = ticketService;
    this.paymentModeRepository = paymentModeRepository;
  }

  @Override
  public void clonePayment(Payment payment, List<Ticket> tickets, Sales copy) {
    payment.setUpdatedAt(Instant.now());
    payment.setEffectiveUpdateDate(payment.getUpdatedAt());
    payment.setCanceled(true);
    Payment paymentCopy = (Payment) payment.clone();
    paymentCopy.setId(null);
    paymentCopy.setCanceled(true);
    paymentCopy.setCreatedAt(payment.getUpdatedAt());
    paymentCopy.setUser(copy.getUser());
    paymentCopy.setDateDimension(copy.getDateDimension());
    paymentCopy.setSales(copy);
    paymentCopy.setMontantVerse(paymentCopy.getMontantVerse() * (-1));
    paymentCopy.setNetAmount(paymentCopy.getNetAmount() * (-1));
    paymentCopy.setPaidAmount(paymentCopy.getPaidAmount() * (-1));
    String paymentTicket = payment.getTicketCode();
    if (paymentTicket != null) {
      for (Ticket ticket : tickets) {
        if (ticket.getCode().equals(paymentTicket)) {
          Ticket ticketCopy = ticketService.cloneTicket(ticket, copy);
          paymentCopy.setTicketCode(ticketCopy.getCode());
          break;
        }
      }
    }
    paymentCopy.setStatut(SalesStatut.REMOVE);
    paymentRepository.save(paymentCopy);
    paymentRepository.save(payment);
  }

  @Override
  public List<Payment> findAllBySalesId(Long id) {
    return paymentRepository.findAllBySalesId(id);
  }

  @Override
  public Set<Payment> buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO, User user) {

    Set<Payment> payments = new HashSet<>();
    saleDTO
        .getPayments()
        .forEach(
            paymentDTO -> {
              Payment payment = buildPaymentFromFromPaymentDTO(sales, paymentDTO, user, null);
              payment.setStatut(SalesStatut.PENDING);
              paymentRepository.save(payment);
              payments.add(payment);
            });
    return payments;
  }

  @Override
  public Set<Payment> buildPaymentFromFromPaymentDTO(
      Sales sales, SaleDTO saleDTO, Ticket ticket, User user) {
    Set<Payment> payments = new HashSet<>();
    removeOldPayment(sales);
    saleDTO
        .getPayments()
        .forEach(
            paymentDTO -> {
              Payment payment = buildPaymentFromFromPaymentDTO(sales, paymentDTO, user, ticket);
              paymentRepository.save(payment);
              payments.add(payment);
            });
    return payments;
  }

  private void removeOldPayment(Sales sales) {
    Set<Payment> payments = sales.getPayments();
    if (!CollectionUtils.isEmpty(payments)) {
      payments.stream()
          .forEach(
              payment -> {
                payment.setSales(null);
                this.paymentRepository.delete(payment);
              });
      sales.setPayments(null);
    }
  }

  @Override
  public void delete(Payment payment) {
    paymentRepository.delete(payment);
  }

  private Payment buildPaymentFromFromPaymentDTO(
      Sales sales, PaymentDTO paymentDTO, User user, Ticket ticket) {
    Payment payment = new Payment();
    payment.setCreatedAt(Instant.now());
    payment.setUpdatedAt(payment.getCreatedAt());
    payment.setEffectiveUpdateDate(payment.getCreatedAt());
    payment.setSales(sales);
    payment.setUser(user);
    payment.setCustomer(sales.getCustomer());
    payment.setDateDimension(sales.getDateDimension());
    if (paymentDTO.getPaymentMode() != null) {
      PaymentMode paymentMode =
          paymentModeRepository.getReferenceById(paymentDTO.getPaymentMode().getCode());
      ModePaimentCode modePaimentCode = ModePaimentCode.valueOf(paymentMode.getCode());
        if (modePaimentCode == ModePaimentCode.CASH) {
            payment.setMontantVerse(paymentDTO.getMontantVerse());
        }
      payment.setPaymentMode(paymentMode);
    }

    payment.setNetAmount(paymentDTO.getNetAmount());
    payment.setPaidAmount(paymentDTO.getPaidAmount());
    if (ticket != null) {
      payment.setTicketCode(ticket.getCode());
    }

    if (sales instanceof ThirdPartySales thirdPartySales) {
        payment.setPartTiersPayant(thirdPartySales.getPartTiersPayant());
      payment.setPartAssure(thirdPartySales.getPartAssure());
    }
    return payment;
  }
}
