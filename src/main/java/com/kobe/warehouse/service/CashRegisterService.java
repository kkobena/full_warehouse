package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.web.rest.errors.CashRegisterException;

import java.util.Optional;

public interface CashRegisterService {
    Optional<CashRegister> getOpiningCashRegisterByUser(User user) throws CashRegisterException;

    CashRegister openCashRegister(User user, Long cashFundId);

    CashRegister openCashRegister(User user, User cashRegisterOwner);

    boolean checkAutomaticFundAllocation();
    void checkIfCashRegisterIsOpen(User user,User admin) throws  CashRegisterException;
}
