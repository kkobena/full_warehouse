package com.kobe.warehouse.service.cash_register;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface CashFundService {
    CashFund allocateCashFund(Integer amount, CashFundType cashFundType, AppUser cashRegisterOwner, AppUser user);

    CashFund initCashFund(int amount, AppUser user);

    void validateCashFund(CashRegister cashRegister);

    CashFund getLastPendingCashFundByCashRegister(Integer cashRegisterId);

    CashFund findById(Integer id);

    void updateCashFund(CashFund cashFund, CashRegister cashRegister);

    CashFund save(CashFund cashFund);
}
