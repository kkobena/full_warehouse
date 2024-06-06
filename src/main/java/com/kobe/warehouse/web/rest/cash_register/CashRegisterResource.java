package com.kobe.warehouse.web.rest.cash_register;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.TicketingService;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterDTO;
import com.kobe.warehouse.service.cash_register.dto.FetchCashRegisterParams;
import com.kobe.warehouse.service.cash_register.dto.TicketingDTO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CashRegisterResource extends CashRegisterResourceProxy {

  public CashRegisterResource(
      CashRegisterService cashRegisterService, TicketingService ticketingService) {
    super(cashRegisterService, ticketingService);
  }

  @GetMapping("/cash-registers/connected-user-non-closed-cash-registers")
  public ResponseEntity<List<CashRegisterDTO>> getConnectedUserNonClosedCashRegisters() {
    return super.getConnectedUserNonClosedCashRegisters();
  }

  @PostMapping("/cash-registers/do-ticketing")
  public ResponseEntity<Void> doTicketing(@Valid @RequestBody TicketingDTO ticketingDto) {
    return super.doTicketing(ticketingDto);
  }

  @GetMapping("/cash-registers")
  public ResponseEntity<List<CashRegisterDTO>> fetchCashRegisters(
      @RequestParam(required = false, name = "userId") Long userId,
      @RequestParam(required = false, name = "statuts") Set<CashRegisterStatut> statuts,
      @RequestParam(required = false, name = "fromDate") LocalDate fromDate,
      @RequestParam(required = false, name = "toDate") LocalDate toDate,
      @RequestParam(required = false, name = "beginTime") String beginTime,
      @RequestParam(required = false, name = "endTime") String endTime,
      Pageable pageable) {
    return super.fetchCashRegisters(
        new FetchCashRegisterParams(userId, statuts, fromDate, toDate, beginTime, endTime),
        pageable);
  }

  @GetMapping("/cash-registers/{id}")
  public ResponseEntity<CashRegisterDTO> getCashRegister(@PathVariable Long id) {
    return super.getCashRegister(id);
  }

  @GetMapping("/cash-registers/open-cash-register")
  public ResponseEntity<CashRegisterDTO> openCashRegister(
      @RequestParam(name = "cashFundAmount") int cashFundAmount) {
    return super.openCashRegister(cashFundAmount);
  }
}
