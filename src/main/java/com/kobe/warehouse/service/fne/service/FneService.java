package com.kobe.warehouse.service.fne.service;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.PlanificationCertificationFne;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.fne.model.CertificationFneResult;
import com.kobe.warehouse.service.fne.model.FneResponse;

public interface FneService {
    FneResponse create(FactureItemId factureItemId) throws GenericError;
    void certifyGroupInvoice(FactureItemId factureItemId) throws GenericError;

    /**
     * Certifie toutes les factures définitives en attente de certification FNE.
     */
    CertificationFneResult certifierFacturesPendantes(PlanificationCertificationFne plan);
}
