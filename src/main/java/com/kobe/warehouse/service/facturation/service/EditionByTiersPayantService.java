package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class EditionByTiersPayantService extends AbstractEditionFactureService {

    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public EditionByTiersPayantService(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        FacturationRepository facturationRepository,
        AppConfigurationService appConfigurationService,
        UserService userService
    ) {
        super(thirdPartySaleLineRepository, facturationRepository, appConfigurationService, userService);
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
    }

    @Override
    protected Specification<ThirdPartySaleLine> buildCriteria(EditionSearchParams editionSearchParams) {
        if (!CollectionUtils.isEmpty(editionSearchParams.tiersPayantIds())) {
            return super.buildFetchSpecification(editionSearchParams).and(
                this.thirdPartySaleLineRepository.tiersPayantIdsCriteria(editionSearchParams.tiersPayantIds())
            );
        }
        return super.buildFetchSpecification(editionSearchParams);
    }
}