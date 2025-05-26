package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public abstract class AbstractEditionFactureService implements EditionService {

    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final FacturationRepository facturationRepository;
    private final AppConfigurationService appConfigurationService;
    private final UserService userService;

    protected AbstractEditionFactureService(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        FacturationRepository facturationRepository,
        AppConfigurationService appConfigurationService,
        UserService userService
    ) {
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.facturationRepository = facturationRepository;
        this.appConfigurationService = appConfigurationService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public FactureEditionResponse createFactureEdition(EditionSearchParams editionSearchParams) {
        LocalDateTime dateCreation = LocalDateTime.now();
        saveAll(editionSearchParams, dateCreation);
        return new FactureEditionResponse(dateCreation, false);
    }

    protected abstract Specification<ThirdPartySaleLine> buildCriteria(
        EditionSearchParams editionSearchParams);

    protected List<ThirdPartySaleLine> getDatas(EditionSearchParams editionSearchParams) {
        return this.thirdPartySaleLineRepository.findAll(this.buildCriteria(editionSearchParams));
    }

    protected Specification<ThirdPartySaleLine> buildFetchSpecification(
        EditionSearchParams editionSearchParams) {
        Specification<ThirdPartySaleLine> thirdPartySaleLineSpecification = Specification.where(
            this.thirdPartySaleLineRepository.canceledCriteria()
        );
        thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
            this.thirdPartySaleLineRepository.saleStatutsCriteria(Set.of(SalesStatut.CLOSED))
        );
        thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
            this.thirdPartySaleLineRepository.periodeCriteria(editionSearchParams.startDate(),
                editionSearchParams.endDate())
        );
        if (editionSearchParams.factureProvisoire()) {
            thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
                this.thirdPartySaleLineRepository.factureProvisoireCriteria()
            );
        } else {
            thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
                this.thirdPartySaleLineRepository.notBilledCriteria());
        }

        return thirdPartySaleLineSpecification;
    }

    protected Map<TiersPayant, List<ThirdPartySaleLine>> groupByTiersPayant(
        List<ThirdPartySaleLine> thirdPartySaleLines) {
        return thirdPartySaleLines.stream()
            .collect(Collectors.groupingBy(t -> t.getClientTiersPayant().getTiersPayant()));
    }

    private void saveAll(EditionSearchParams editionSearchParams, LocalDateTime dateCreation) {
        var year = dateCreation.getYear();
        var lastFactureNumero = getLastFactureNumero();
        AtomicInteger numero = new AtomicInteger(lastFactureNumero);
        List<ThirdPartySaleLine> thirdPartySaleLines = this.getDatas(editionSearchParams);

        Map<TiersPayant, List<ThirdPartySaleLine>> groupByTiersPayant = this.groupByTiersPayant(
            thirdPartySaleLines);
        groupByTiersPayant.forEach((tiersPayant, saleLines) ->
            this.buildAndSaveFacture(null, tiersPayant, saleLines, dateCreation, year,
                numero.incrementAndGet(), editionSearchParams)
        );
    }

    protected String getFactureNumber(int year, int count) {
        return year + "_" + StringUtils.leftPad(count + "", 4, "0");
    }

    protected void buildAndSaveFacture(
        FactureTiersPayant factureGroup,
        TiersPayant tiersPayant,
        List<ThirdPartySaleLine> saleLines,
        LocalDateTime dateCreation,
        int year,
        int lastFactureNumero,
        EditionSearchParams editionSearchParams
    ) {

        FactureTiersPayant factureTiersPayant = new FactureTiersPayant()
            .setCreated(dateCreation)
            .setRemiseForfetaire(Objects.requireNonNullElse(tiersPayant.getRemiseForfaitaire(), 0))
            .setUpdated(dateCreation)
            .setGroupeFactureTiersPayant(factureGroup)
            .setDebutPeriode(editionSearchParams.startDate())
            .setFinPeriode(editionSearchParams.endDate())
            .setFactureProvisoire(editionSearchParams.factureProvisoire())
            .setUser(this.userService.getUser())
            .setGroupeFactureTiersPayant(null)
            .setTiersPayant(tiersPayant)
            .setNumFacture(getFactureNumber(year, lastFactureNumero));
        if (factureGroup != null) {
            factureTiersPayant.setGroupeFactureTiersPayant(
                this.facturationRepository.saveAndFlush(factureGroup));
        }
        factureTiersPayant = this.facturationRepository.saveAndFlush(factureTiersPayant);
        for (ThirdPartySaleLine saleLine : saleLines) {
            saleLine.setFactureTiersPayant(factureTiersPayant);
            factureTiersPayant.getFacturesDetails().add(saleLine);
            thirdPartySaleLineRepository.saveAndFlush(saleLine);
        }
    }

    protected int getLastFactureNumero() {
        if (this.appConfigurationService.findParamResetInvoiceNumberEveryYear().isEmpty()) {
            String num = this.facturationRepository.findLatestFactureNumber();
            if (org.springframework.util.StringUtils.hasLength(num)) {
                return Integer.parseInt(num.split("_")[1]);
            }
        }

        return 0;
    }
}
// est-ce qu'on n'a des endpoint côte ROD pour recuper les informations zone de chanladises
// le pacte
// administrateur
// pour les missions 0:: n le pacte
// modification des droits de type de structure
// les pactes ==> ROD
// les missions ==> appel à l'API rod avec id clavis
// impact exportation des données  avec l'id clavis,inditification des structures
// deux type de structure:: faire acte et faire mission
//faire mission
// les existantes :  acte et mission :: type de structure
// les nouvelles : mission uniquement
// las pactes une liste  de pactes
