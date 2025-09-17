package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import org.springframework.core.io.Resource;

import java.net.MalformedURLException;

public interface BalanceCaisseService extends MvtCommonService {

    BalanceCaisseWrapper getBalanceCaisse(MvtParam mvtParam);

    Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException;
}
