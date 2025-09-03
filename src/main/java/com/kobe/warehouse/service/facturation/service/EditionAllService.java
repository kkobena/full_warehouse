package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EditionAllService extends AbstractEditionFactureService {

    public EditionAllService(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        FacturationRepository facturationRepository,
        AppConfigurationService appConfigurationService,
        UserService userService,
        FactureIdGeneratorService factureIdGeneratorService
    ) {
        super(thirdPartySaleLineRepository, facturationRepository, appConfigurationService, userService, factureIdGeneratorService);
    }

    @Override
    protected Specification<ThirdPartySaleLine> buildCriteria(EditionSearchParams editionSearchParams) {
        return super.buildFetchSpecification(editionSearchParams);
    }
}
