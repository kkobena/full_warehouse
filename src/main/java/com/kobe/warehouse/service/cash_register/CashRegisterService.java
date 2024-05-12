package com.kobe.warehouse.service.cash_register;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterDTO;
import com.kobe.warehouse.service.cash_register.dto.FetchCashRegisterParams;
import com.kobe.warehouse.web.rest.errors.CashRegisterException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CashRegisterService {
  Optional<CashRegister> getOpiningCashRegisterByUser(User user);

  CashRegister openCashRegister(User user, Long cashFundId);

  CashRegister openCashRegister(User user, User cashRegisterOwner);

  boolean checkAutomaticFundAllocation();

  void checkIfCashRegisterIsOpen(User user, User admin) throws CashRegisterException;

  CashRegister getCashRegisterById(Long id);

  void buildCashRegisterItems(CashRegister cashRegister);

  Page<CashRegisterDTO> fetchCashRegisters(
      FetchCashRegisterParams fetchCashRegisterParams, Pageable pageable);
}
