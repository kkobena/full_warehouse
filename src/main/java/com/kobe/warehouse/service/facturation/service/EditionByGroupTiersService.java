package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class EditionByGroupTiersService extends AbstractEditionFactureService {

    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final UserService userService;
    private final  FactureIdGeneratorService factureIdGeneratorService;

    public EditionByGroupTiersService(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        FacturationRepository facturationRepository,
        AppConfigurationService appConfigurationService,
        UserService userService,
        FactureIdGeneratorService factureIdGeneratorService
    ) {
        super(thirdPartySaleLineRepository, facturationRepository, appConfigurationService, userService, factureIdGeneratorService);
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.userService = userService;
        this.factureIdGeneratorService = factureIdGeneratorService;
    }

    @Override
    public FactureEditionResponse createFactureEdition(EditionSearchParams editionSearchParams) {
        return buildAndSaveFacturesGroupe(editionSearchParams);
    }

    @Override
    protected Specification<ThirdPartySaleLine> buildCriteria(EditionSearchParams editionSearchParams) {
        if (!CollectionUtils.isEmpty(editionSearchParams.ids())) {
            return super
                .buildFetchSpecification(editionSearchParams)
                .and(this.thirdPartySaleLineRepository.selectionBonCriteria(editionSearchParams.ids()));
        } else if (!CollectionUtils.isEmpty(editionSearchParams.groupIds())) {
            return super
                .buildFetchSpecification(editionSearchParams)
                .and(this.thirdPartySaleLineRepository.groupIdsCriteria(editionSearchParams.groupIds()));
        }
        return super.buildFetchSpecification(editionSearchParams);
    }

    private FactureEditionResponse buildAndSaveFacturesGroupe(EditionSearchParams editionSearchParams) {
        Map<GroupeTiersPayant, FactureTiersPayant> groupeTiersPayantFactureTiersPayantMap = new HashMap<>();
        var dateCreation = LocalDateTime.now();
        var year = dateCreation.getYear();
        var lastFactureNumero = super.getLastFactureNumero();
        AtomicInteger numero = new AtomicInteger(lastFactureNumero);
        List<ThirdPartySaleLine> thirdPartySaleLines = this.getDatas(editionSearchParams);
        Map<TiersPayant, List<ThirdPartySaleLine>> groupByTiersPayant = this.groupByTiersPayant(thirdPartySaleLines);
        groupByTiersPayant.forEach((tiersPayant, saleLines) -> {
            GroupeTiersPayant groupeTiersPayant = tiersPayant.getGroupeTiersPayant();
            FactureTiersPayant factGroupe = groupeTiersPayantFactureTiersPayantMap.get(groupeTiersPayant);
            if (factGroupe == null) {
                factGroupe = buildGroupeFacture(groupeTiersPayant, dateCreation, year, numero.incrementAndGet(), editionSearchParams);
                groupeTiersPayantFactureTiersPayantMap.put(groupeTiersPayant, factGroupe);
            }
            this.buildAndSaveFacture(factGroupe, tiersPayant, saleLines, dateCreation, year, numero.incrementAndGet(), editionSearchParams);
        });

        return new FactureEditionResponse(dateCreation, true);
    }

    private FactureTiersPayant buildGroupeFacture(
        GroupeTiersPayant groupeTiersPayant,
        LocalDateTime dateCreation,
        int year,
        int lastFactureNumero,
        EditionSearchParams editionSearchParams
    ) {
        return new FactureTiersPayant()
            .setId(this.factureIdGeneratorService.nextId())
            .setCreated(dateCreation)
            .setUser(userService.getUser())
            .setUpdated(dateCreation)
            .setDebutPeriode(editionSearchParams.startDate())
            .setFinPeriode(editionSearchParams.endDate())
            .setFactureProvisoire(editionSearchParams.factureProvisoire())
            .setGroupeTiersPayant(groupeTiersPayant)
            .setNumFacture(getFactureNumber(year, lastFactureNumero));
    }
}
