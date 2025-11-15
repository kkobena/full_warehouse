package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.facturation.dto.DossierFactureDto;
import com.kobe.warehouse.service.facturation.dto.DossierFactureProjection;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FacturationDossier;
import com.kobe.warehouse.service.facturation.dto.FacturationGroupeDossier;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import com.kobe.warehouse.service.facturation.dto.FactureDtoWrapper;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.FactureItemDto;
import com.kobe.warehouse.service.facturation.dto.GroupeFactureDto;
import com.kobe.warehouse.service.facturation.dto.InvoiceSearchParams;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.dto.TiersPayantDossierFactureDto;
import com.kobe.warehouse.service.facturation.specification.EditionDataSpecification;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class EditionDataServiceImpl implements EditionDataService {

    private static final Logger log = LoggerFactory.getLogger(EditionDataServiceImpl.class);

    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final FacturationReportService facturationReportService;
    private final GroupeFactureReportService groupeFactureReportService;

    public EditionDataServiceImpl(
        FacturationRepository facturationRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        FacturationReportService facturationReportService,
        GroupeFactureReportService groupeFactureReportService
    ) {
        this.facturationRepository = facturationRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.facturationReportService = facturationReportService;
        this.groupeFactureReportService = groupeFactureReportService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DossierFactureDto> getSales(EditionSearchParams editionSearchParams, Pageable pageable) {
        return this.thirdPartySaleLineRepository.findAll(
                buildFetchSpecification(editionSearchParams),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "sale.updatedAt"))
            ).map(this::fromThirdPartySaleLine);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TiersPayantDossierFactureDto> getEditionData(EditionSearchParams editionSearchParams, Pageable pageable) {
        try {
            if (editionSearchParams.modeEdition() == ModeEditionEnum.GROUP) {
                return this.thirdPartySaleLineRepository.fetchGroup(
                        EditionDataSpecification.aThirdPartySaleLine(editionSearchParams),
                        pageable
                    );
            }
            return this.thirdPartySaleLineRepository.fetch(EditionDataSpecification.aThirdPartySaleLine(editionSearchParams), pageable);
        } catch (Exception e) {
            log.error("Error", e);
            return Page.empty();
        }
    }

    @Override
    public Page<FactureDto> getInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable) {
        try {
            return this.facturationRepository.fetchInvoices(facturationRepository.aFacture(invoiceSearchParams), pageable);
        } catch (Exception e) {
            log.error("Error", e);
            return Page.empty();
        }
    }

    @Override
    public Page<FactureDto> getGroupInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable) {
        try {
            return this.facturationRepository.fetchGroupedInvoices(facturationRepository.aGroupedFacture(invoiceSearchParams), pageable);
        } catch (Exception e) {
            log.error("Error", e);
            return Page.empty();
        }
    }

    @Override
    public void deleteFacture(FactureItemId id) {
        FactureTiersPayant factureTiersPayant = getFactureTiersPayant(id);
        if (Objects.nonNull(factureTiersPayant)) {
            resetThirdPartySaleLines(factureTiersPayant);
            List<FactureTiersPayant> factureTiersPayants = factureTiersPayant.getFactureTiersPayants();
            if (!CollectionUtils.isEmpty(factureTiersPayants)) {
                factureTiersPayants.forEach(this::resetThirdPartySaleLines);
                this.facturationRepository.deleteAll(factureTiersPayants);
            }

            this.facturationRepository.delete(factureTiersPayant);
        }
    }

    private void resetThirdPartySaleLines(FactureTiersPayant factureTiersPayant) {
        List<ThirdPartySaleLine> thirdPartySaleLines = factureTiersPayant.getFacturesDetails();
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            thirdPartySaleLine.setFactureTiersPayant(null);
            thirdPartySaleLine.setUpdated(LocalDateTime.now());
            thirdPartySaleLineRepository.save(thirdPartySaleLine);
        });
    }

    @Override
    public Optional<FactureDtoWrapper> getFacture(FactureItemId id) {
        return Optional.ofNullable(buildFactureDtoWrapper(getFactureTiersPayant(id)));
    }

    @Override
    public void deleteFacture(Set<FactureItemId> ids) {
        List<FactureTiersPayant> factureTiersPayants = this.facturationRepository.findAll(this.facturationRepository.fetchByIs(ids));
        factureTiersPayants.forEach(t -> {
            List<ThirdPartySaleLine> thirdPartySaleLines = t.getFacturesDetails();
            thirdPartySaleLines.forEach(thirdPartySaleLine -> {
                thirdPartySaleLine.setFactureTiersPayant(null);
                thirdPartySaleLine.setUpdated(LocalDateTime.now());
                thirdPartySaleLineRepository.save(thirdPartySaleLine);
            });
            this.facturationRepository.delete(t);
        });
    }

    @Override
    public FactureTiersPayant getFactureTiersPayant(FactureItemId id) {
        return this.facturationRepository.getReferenceById(id);
    }

    @Override
    public List<FactureTiersPayant> getFactureTiersPayant(Integer generationCode, boolean isGroup) {
        String codeStr = String.valueOf(generationCode);
        int yyyymm = Integer.parseInt(codeStr.substring(0, Math.min(6, codeStr.length())));
        var generatedDate = LocalDate.parse(yyyymm + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (isGroup) {
            return this.facturationRepository.findAllByGenerationCodeAndGroupeFactureTiersPayantIsNull(
                    generationCode,
                    generatedDate,
                    Sort.by(Direction.DESC, "created").and(Sort.by(Direction.ASC, "groupeTiersPayant.name"))
                );
        }
        return this.facturationRepository.findAll(
                generationCode,
                generatedDate,
                Sort.by(Direction.DESC, "created").and(Sort.by(Direction.ASC, "tiersPayant.fullName"))
            );
    }

    @Override
    public Resource printToPdf(FactureEditionResponse factureEditionResponse) {
        if (factureEditionResponse.isGroup()) {
            return groupeFactureReportService.printToPdf(
                getFactureTiersPayant(factureEditionResponse.generationCode(), true)
                    .stream()
                    .map(this::buildGroupeFactureDtoFromEntity)
                    .toList()
            );
        }

        return this.facturationReportService.printToPdf(getFactureTiersPayant(factureEditionResponse.generationCode(), false));
    }

    @Override
    public Resource printToPdf(FactureItemId id) {
        FactureTiersPayant factureTiersPayant = getFactureTiersPayant(id);
        if (Objects.nonNull(factureTiersPayant.getTiersPayant())) {
            return this.facturationReportService.printToPdf(factureTiersPayant);
        }
        return this.groupeFactureReportService.printToPdf(buildGroupeFactureDtoFromEntity(factureTiersPayant));
    }

    @Override
    public Page<FacturationGroupeDossier> findGroupeFactureReglementData(FactureItemId id, Pageable pageable) {
        return this.facturationRepository.findGroupeFactureById(id.getId(), id.getInvoiceDate(), pageable);
    }

    @Override
    public Page<FacturationDossier> findFactureReglementData(FactureItemId id, Pageable pageable) {
        return this.facturationRepository.findFacturationDossierByFactureId(id.getId(), id.getInvoiceDate(), pageable);
    }

    @Override
    public DossierFactureProjection findDossierFacture(FactureItemId id, boolean isGroup) {
        if (isGroup) {
            return this.facturationRepository.findGroupDossierFacture(id.getId(), id.getInvoiceDate());
        }
        return this.facturationRepository.findSingleDossierFacture(id.getId(), id.getInvoiceDate());
    }

    private Specification<ThirdPartySaleLine> buildFetchSpecification(EditionSearchParams editionSearchParams) {
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

        if (!CollectionUtils.isEmpty(editionSearchParams.groupIds())) {
            thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
                this.thirdPartySaleLineRepository.groupIdsCriteria(editionSearchParams.groupIds())
            );
        }

        if (!CollectionUtils.isEmpty(editionSearchParams.tiersPayantIds())) {
            thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
                this.thirdPartySaleLineRepository.tiersPayantIdsCriteria(editionSearchParams.tiersPayantIds())
            );
        }
        if (!CollectionUtils.isEmpty(editionSearchParams.categorieTiersPayants())) {
            thirdPartySaleLineSpecification = thirdPartySaleLineSpecification.and(
                this.thirdPartySaleLineRepository.categorieTiersPayantCriteria(editionSearchParams.categorieTiersPayants())
            );
        }
        return thirdPartySaleLineSpecification;
    }

    private DossierFactureDto fromThirdPartySaleLine(ThirdPartySaleLine thirdPartySaleLine) {
        Sales sales = thirdPartySaleLine.getSale();
        AssuredCustomer assuredCustomer = (AssuredCustomer) sales.getCustomer();
        return new DossierFactureDto()
            .setId(thirdPartySaleLine.getId().getId())
            .setAssuranceSaleId(thirdPartySaleLine.getId())
            .setNumBon(thirdPartySaleLine.getNumBon())
            .setCreatedAt(sales.getUpdatedAt())
            .setMontantBon(thirdPartySaleLine.getMontant())
            .setMontantVente(sales.getSalesAmount())
            .setAssuredCustomer(fromAssuredCustomer(assuredCustomer));
    }

    private AssuredCustomerDTO fromAssuredCustomer(AssuredCustomer assuredCustomer) {
        AssuredCustomerDTO assuredCustomerDTO = new AssuredCustomerDTO();
        assuredCustomerDTO.setId(assuredCustomer.getId());
        assuredCustomerDTO.setFirstName(assuredCustomer.getFirstName());
        assuredCustomerDTO.setLastName(assuredCustomer.getLastName());
        return assuredCustomerDTO;
    }

    private GroupeFactureDto buildGroupeFactureDtoFromEntity(FactureTiersPayant factureTiersPayant) {
        GroupeTiersPayant groupeTiersPayant = factureTiersPayant.getGroupeTiersPayant();
        GroupeFactureDto groupeFactureDto = new GroupeFactureDto();
        groupeFactureDto.setFactureItemId(factureTiersPayant.getId());
        groupeFactureDto.setName(groupeTiersPayant.getName());
        groupeFactureDto.setTelephone(groupeTiersPayant.getTelephone());
        groupeFactureDto.setAdresse(groupeTiersPayant.getAdresse());
        groupeFactureDto.setNumFacture(factureTiersPayant.getDisplayNumFacture());
        groupeFactureDto.setCreated(factureTiersPayant.getCreated());
        List<FactureTiersPayant> factureTiersPayants = factureTiersPayant.getFactureTiersPayants();
        groupeFactureDto.setFacturesTiersPayants(factureTiersPayants);
        List<FactureDto> factures = factureTiersPayants
            .stream()
            .sorted(Comparator.comparing(f -> f.getId().getId()))
            .map(fact -> {
                FactureDto factureDto = new FactureDto();
                factureDto.setFactureItemId(fact.getId());
                TiersPayant tiersPayant = fact.getTiersPayant();
                factureDto.setFactureId(fact.getId().getId());
                factureDto.setTiersPayantName(tiersPayant.getFullName());
                List<ThirdPartySaleLine> thirdPartySaleLines = fact.getFacturesDetails();
                int montant = thirdPartySaleLines.stream().mapToInt(ThirdPartySaleLine::getMontant).sum();
                factureDto.setMontant(montant);
                int count = thirdPartySaleLines.size();
                factureDto.setItemsCount((long) count);
                factureDto.setNumFacture(fact.getDisplayNumFacture());
                groupeFactureDto.setInvoiceTotalAmount(groupeFactureDto.getInvoiceTotalAmount() + montant);
                groupeFactureDto.setItemsBonCount(groupeFactureDto.getItemsBonCount() + count);

                return factureDto;
            })
            .toList();
        groupeFactureDto.getFactures().addAll(factures);
        groupeFactureDto.setInvoiceTotalAmountLetters(NumberUtil.getNumberToWords(groupeFactureDto.getInvoiceTotalAmount()).toUpperCase());
        return groupeFactureDto;
    }

    private FactureDtoWrapper buildFactureDtoWrapper(FactureTiersPayant factureTiersPayant) {
        List<FactureTiersPayant> factureTiersPayants = factureTiersPayant.getFactureTiersPayants();
        if (!CollectionUtils.isEmpty(factureTiersPayants)) {
            return buildGroupeFactureDto(factureTiersPayant);
        } else {
            return buildFactureDto(factureTiersPayant);
        }
    }

    private GroupeFactureDto buildGroupeFactureDto(FactureTiersPayant factureTiersPayant) {
        GroupeTiersPayant groupeTiersPayant = factureTiersPayant.getGroupeTiersPayant();

        GroupeFactureDto groupeFactureDto = new GroupeFactureDto();
        groupeFactureDto.setFactureItemId(factureTiersPayant.getId());
        groupeFactureDto.setDebutPeriode(factureTiersPayant.getDebutPeriode());
        groupeFactureDto.setFinPeriode(factureTiersPayant.getFinPeriode());
        groupeFactureDto.setName(groupeTiersPayant.getName());
        groupeFactureDto.setNumFacture(factureTiersPayant.getDisplayNumFacture());
        groupeFactureDto.setCreated(factureTiersPayant.getCreated());
        List<FactureTiersPayant> factureTiersPayants = factureTiersPayant.getFactureTiersPayants();
        groupeFactureDto.setItemsBonCount(factureTiersPayants.size());
        long remiseVente = 0;
        long montant = 0;
        long montantPaye = 0;
        long montantVente = 0;
        long remiseForfaitaire = 0;
        long montantNet = 0;
        long montantAttendu = 0;
        for (FactureTiersPayant facture : factureTiersPayants) {
            FactureDto factureDto = buildFactureDto(facture);
            montantPaye += factureDto.getMontantRegle();
            montantVente += factureDto.getMontantVente();
            remiseForfaitaire += factureDto.getRemiseForfetaire();
            montant += factureDto.getMontant();
            remiseVente += factureDto.getMontantRemiseVente();
            montantNet += factureDto.getMontantNet();
            montantNet += factureDto.getMontantNet();
            groupeFactureDto.getFactures().add(factureDto);
        }
        groupeFactureDto.setMontantAttendu(montantAttendu);
        groupeFactureDto.setMontantNetVente(montantVente);
        groupeFactureDto.setRemiseForfetaire(remiseForfaitaire);
        groupeFactureDto.setInvoiceTotalAmount(montant);
        groupeFactureDto.setMontantNet(montantNet);
        groupeFactureDto.setMontantRegle(montantPaye);
        groupeFactureDto.setMontantRemiseVente(remiseVente);
        return groupeFactureDto;
    }

    private FactureDto buildFactureDto(FactureTiersPayant factureTiersPayant) {
        FactureDto factureDto = new FactureDto();
        factureDto.setFactureItemId(factureTiersPayant.getId());
        TiersPayant tiersPayant = factureTiersPayant.getTiersPayant();
        factureDto.setFactureId(factureTiersPayant.getId().getId());
        factureDto.setTiersPayantName(tiersPayant.getFullName());
        factureDto.setNumFacture(factureTiersPayant.getDisplayNumFacture());
        factureDto.setDebutPeriode(factureTiersPayant.getDebutPeriode());
        factureDto.setFinPeriode(factureTiersPayant.getFinPeriode());
        factureDto.setCreated(factureTiersPayant.getCreated());
        factureDto.setFactureProvisoire(factureTiersPayant.isFactureProvisoire());
        int remiseVente = 0;
        int montantPaye = 0;
        int montantVente = 0;
        //   boolean isCarnet = false;
        List<ThirdPartySaleLine> thirdPartySaleLines = factureTiersPayant.getFacturesDetails();
        for (ThirdPartySaleLine thirdPartySaleLine : thirdPartySaleLines) {
            ThirdPartySales sales = thirdPartySaleLine.getSale();
            //            if (sales.getNatureVente() == NatureVente.CARNET) {
            //                isCarnet = true;
            //            }
            montantVente += sales.getSalesAmount();
            factureDto.setMontant(Objects.requireNonNullElse(factureDto.getMontant(), 0) + thirdPartySaleLine.getMontant());
            remiseVente += Objects.requireNonNullElse(sales.getDiscountAmount(), 0);
            montantPaye += Objects.requireNonNullElse(thirdPartySaleLine.getMontantRegle(), 0);
            factureDto.getItems().add(buildFromThirdPartySaleLine(thirdPartySaleLine, sales));
        }
        factureDto.setMontantRemiseVente(remiseVente);
        factureDto.setMontantRegle(factureTiersPayant.getMontantRegle());
        factureDto.setRemiseForfetaire(factureTiersPayant.getRemiseForfetaire());

        factureDto.setItemsCount((long) thirdPartySaleLines.size());
        factureDto.setItemMontantRegle(montantPaye);
        factureDto.setMontantNet(factureDto.getMontant() - factureDto.getRemiseForfetaire());
        factureDto.setMontantRestant(factureDto.getMontant() - Objects.requireNonNullElse(factureDto.getMontantRegle(), 0));
        factureDto.setMontantVente(montantVente);
        factureDto.setMontantAttendu(factureDto.getMontantNet());
        return factureDto;
    }

    private FactureItemDto buildFromThirdPartySaleLine(ThirdPartySaleLine thirdPartySaleLine, ThirdPartySales sales) {
        FactureItemDto factureItemDto = new FactureItemDto();
        factureItemDto.setMontantClient(Objects.requireNonNullElse(sales.getPartAssure(), 0));
        factureItemDto.setTaux(thirdPartySaleLine.getTaux());
        factureItemDto.setSaleNumber(sales.getNumberTransaction());
        factureItemDto.setSaleId(sales.getId().getId());
        factureItemDto.setAssuranceSaleId(thirdPartySaleLine.getId());
        factureItemDto.setComppsiteSaleId(sales.getId());
        factureItemDto.setStatut(thirdPartySaleLine.getStatut());
        factureItemDto.setMontantVente(sales.getSalesAmount());
        factureItemDto.setMontantRegle(Objects.requireNonNullElse(thirdPartySaleLine.getMontantRegle(), 0));
        factureItemDto.setMontant(thirdPartySaleLine.getMontant());
        factureItemDto.setNumBon(thirdPartySaleLine.getNumBon());
        factureItemDto.setMontantRemise(Objects.requireNonNullElse(sales.getDiscountAmount(), 0));
        factureItemDto.setCreated(thirdPartySaleLine.getCreated());
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
        AssuredCustomer customer = clientTiersPayant.getAssuredCustomer();
        AssuredCustomerDTO assuredCustomerDTO = buildCustomerInfos(customer, clientTiersPayant.getNum());
        factureItemDto.setCustomer(assuredCustomerDTO);
        factureItemDto.setAyantsDroit(buildCustomerInfos(sales.getAyantDroit(), null));

        return factureItemDto;
    }

    private AssuredCustomerDTO buildCustomerInfos(AssuredCustomer customer, String numAssure) {
        if (Objects.isNull(customer)) {
            return null;
        }
        AssuredCustomerDTO assuredCustomerDTO = new AssuredCustomerDTO();
        if (numAssure == null) {
            numAssure = customer.getNumAyantDroit();
        }
        assuredCustomerDTO.setNum(numAssure);
        assuredCustomerDTO.setFirstName(customer.getFirstName());
        assuredCustomerDTO.setLastName(customer.getLastName());
        assuredCustomerDTO.setFullName(customer.getFirstName() + " " + customer.getLastName());
        assuredCustomerDTO.setPhone(customer.getPhone());
        assuredCustomerDTO.setEmail(customer.getEmail());
        assuredCustomerDTO.setNum(customer.getNumAyantDroit());
        assuredCustomerDTO.setDatNaiss(customer.getDatNaiss());
        return assuredCustomerDTO;
    }
}
