package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;

public interface ReglementService {
    ResponseReglementDTO doReglement(ReglementParam reglementParam) throws CashRegisterException, PaymentAmountException;
}
