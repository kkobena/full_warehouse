package com.kobe.warehouse.service.cash_register.impl;

import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.repository.CashFundRepository;
import com.kobe.warehouse.service.cash_register.CashFundService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashFundServiceImpl implements CashFundService {

    private final CashFundRepository cashFundRepository;

    public CashFundServiceImpl(CashFundRepository cashFundRepository) {
        this.cashFundRepository = cashFundRepository;
    }

    @Override
    public CashFund allocateCashFund(Integer amount, CashFundType cashFundType, User cashRegisterOwner, User user) {
        CashFund cashFund = createCashFund(amount, cashFundType, cashRegisterOwner, user, CashFundStatut.PENDING);
        return cashFundRepository.save(cashFund);
    }

    @Override
    public CashFund initCashFund(int amount, User user) {
        return createCashFund(amount, CashFundType.MANUAL, user, user, CashFundStatut.VALIDETED);
    }

    private CashFund createCashFund(
        int amount,
        CashFundType cashFundType,
        User cashRegisterOwner,
        User user,
        CashFundStatut cashFundStatut
    ) {
        CashFund cashFund = new CashFund();
        cashFund.setAmount(amount);
        cashFund.setCreated(LocalDateTime.now());
        cashFund.setUpdated(cashFund.getCreated());
        cashFund.setUser(user);
        cashFund.setCashFundType(cashFundType);
        cashFund.setStatut(cashFundStatut);
        cashFund.setValidatedBy(cashRegisterOwner);
        return cashFund;
    }

    @Override
    public void validateCashFund(CashRegister cashRegister) {
        CashFund cashFund = getLastPendingCashFundByCashRegister(cashRegister.getId());
        cashFund.setUpdated(LocalDateTime.now());
        cashFund.setValidatedBy(cashRegister.getUser());
        cashFund.setStatut(CashFundStatut.VALIDETED);
        cashFundRepository.save(cashFund);
    }

    @Override
    public CashFund getLastPendingCashFundByCashRegister(Long cashRegisterId) {
        return cashFundRepository.findOneByCashRegisterIdAndStatut(cashRegisterId, CashRegisterStatut.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public CashFund findById(Long id) {
        return cashFundRepository.getReferenceById(id);
    }

    @Override
    public void updateCashFund(CashFund cashFund, CashRegister cashRegister) {
        cashFund.setCashRegister(cashRegister);
        cashFund.setStatut(CashFundStatut.VALIDETED);
        cashFund.setUpdated(LocalDateTime.now());
        cashFundRepository.save(cashFund);
    }

    @Override
    public CashFund save(CashFund cashFund) {
        return this.cashFundRepository.save(cashFund);
    }
}
