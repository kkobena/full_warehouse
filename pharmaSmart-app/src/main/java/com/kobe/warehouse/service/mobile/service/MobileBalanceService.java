package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.mobile.dto.BalanceWrapper;

public interface MobileBalanceService {
    BalanceWrapper getBalanceCaisse(MvtParam mvtParam);
}
