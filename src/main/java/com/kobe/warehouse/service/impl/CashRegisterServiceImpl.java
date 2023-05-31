package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.CashFundService;
import com.kobe.warehouse.service.CashRegisterService;
import com.kobe.warehouse.web.rest.errors.CashRegisterException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CashRegisterServiceImpl implements CashRegisterService {
    private final CashRegisterRepository cashRegisterRepository;
    private final AppConfigurationService appConfigurationService;
    private final CashFundService cashFundService;

    public CashRegisterServiceImpl(CashRegisterRepository cashRegisterRepository, AppConfigurationService appConfigurationService, CashFundService cashFundService) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.appConfigurationService = appConfigurationService;
        this.cashFundService = cashFundService;
    }

    @Override
    public Optional<CashRegister> getOpiningCashRegisterByUser(User user) throws CashRegisterException {
        List<CashRegister> cashRegisters = cashRegisterRepository.findOneByUserIdAndStatutAndAndBeginTime(user.getId(), CashRegisterStatut.OPEN, LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0, 0)), LocalDateTime.now());
        if (!cashRegisters.isEmpty()) {
            return cashRegisters.stream().sorted(Comparator.comparing(CashRegister::getCreated)).findFirst();
        }
        throw new CashRegisterException();
    }

    @Override
    public CashRegister openCashRegister(User user, Long cashFundId) {
        CashRegister cashRegister = create(user);
        cashRegister.setCashFund(this.cashFundService.findById(cashFundId));
        this.cashRegisterRepository.save(cashRegister);
        this.cashFundService.updateCashFund(cashRegister.getCashFund(), cashRegister);
        return cashRegister;
    }

    @Override
    public CashRegister openCashRegister(User user, User cashRegisterOwner) {
        AppConfiguration appConfiguration = this.appConfigurationService.findOneById(EntityConstant.APP_CASH_FUND).orElse(null);
        CashRegister cashRegister = create(cashRegisterOwner);
        CashFund cashFund = this.cashFundService.allocateCashFund(convertToDouble(appConfiguration.getOtherValue()), CashFundType.AUTO, cashRegisterOwner, user);
        cashFund.setCashRegister(cashRegister);
        cashFund.setStatut(CashRegisterStatut.VALIDETED);
        cashRegister.setCashFund(cashFund);
        this.cashRegisterRepository.save(cashRegister);
        this.cashFundService.updateCashFund(cashFund, cashRegister);
        return cashRegister;
    }

    @Override
    public boolean checkAutomaticFundAllocation() {
        AppConfiguration appConfiguration = this.appConfigurationService.findOneById(EntityConstant.APP_CASH_FUND).orElse(null);
        if (appConfiguration == null) {
            return false;
        } else {
            try {
                return Integer.valueOf(appConfiguration.getValue().trim()) == 1;
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    public void checkIfCashRegisterIsOpen(User user, User admin) throws CashRegisterException {
        try {
            this.getOpiningCashRegisterByUser(user);
        } catch (CashRegisterException e) {
            if (this.checkAutomaticFundAllocation()) {
                this.openCashRegister(admin, user);
            } else {
                throw new CashRegisterException();
            }
        }


    }

    private CashRegister create(User user) {
        CashRegister cashRegister = new CashRegister();
        cashRegister.setBeginTime(LocalDateTime.now());
        cashRegister.setCreated(cashRegister.getBeginTime());
        cashRegister.setUpdated(cashRegister.getCreated());
        cashRegister.setInitAmount(0d);
        cashRegister.setUser(user);
        cashRegister.setStatut(CashRegisterStatut.OPEN);
        return cashRegister;
    }

    private Double convertToDouble(String value) {
        if (!StringUtils.hasLength(value)) return 0d;
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return 0d;
        }

    }
}
