package com.kobe.warehouse.service.cash_register.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashRegisterItem;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.cash_register.CashFundService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterDTO;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterSpecialisation;
import com.kobe.warehouse.service.cash_register.dto.FetchCashRegisterParams;
import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.web.rest.errors.CashRegisterException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CashRegisterServiceImpl implements CashRegisterService {
  private final CashRegisterRepository cashRegisterRepository;
  private final AppConfigurationService appConfigurationService;
  private final CashFundService cashFundService;

  public CashRegisterServiceImpl(
      CashRegisterRepository cashRegisterRepository,
      AppConfigurationService appConfigurationService,
      CashFundService cashFundService) {
    this.cashRegisterRepository = cashRegisterRepository;
    this.appConfigurationService = appConfigurationService;
    this.cashFundService = cashFundService;
  }

  @Override
  public Optional<CashRegister> getOpiningCashRegisterByUser(User user)
      throws CashRegisterException {
    List<CashRegister> cashRegisters =
        cashRegisterRepository.findOneByUserIdAndStatutAndAndBeginTime(
            user.getId(),
            CashRegisterStatut.OPEN,
            LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0, 0)),
            LocalDateTime.now());
    if (!cashRegisters.isEmpty()) {
      return cashRegisters.stream().max(Comparator.comparing(CashRegister::getCreated));
    }
    return Optional.empty();
    // throw new CashRegisterException();
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
    AppConfiguration appConfiguration =
        this.appConfigurationService.findOneById(EntityConstant.APP_CASH_FUND).orElse(null);
    CashRegister cashRegister = create(cashRegisterOwner);
    CashFund cashFund =
        this.cashFundService.allocateCashFund(
            convertInteger(appConfiguration.getOtherValue()),
            CashFundType.AUTO,
            cashRegisterOwner,
            user);
    cashFund.setCashRegister(cashRegister);
    cashFund.setStatut(CashFundStatut.VALIDETED);
    cashRegister.setCashFund(cashFund);
    this.cashRegisterRepository.save(cashRegister);
    this.cashFundService.updateCashFund(cashFund, cashRegister);
    return cashRegister;
  }

  @Override
  public boolean checkAutomaticFundAllocation() {
    AppConfiguration appConfiguration =
        this.appConfigurationService.findOneById(EntityConstant.APP_CASH_FUND).orElse(null);
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

  @Override
  public CashRegister getCashRegisterById(Long id) {
    return this.cashRegisterRepository.getReferenceById(id);
  }

  @Override
  public void buildCashRegisterItems(CashRegister cashRegister) {
    List<CashRegisterSpecialisation> salesData =
        this.cashRegisterRepository.findCashRegisterSalesDataById(
            cashRegister.getId(),
            Set.of(CategorieChiffreAffaire.CA, CategorieChiffreAffaire.CALLEBASE),
            Set.of(SalesStatut.CLOSED));
    List<CashRegisterSpecialisation> mvtData =
        this.cashRegisterRepository.findCashRegisterMvtDataById(
            cashRegister.getId(), Set.of(CategorieChiffreAffaire.CA.ordinal()));
    salesData.addAll(mvtData);
    salesData.stream()
        .collect(Collectors.groupingBy(CashRegisterSpecialisation::getPaymentModeCode))
        .forEach(
            (modePaymentCode, data) -> {
              CashRegisterItem cashRegisterItem = new CashRegisterItem();
              cashRegisterItem.setPaymentMode(fromCode(modePaymentCode));
              cashRegisterItem.setAmount(
                  data.stream()
                      .map(CashRegisterSpecialisation::getPaidAmount)
                      .reduce(new BigDecimal(0), BigDecimal::add)
                      .longValue());
              cashRegisterItem.setCashRegister(cashRegister);
              cashRegister.getCashRegisterItems().add(cashRegisterItem);
            });
  }

  @Override
  public Page<CashRegisterDTO> fetchCashRegisters(
      FetchCashRegisterParams fetchCashRegisterParams, Pageable pageable) {
    return loadCashRegisters(fetchCashRegisterParams, pageable).map(CashRegisterDTO::new);
  }

  private Page<CashRegister> loadCashRegisters(
      FetchCashRegisterParams fetchCashRegisterParams, Pageable pageable) {
    Specification<CashRegister> cashRegisterSpecification =
        Specification.where(
            this.cashRegisterRepository.specialisation(fetchCashRegisterParams.statuts()));
    if (Objects.nonNull(fetchCashRegisterParams.userId())) {
      cashRegisterSpecification =
          cashRegisterSpecification.and(
              this.cashRegisterRepository.specialisation(fetchCashRegisterParams.userId()));
    }
    if (Objects.nonNull(fetchCashRegisterParams.fromDate())
        && Objects.nonNull(fetchCashRegisterParams.toDate())) {
      var fromDate = LocalDateTime.of(fetchCashRegisterParams.fromDate(), LocalTime.MIN);
      var toDate = LocalDateTime.of(fetchCashRegisterParams.toDate(), LocalTime.MAX);
      cashRegisterSpecification =
          cashRegisterSpecification.and(
              this.cashRegisterRepository.specialisation(fromDate, toDate));
    }
    if (StringUtils.hasLength(fetchCashRegisterParams.beginTime())) {
      LocalDateTime begin = DateUtil.fromString(fetchCashRegisterParams.beginTime());
      cashRegisterSpecification =
          cashRegisterSpecification.and(this.cashRegisterRepository.specialisationBeginTime(begin));

      if (StringUtils.hasLength(fetchCashRegisterParams.endTime())) {
        LocalDateTime end = DateUtil.fromString(fetchCashRegisterParams.endTime());
        cashRegisterSpecification =
            cashRegisterSpecification.and(this.cashRegisterRepository.specialisationEndTime(end));
      } else {
        cashRegisterSpecification =
            cashRegisterSpecification.and(
                this.cashRegisterRepository.specialisationEndTime(LocalDateTime.now()));
      }
    }
    return this.cashRegisterRepository.findAll(cashRegisterSpecification, pageable);
  }

  private PaymentMode fromCode(String code) {
    return new PaymentMode().code(code);
  }

  private CashRegister create(User user) {
    CashRegister cashRegister = new CashRegister();
    cashRegister.setBeginTime(LocalDateTime.now());
    cashRegister.setCreated(cashRegister.getBeginTime());
    cashRegister.setUpdated(cashRegister.getCreated());
    cashRegister.setInitAmount(0L);
    cashRegister.setUser(user);
    cashRegister.setStatut(CashRegisterStatut.OPEN);
    return cashRegister;
  }

  private Integer convertInteger(String value) {
    if (!StringUtils.hasLength(value)) return 0;
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
