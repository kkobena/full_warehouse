package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.util.List;

public interface TableauPharmacienService extends MvtCommonService {
    TableauPharmacienWrapper getTableauPharmacien(MvtParam mvtParam);

    byte[] exportToPdf(MvtParam mvtParam);

    byte[] exportToExcel(MvtParam mvtParam);

    List<GroupeFournisseurDTO> fetchGroupGrossisteToDisplay();
}
