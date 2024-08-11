package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import java.net.MalformedURLException;
import org.springframework.core.io.Resource;

public interface TaxeService {
    TaxeWrapperDTO fetchTaxe(MvtParam mvtParam, boolean ignoreSomeTaxe, boolean toExport);

    Resource exportToPdf(MvtParam mvtParam, boolean ignoreSomeTaxe) throws MalformedURLException;
}
