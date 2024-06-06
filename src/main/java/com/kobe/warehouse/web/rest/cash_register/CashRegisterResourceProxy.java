package com.kobe.warehouse.web.rest.cash_register;

import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.TicketingService;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterDTO;
import com.kobe.warehouse.service.cash_register.dto.FetchCashRegisterParams;
import com.kobe.warehouse.service.cash_register.dto.TicketingDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

public class CashRegisterResourceProxy {
  private final CashRegisterService cashRegisterService;
  private final TicketingService ticketingService;

  public CashRegisterResourceProxy(
      CashRegisterService cashRegisterService, TicketingService ticketingService) {
    this.cashRegisterService = cashRegisterService;
    this.ticketingService = ticketingService;
  }

  public ResponseEntity<List<CashRegisterDTO>> getConnectedUserNonClosedCashRegisters() {
    return ResponseEntity.ok().body(cashRegisterService.getConnectedUserNonClosedCashRegisters());
  }

  public ResponseEntity<Void> doTicketing(TicketingDTO ticketingDto) {
    ticketingService.doTicketing(ticketingDto);
    return ResponseEntity.ok().build();
  }

  public ResponseEntity<List<CashRegisterDTO>> fetchCashRegisters(
      FetchCashRegisterParams fetchCashRegisterParams, Pageable pageable) {
    Page<CashRegisterDTO> page =
        cashRegisterService.fetchCashRegisters(fetchCashRegisterParams, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  public ResponseEntity<CashRegisterDTO> getCashRegister(@PathVariable Long id) {

    Optional<CashRegisterDTO> cashRegister = cashRegisterService.findOne(id);
    return ResponseUtil.wrapOrNotFound(cashRegister);
  }

  public ResponseEntity<CashRegisterDTO> openCashRegister(int cashFundAmount) {
    return ResponseUtil.wrapOrNotFound(cashRegisterService.openCashRegister(cashFundAmount));
  }
}
