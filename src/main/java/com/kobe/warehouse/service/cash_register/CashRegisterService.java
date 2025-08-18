package com.kobe.warehouse.service.cash_register;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterDTO;
import com.kobe.warehouse.service.cash_register.dto.FetchCashRegisterParams;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.NonClosedCashRegisterException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface CashRegisterService {
    Optional<CashRegister> getOpiningCashRegisterByUser(User user);

    CashRegister getLastOpiningUserCashRegisterByUser(User user);

    CashRegister openCashRegister(User user, Long cashFundId);

    Optional<CashRegisterDTO> openCashRegister(int cashFundAmount) throws NonClosedCashRegisterException;

    CashRegister openCashRegister(User user, User cashRegisterOwner);

    boolean checkAutomaticFundAllocation();

    void checkIfCashRegisterIsOpen(User user, User admin) throws CashRegisterException;

    CashRegister getCashRegisterById(Long id);

    Optional<CashRegisterDTO> findOne(Long id);

    void buildCashRegisterItems(CashRegister cashRegister);

    Page<CashRegisterDTO> fetchCashRegisters(FetchCashRegisterParams fetchCashRegisterParams, Pageable pageable);

    void save(CashRegister cashRegister);

    List<CashRegisterDTO> getConnectedUserNonClosedCashRegisters();

    default TypeVente getTypeVenteFromLibelle(String type) {
        return Stream.of(TypeVente.values()).filter(typeVente -> typeVente.getValue().equals(type)).findFirst().orElseThrow();
    }

    int getCanceledAmount(CashRegister cashRegister);

    boolean hasOpenCashRegister();

    CashRegister getCashRegister();
}
