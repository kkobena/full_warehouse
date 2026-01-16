package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;

public interface TaxeService {
    TaxeWrapperDTO fetchTaxe(MvtParam mvtParam, boolean toExport);

    byte[] exportToPdf(MvtParam mvtParam);
}
