package com.kobe.warehouse.service.facturation.registry;

import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.service.EditionAllService;
import com.kobe.warehouse.service.facturation.service.EditionByGroupTiersService;
import com.kobe.warehouse.service.facturation.service.EditionBySelectionBonsService;
import com.kobe.warehouse.service.facturation.service.EditionBySelectionService;
import com.kobe.warehouse.service.facturation.service.EditionByTiersPayantService;
import com.kobe.warehouse.service.facturation.service.EditionByTypeTiersPayantService;
import com.kobe.warehouse.service.facturation.service.EditionService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FacturationServiceRegistry {

    private final Map<ModeEditionEnum, EditionService> serviceMap = new HashMap<>();

    public FacturationServiceRegistry(
        EditionAllService editionAllService,
        EditionBySelectionService editionBySelectionService,
        EditionBySelectionBonsService editionBySelectionBonsService,
        EditionByTiersPayantService editionByTiersPayantService,
        EditionByTypeTiersPayantService editionByTypeTiersPayantService,
        EditionByGroupTiersService editionByGroupTiersService
    ) {
        registerService(ModeEditionEnum.ALL, editionAllService);
        registerService(ModeEditionEnum.SELECTED, editionBySelectionService);
        registerService(ModeEditionEnum.SELECTION_BON, editionBySelectionBonsService);
        registerService(ModeEditionEnum.TIERS_PAYANT, editionByTiersPayantService);
        registerService(ModeEditionEnum.TYPE, editionByTypeTiersPayantService);
        registerService(ModeEditionEnum.GROUP, editionByGroupTiersService);
    }

    private void registerService(ModeEditionEnum modeEditionEnum, EditionService editionService) {
        serviceMap.put(modeEditionEnum, editionService);
    }

    public EditionService getService(ModeEditionEnum modeEditionEnum) throws GenericError {
        if (!serviceMap.containsKey(modeEditionEnum)) {
            throw new GenericError("Ce mode d'édition n'est pas pris en charge");
        }
        return serviceMap.get(modeEditionEnum);
    }
}
