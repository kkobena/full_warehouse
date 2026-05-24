package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;

public interface BalanceCaisseService extends MvtCommonService {
    BalanceCaisseWrapper getBalanceCaisse(MvtParam mvtParam);

    byte[] exportToPdf(MvtParam mvtParam);
}
