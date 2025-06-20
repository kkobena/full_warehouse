package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.mobile.dto.Tva;

public interface MobileTvaService {
    Tva getTva(MvtParam mvtParam);
}
