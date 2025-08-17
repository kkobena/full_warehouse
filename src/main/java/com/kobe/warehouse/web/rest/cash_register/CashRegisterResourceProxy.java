package com.kobe.warehouse.web.rest.cash_register;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.TicketingService;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterDTO;
import com.kobe.warehouse.service.cash_register.dto.FetchCashRegisterParams;
import com.kobe.warehouse.service.cash_register.dto.TicketingDTO;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class CashRegisterResourceProxy {

    private final CashRegisterService cashRegisterService;
    private final TicketingService ticketingService;

    public CashRegisterResourceProxy(CashRegisterService cashRegisterService, TicketingService ticketingService) {
        this.cashRegisterService = cashRegisterService;
        this.ticketingService = ticketingService;
    }

    @GetMapping("/cash-registers/connected-user-non-closed-cash-registers")
    public ResponseEntity<List<CashRegisterDTO>> getConnectedUserNonClosedCashRegisters() {
        return ResponseEntity.ok().body(cashRegisterService.getConnectedUserNonClosedCashRegisters());
    }

    @PostMapping("/cash-registers/do-ticketing")
    public ResponseEntity<Void> doTicketing(@Valid @RequestBody TicketingDTO ticketingDto) {
        ticketingService.doTicketing(ticketingDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cash-registers")
    public ResponseEntity<List<CashRegisterDTO>> fetchCashRegisters(
        @RequestParam(required = false, name = "userId") Long userId,
        @RequestParam(required = false, name = "statuts") Set<CashRegisterStatut> statuts,
        @RequestParam(required = false, name = "fromDate") LocalDate fromDate,
        @RequestParam(required = false, name = "toDate") LocalDate toDate,
        @RequestParam(required = false, name = "beginTime") String beginTime,
        @RequestParam(required = false, name = "endTime") String endTime,
        Pageable pageable
    ) {
        Page<CashRegisterDTO> page = cashRegisterService.fetchCashRegisters(
            new FetchCashRegisterParams(userId, statuts, fromDate, toDate, beginTime, endTime),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/cash-registers/{id}")
    public ResponseEntity<CashRegisterDTO> getCashRegister(@PathVariable Long id) {
        Optional<CashRegisterDTO> cashRegister = cashRegisterService.findOne(id);
        return ResponseUtil.wrapOrNotFound(cashRegister);
    }

    @GetMapping("/cash-registers/open-cash-register")
    public ResponseEntity<CashRegisterDTO> openCashRegister(@RequestParam(name = "cashFundAmount") int cashFundAmount) {
        return ResponseUtil.wrapOrNotFound(cashRegisterService.openCashRegister(cashFundAmount));
    }
}
