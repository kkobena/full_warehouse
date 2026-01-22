package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.RepartitionTiersPayantParTva;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import com.kobe.warehouse.service.id_generator.InvoiceGenerationCodeGeneratorService;
import com.kobe.warehouse.service.settings.AppConfigurationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;
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
    private final FactureIdGeneratorService factureIdGeneratorService;
    private final InvoiceGenerationCodeGeneratorService invoiceGenerationCodeGeneratorService;

    protected AbstractEditionFactureService(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        FacturationRepository facturationRepository,
        AppConfigurationService appConfigurationService,
        UserService userService,
        FactureIdGeneratorService factureIdGeneratorService,
        InvoiceGenerationCodeGeneratorService invoiceGenerationCodeGeneratorService
    ) {
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.facturationRepository = facturationRepository;
        this.appConfigurationService = appConfigurationService;
        this.userService = userService;
        this.factureIdGeneratorService = factureIdGeneratorService;
        this.invoiceGenerationCodeGeneratorService = invoiceGenerationCodeGeneratorService;
    }

    @Override
    @Transactional
    public FactureEditionResponse createFactureEdition(EditionSearchParams editionSearchParams) {
        LocalDateTime dateCreation = LocalDateTime.now();
        int generationCode = getGenerationCode();
        saveAll(editionSearchParams, dateCreation, generationCode);
        return new FactureEditionResponse(generationCode, false);
    }

    protected abstract Specification<ThirdPartySaleLine> buildCriteria(EditionSearchParams editionSearchParams);

    protected List<ThirdPartySaleLine> getDatas(EditionSearchParams editionSearchParams) {
        return this.thirdPartySaleLineRepository.findAll(this.buildCriteria(editionSearchParams));
    }

    protected Specification<ThirdPartySaleLine> buildFetchSpecification(EditionSearchParams editionSearchParams) {
        Specification<ThirdPartySaleLine> thirdPartySaleLineSpecification = this.thirdPartySaleLineRepository.canceledCriteria();
        thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
            this.thirdPartySaleLineRepository.saleStatutsCriteria(Set.of(SalesStatut.CLOSED))
        );
        thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
            this.thirdPartySaleLineRepository.periodeCriteria(editionSearchParams.startDate(), editionSearchParams.endDate())
        );
        if (editionSearchParams.factureProvisoire()) {
            thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
                this.thirdPartySaleLineRepository.factureProvisoireCriteria()
            );
        } else {
            thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(this.thirdPartySaleLineRepository.notBilledCriteria());
        }

        return thirdPartySaleLineSpecification;
    }

    protected Map<TiersPayant, List<ThirdPartySaleLine>> groupByTiersPayant(List<ThirdPartySaleLine> thirdPartySaleLines) {
        return thirdPartySaleLines.stream().collect(Collectors.groupingBy(t -> t.getClientTiersPayant().getTiersPayant()));
    }

    private void saveAll(EditionSearchParams editionSearchParams, LocalDateTime dateCreation, int generationCode) {
        var year = dateCreation.getYear();
        var lastFactureNumero = getLastFactureNumero();
        AtomicInteger numero = new AtomicInteger(lastFactureNumero);
        List<ThirdPartySaleLine> thirdPartySaleLines = getDatas(editionSearchParams);

        Map<TiersPayant, List<ThirdPartySaleLine>> groupByTiersPayant = groupByTiersPayant(thirdPartySaleLines);
        groupByTiersPayant.forEach((tiersPayant, saleLines) ->
            this.buildAndSaveFacture(
                null,
                tiersPayant,
                saleLines,
                dateCreation,
                year,
                numero.incrementAndGet(),
                generationCode,
                editionSearchParams
            )
        );
    }

    protected String getFactureNumber(int year, int count) {
        return year + "_" + StringUtils.leftPad(count + "", 4, "0");
    }

    protected int getGenerationCode() {
        return Integer.parseInt(
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")).concat(invoiceGenerationCodeGeneratorService.getNextIdAsString())
        );
    }

    protected void buildAndSaveFacture(
        FactureTiersPayant factureGroup,
        TiersPayant tiersPayant,
        List<ThirdPartySaleLine> saleLines,
        LocalDateTime dateCreation,
        int year,
        int lastFactureNumero,
        int generationCode,
        EditionSearchParams editionSearchParams
    ) {
        FactureTiersPayant factureTiersPayant = new FactureTiersPayant()
            .setId(this.factureIdGeneratorService.nextId())
            .setCreated(dateCreation)
            .setGenerationCode(generationCode)
            .setRemiseForfetaire(tiersPayant.getRemiseForfaitaire())
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
            factureTiersPayant.setGroupeFactureTiersPayant(this.facturationRepository.saveAndFlush(factureGroup));
        }
        factureTiersPayant = this.facturationRepository.saveAndFlush(factureTiersPayant);
        List<RepartitionTiersPayantParTva> facturesRepartitions = new ArrayList<>();
        List<RepartitionTiersPayantParTva> finalFacturesRepartitions = new ArrayList<>();

        for (ThirdPartySaleLine saleLine : saleLines) {
            List<RepartitionTiersPayantParTva> repartitions = saleLine.getRepartitions();

            if (!CollectionUtils.isEmpty(repartitions)) {
                for (RepartitionTiersPayantParTva repartition : repartitions) {
                    factureTiersPayant.setMontantTtc(factureTiersPayant.getMontantTtc().add(BigDecimal.valueOf(repartition.montantTtc())));
                    factureTiersPayant.setMontantTva(factureTiersPayant.getMontantTva().add(BigDecimal.valueOf(repartition.montantTva())));
                    factureTiersPayant.setMontantNet(factureTiersPayant.getMontantNet().add(BigDecimal.valueOf(repartition.montantNet())));
                    factureTiersPayant.setMontantHt(factureTiersPayant.getMontantHt().add(BigDecimal.valueOf(repartition.montantHt())));
                    facturesRepartitions.add(repartition);
                }
            }
            saleLine.setFactureTiersPayant(factureTiersPayant);
            factureTiersPayant.getFacturesDetails().add(saleLine);
            thirdPartySaleLineRepository.saveAndFlush(saleLine);
        }
        Map<Integer, List<RepartitionTiersPayantParTva>> repartitionByTva = facturesRepartitions.stream()
            .collect(Collectors.groupingBy(RepartitionTiersPayantParTva::tva));
        repartitionByTva.forEach((tva, repartitions) -> {
            BigDecimal montantTtc = BigDecimal.ZERO;
            BigDecimal montantTva = BigDecimal.ZERO;
            BigDecimal montantNet = BigDecimal.ZERO;
            BigDecimal montantHt = BigDecimal.ZERO;
            for (RepartitionTiersPayantParTva repartition : repartitions) {
                montantTtc = montantTtc.add(BigDecimal.valueOf(repartition.montantTtc()));
                montantTva = montantTva.add(BigDecimal.valueOf(repartition.montantTva()));
                montantNet = montantNet.add(BigDecimal.valueOf(repartition.montantNet()));
                montantHt = montantHt.add(BigDecimal.valueOf(repartition.montantHt()));
            }
            //double montantTtc, double montantTva, double montantNet, double montantHt, int tva
            finalFacturesRepartitions.add(new RepartitionTiersPayantParTva(montantTtc.doubleValue(), montantTva.doubleValue(), montantNet.doubleValue(), montantHt.doubleValue(), tva));
        });
        factureTiersPayant.setRepartitions(finalFacturesRepartitions);
    }

    protected int getLastFactureNumero() {
        String num = this.facturationRepository.findLatestFactureNumber();
        Year lastFactureDate = Year.now();
        int index = 0;
        if (org.springframework.util.StringUtils.hasLength(num)) {
            lastFactureDate = Year.parse(num.split("_")[0]);
            index = Integer.parseInt(num.split("_")[1]);
        }

        if (!lastFactureDate.equals(Year.now()) && this.appConfigurationService.findParamResetInvoiceNumberEveryYear()
            .map(AppConfiguration::getValue)
            .map(Integer::parseInt)
            .filter(v -> v == 1)
            .isPresent()) {
            index = 0;
        }

        return index;
    }
}
