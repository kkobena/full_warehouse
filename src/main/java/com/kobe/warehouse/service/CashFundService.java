package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface CashFundService {
    CashFund allocateCashFund(double amount, CashFundType cashFundType, User cashRegisterOwner, User user);

    void validateCashFund(CashRegister cashRegister);

    CashFund getLastPendingCashFundByCashRegister(Long cashRegisterId);

    CashFund findById(Long id);

    void updateCashFund(CashFund cashFund, CashRegister cashRegister);

}
