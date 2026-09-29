package com.kobe.warehouse.service.reglement;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.service.ReglementFactureModeAllService;
import com.kobe.warehouse.service.reglement.service.ReglementFactureSelectionneesService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeFactureService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeSelectionFactureService;
import com.kobe.warehouse.service.reglement.service.ReglementService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReglementRegistry {

    private final Map<ModeEditionReglement, ReglementService> serviceMap = new HashMap<>();
    private final FacturationRepository facturationRepository;

    public ReglementRegistry(
        ReglementGroupeSelectionFactureService reglementGroupeSelectionFactureService,
        ReglementGroupeFactureService reglementGroupeFactureService,
        ReglementFactureModeAllService reglementFactureModeAllService,
        ReglementFactureSelectionneesService reglementFactureSelectionneesService,
        FacturationRepository facturationRepository
    ) {
        this.facturationRepository = facturationRepository;
        registerService(ModeEditionReglement.GROUPE_TOTAL, reglementGroupeFactureService);
        registerService(ModeEditionReglement.GROUPE_PARTIEL, reglementGroupeSelectionFactureService);
        registerService(ModeEditionReglement.FACTURE_TOTAL, reglementFactureModeAllService);
        registerService(ModeEditionReglement.FACTURE_PARTIEL, reglementFactureSelectionneesService);
    }

    private void registerService(ModeEditionReglement modeEditionReglement, ReglementService reglementService) {
        serviceMap.put(modeEditionReglement, reglementService);
    }

    public ReglementService getService(ModeEditionReglement modeEditionReglement) throws GenericError {
        if (!serviceMap.containsKey(modeEditionReglement)) {
            throw new GenericError("Ce mode de facturation n'est pas pris en charge");
        }
        return serviceMap.get(modeEditionReglement);
    }

    /** Un règlement total de facture de groupe part vers le service des groupes : le rapprochement envoie toujours FACTURE_TOTAL. */
    public ReglementService getService(ReglementParam reglementParam) throws GenericError {
        ModeEditionReglement mode = reglementParam.getMode();
        FactureItemId id = reglementParam.getId();
        if (mode == ModeEditionReglement.FACTURE_TOTAL
            && Objects.nonNull(id)
            && facturationRepository.existsByIdAndInvoiceDateAndGroupeTiersPayantIsNotNull(id.getId(), id.getInvoiceDate())) {
            mode = ModeEditionReglement.GROUPE_TOTAL;
        }
        return getService(mode);
    }
}
