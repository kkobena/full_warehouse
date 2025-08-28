package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public interface TableauPharmacienService extends MvtCommonService {
    TableauPharmacienWrapper getTableauPharmacien(MvtParam mvtParam);

    Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException;

    Resource exportToExcel(MvtParam mvtParam) throws IOException;

    List<GroupeFournisseurDTO> fetchGroupGrossisteToDisplay();

}
