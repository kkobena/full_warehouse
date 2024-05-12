package com.kobe.warehouse.service.cash_register.impl;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Ticketing;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.TicketingService;
import com.kobe.warehouse.service.cash_register.dto.TicketingDTO;
import com.kobe.warehouse.web.rest.errors.CashRegisterException;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TicketingServiceImpl implements TicketingService {
  private final CashRegisterService cashRegisterService;
  private final StorageService storageService;

  public TicketingServiceImpl(
      CashRegisterService cashRegisterService, StorageService storageService) {
    this.cashRegisterService = cashRegisterService;
    this.storageService = storageService;
  }

  @Override
  public void doTicketing(TicketingDTO ticketingDto) {
    CashRegister cashRegister;
    if (Objects.isNull(ticketingDto.cashRegisterId())) {
      cashRegister =
          this.cashRegisterService
              .getOpiningCashRegisterByUser(this.storageService.getUser())
              .orElseThrow();
    } else {
      cashRegister = this.cashRegisterService.getCashRegisterById(ticketingDto.cashRegisterId());
      if (cashRegister.getStatut() == CashRegisterStatut.CLOSED) throw new CashRegisterException();
    }
    Ticketing ticketing = buildTicketing(ticketingDto, cashRegister);
    cashRegister.setFinalAmount(ticketing.getTotalAmount());
    cashRegister.setEndTime(LocalDateTime.now());
    cashRegister.setUpdated(cashRegister.getEndTime());
    cashRegister.setStatut(CashRegisterStatut.CLOSED);
  }

  private Ticketing buildTicketing(TicketingDTO ticketingDto, CashRegister cashRegister) {
    return new Ticketing()
        .setCashRegister(cashRegister)
        .setNumberOf10Thousand(ticketingDto.numberOf10Thousand())
        .setNumberOf5Thousand(ticketingDto.numberOf5Thousand())
        .setNumberOf2Thousand(ticketingDto.numberOf2Thousand())
        .setNumberOf1Thousand(ticketingDto.numberOf1Thousand())
        .setNumberOf500Hundred(ticketingDto.numberOf500Hundred())
        .setNumberOf200Hundred(ticketingDto.numberOf200Hundred())
        .setNumberOf100Hundred(ticketingDto.numberOf100Hundred())
        .setNumberOf50(ticketingDto.numberOf50())
        .setNumberOf25(ticketingDto.numberOf25())
        .setNumberOf10(ticketingDto.numberOf10())
        .setNumberOf5(ticketingDto.numberOf5())
        .setNumberOf1(ticketingDto.numberOf1())
        .setOtherAmount(ticketingDto.otherAmount())
        .setTotalAmount(computeCashAmount(ticketingDto));
  }

  private long computeCashAmount(TicketingDTO ticketingDto) {
    return ticketingDto.otherAmount()
        + ticketingDto.numberOf1()
        + (ticketingDto.numberOf25() * 25L)
        + (ticketingDto.numberOf5() * 5L)
        + (ticketingDto.numberOf10() * 10L)
        + (ticketingDto.numberOf50() * 50L)
        + (ticketingDto.numberOf100Hundred() * 100L)
        + (ticketingDto.numberOf200Hundred() * 200L)
        + (ticketingDto.numberOf500Hundred() * 500L)
        + (ticketingDto.numberOf1Thousand() * 1000L)
        + (ticketingDto.numberOf2Thousand() * 2000L)
        + (ticketingDto.numberOf5Thousand() * 5000L)
        + (ticketingDto.numberOf10Thousand() * 10000L);
  }
}
