package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
