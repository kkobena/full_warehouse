package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.service.facturation.dto.DossierFactureDto;
import com.kobe.warehouse.service.facturation.dto.DossierFactureProjection;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FacturationDossier;
import com.kobe.warehouse.service.facturation.dto.FacturationGroupeDossier;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import com.kobe.warehouse.service.facturation.dto.FactureDtoWrapper;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.InvoiceSearchParams;
import com.kobe.warehouse.service.facturation.dto.TiersPayantDossierFactureDto;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EditionDataService {
    Page<DossierFactureDto> getSales(EditionSearchParams editionSearchParams, Pageable pageable);

    Page<TiersPayantDossierFactureDto> getEditionData(EditionSearchParams editionSearchParams, Pageable pageable);

    Page<FactureDto> getInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable);

    Page<FactureDto> getGroupInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable);

    void deleteFacture(Set<FactureItemId> ids);

    void deleteFacture(FactureItemId id);

    Optional<FactureDtoWrapper> getFacture(FactureItemId id);

    FactureTiersPayant getFactureTiersPayant(FactureItemId id);

    List<FactureTiersPayant> getFactureTiersPayant(Integer generationCode, boolean isGroup);

    Resource printToPdf(FactureEditionResponse factureEditionResponse);

    Resource printToPdf(FactureItemId id);

    Page<FacturationGroupeDossier> findGroupeFactureReglementData(FactureItemId id, Pageable pageable);

    Page<FacturationDossier> findFactureReglementData(FactureItemId id, Pageable pageable);

    DossierFactureProjection findDossierFacture(FactureItemId id, boolean isGroup);
}
