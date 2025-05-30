package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
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
import com.kobe.warehouse.service.facturation.dto.TiersPayantDossierFactureDto;
import com.kobe.warehouse.service.utils.NumberUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class EditionDataServiceImpl implements EditionDataService {

    private static final Logger log = LoggerFactory.getLogger(EditionDataServiceImpl.class);
    private final EntityManager entityManager;
    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final FacturationReportService facturationReportService;
    private final GroupeFactureReportService groupeFactureReportService;

    public EditionDataServiceImpl(
        EntityManager entityManager,
        FacturationRepository facturationRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        FacturationReportService facturationReportService,
        GroupeFactureReportService groupeFactureReportService
    ) {
        this.entityManager = entityManager;
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
        int total = countEdition(editionSearchParams);
        List<TiersPayantDossierFactureDto> list = fetchEditionDataGroupedByTiersPayant(editionSearchParams, pageable);
        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public Page<FactureDto> getInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable) {
        int total = count(buildCountFinalInvoiceQuery(invoiceSearchParams));
        List<FactureDto> list = fetchInvoices(invoiceSearchParams, pageable);
        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public Page<FactureDto> getGroupInvoicies(InvoiceSearchParams invoiceSearchParams, Pageable pageable) {
        int total = count(buildCountFinalGroupInvoiceQuery(invoiceSearchParams));
        List<FactureDto> list = fetchGroupInvoices(invoiceSearchParams, pageable);
        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public void deleteFacture(Long id) {
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
    public Optional<FactureDtoWrapper> getFacture(Long id) {
        return Optional.ofNullable(buildFactureDtoWrapper(getFactureTiersPayant(id)));
    }

    @Override
    public void deleteFacture(Set<Long> ids) {
        List<FactureTiersPayant> factureTiersPayants =
            this.facturationRepository.findAll(this.facturationRepository.fetchByIs(ids));
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
    public FactureTiersPayant getFactureTiersPayant(Long id) {
        return this.facturationRepository.findById(id).orElse(null);
    }

    @Override
    public List<FactureTiersPayant> getFactureTiersPayant(LocalDateTime created, boolean isGroup) {
        if (isGroup) {
            return this.facturationRepository.findAllByCreatedEqualsAndGroupeFactureTiersPayantIsNull(
                    created,
                    Sort.by(Direction.DESC, "created").and(Sort.by(Direction.ASC, "groupeTiersPayant.name"))
                );
        }
        return this.facturationRepository.findAllByCreatedEquals(
                created,
                Sort.by(Direction.DESC, "created").and(Sort.by(Direction.ASC, "tiersPayant.fullName"))
            );
    }

    @Override
    public Resource printToPdf(FactureEditionResponse factureEditionResponse) {
        if (factureEditionResponse.isGroup()) {
            return groupeFactureReportService.printToPdf(
                getFactureTiersPayant(factureEditionResponse.createdDate(), true)
                    .stream()
                    .map(this::buildGroupeFactureDtoFromEntity)
                    .toList()
            );
        }

        return this.facturationReportService.printToPdf(getFactureTiersPayant(factureEditionResponse.createdDate(), false));
    }

    @Override
    public Resource printToPdf(Long id) {
        FactureTiersPayant factureTiersPayant = getFactureTiersPayant(id);
        if (Objects.nonNull(factureTiersPayant.getTiersPayant())) {
            return this.facturationReportService.printToPdf(factureTiersPayant);
        }
        return this.groupeFactureReportService.printToPdf(buildGroupeFactureDtoFromEntity(factureTiersPayant));
    }

    @Override
    public Page<FacturationGroupeDossier> findGroupeFactureReglementData(Long id, Pageable pageable) {
        return this.facturationRepository.findGroupeFactureById(id, pageable);
    }

    @Override
    public Page<FacturationDossier> findFactureReglementData(Long id, Pageable pageable) {
        return this.facturationRepository.findFacturationDossierByFactureId(id, pageable);
    }

    @Override
    public DossierFactureProjection findDossierFacture(Long id, boolean isGroup) {
        if (isGroup) {
            return this.facturationRepository.findGroupDossierFacture(id);
        }
        return this.facturationRepository.findSingleDossierFacture(id);
    }

    private Specification<ThirdPartySaleLine> buildFetchSpecification(EditionSearchParams editionSearchParams) {
        Specification<ThirdPartySaleLine> thirdPartySaleLineSpecification =
            this.thirdPartySaleLineRepository.canceledCriteria()
        ;
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
            .setId(thirdPartySaleLine.getId())
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

    private List<TiersPayantDossierFactureDto> fetchEditionDataGroupedByTiersPayant(
        EditionSearchParams editionSearchParams,
        Pageable pageable
    ) {
        List<TiersPayantDossierFactureDto> factureDtos = new ArrayList<>();
        getEditionDatas(editionSearchParams, pageable).forEach(t ->
            factureDtos.add(
                new TiersPayantDossierFactureDto(
                    t.get("tiersPayantId", Long.class),
                    t.get("tiersPayantName", String.class),
                    t.get("totalAmount", BigDecimal.class),
                    t.get("factureItemCount", Long.class).intValue()
                )
            )
        );
        return factureDtos;
    }

    private List<Tuple> getEditionDatas(EditionSearchParams editionSearchParams, Pageable pageable) {
        try {
            Query q = this.entityManager.createNativeQuery(this.buildFinalQuery(editionSearchParams), Tuple.class);
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
            return q.getResultList();
        } catch (Exception e) {
            log.error("Error", e);
            return List.of();
        }
    }

    private int countEdition(EditionSearchParams editionSearchParams) {
        try {
            Query q = this.entityManager.createNativeQuery(this.buildFinalCountQuery(editionSearchParams));
            return ((Number) q.getSingleResult()).intValue();
        } catch (Exception e) {
            log.error("Error", e);
            return 0;
        }
    }

    private List<Tuple> getTuples(String query, Pageable pageable) {
        try {
            log.info("Query: {}", query);
            Query q = this.entityManager.createNativeQuery(query, Tuple.class);
            if (Objects.nonNull(pageable)) {
                q.setFirstResult((int) pageable.getOffset());
                q.setMaxResults(pageable.getPageSize());
            }

            return q.getResultList();
        } catch (Exception e) {
            log.error("Error", e);
            return List.of();
        }
    }

    private int count(String query) {
        log.info("Count query: {}", query);
        try {
            Query q = this.entityManager.createNativeQuery(query);
            return ((Number) q.getSingleResult()).intValue();
        } catch (Exception e) {
            log.error("Error", e);
            return 0;
        }
    }

    private List<FactureDto> buildInvoicesFromTuples(List<Tuple> tuples, boolean isGrouped) {
        List<FactureDto> factureDtos = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Tuple t : tuples) {
            BigDecimal montantAttendu = t.get("montantAttendu", BigDecimal.class);
            var paidAmount = t.get("montantRegle", Number.class);
            int montantRegle = 0;
            if (paidAmount != null) {
                montantRegle = paidAmount.intValue();
            }
            BigDecimal montantVente = t.get("montantVente", BigDecimal.class);
            BigDecimal montantRemiseVente = t.get("montantRemise", BigDecimal.class);
            LocalDateTime created = t.get("created", Timestamp.class).toLocalDateTime();
            Long itemsCount = t.get("itemsCount", Long.class);
            LocalDate debutPeriode = t.get("debutPeriode", Date.class).toLocalDate();
            LocalDate finPeriode = t.get("finPeriode", Date.class).toLocalDate();
            boolean factureProvisoire = t.get("factureProvisoire", Boolean.class);
            String tiersPayantName = t.get("tiersPayantName", String.class);
            InvoiceStatut statut = InvoiceStatut.valueOf(t.get("statut", String.class));
            String periode = "Du " + debutPeriode.format(formatter) + " au " + finPeriode.format(formatter);
            String numFacture = t.get("numFacture", String.class);
            String[] numFactureParts = numFacture.split("_");
            if (numFactureParts.length > 1) {
                numFacture = numFactureParts[1];
            } else {
                numFacture = numFactureParts[0];
            }
            FactureDto factureDto = new FactureDto()
                .setPeriode(periode)
                .setCreated(created)
                .setDebutPeriode(debutPeriode)
                .setFinPeriode(finPeriode)
                .setStatut(statut)
                .setFactureProvisoire(factureProvisoire)
                .setMontantAttendu(Objects.isNull(montantAttendu) ? 0 : montantAttendu.longValue())
                .setItemsCount(itemsCount)
                .setMontantRegle(Objects.isNull(montantRegle) ? 0 : montantRegle)
                .setMontantVente(Objects.isNull(montantVente) ? 0 : montantVente.longValue())
                .setMontantRemiseVente(Objects.isNull(montantRemiseVente) ? 0 : montantRemiseVente.longValue())
                .setTiersPayantName(tiersPayantName)
                .setNumFacture(numFacture)
                .setFactureId(t.get("factureId", Long.class));
            factureDto.setMontantNet(factureDto.getMontantAttendu()); // TODO: check this
            factureDto.setMontantRestant(factureDto.getMontantAttendu() - factureDto.getMontantRegle()); // TODO: check this
            if (!isGrouped) {
                Integer remiseForfetaire = t.get("remiseForfetaire", Integer.class);
                factureDto.setRemiseForfetaire(remiseForfetaire);
                BigDecimal itemMontantRegle = t.get("itemMontantRegle", BigDecimal.class);
                factureDto.setItemMontantRegle(Objects.isNull(itemMontantRegle) ? 0 : itemMontantRegle.longValue());
                if (StringUtils.hasText(t.get("groupeNumFacture", String.class))) {
                    String groupeNumFacture = t.get("groupeNumFacture", String.class);
                    String[] groupeNumFactureParts = groupeNumFacture.split("_");
                    if (groupeNumFactureParts.length > 1) {
                        groupeNumFacture = groupeNumFactureParts[1];
                    } else {
                        groupeNumFacture = groupeNumFactureParts[0];
                    }
                    factureDto.setGroupeNumFacture(groupeNumFacture);
                }

                factureDto.setGroupeFactureId(t.get("groupeFactureId", Long.class));
            }
            factureDtos.add(factureDto);
        }
        return factureDtos;
    }

    private List<FactureDto> fetchInvoices(InvoiceSearchParams invoiceSearchParams, Pageable pageable) {
        return buildInvoicesFromTuples(getTuples(buildFinalInvoiceQuery(invoiceSearchParams), pageable), false);
    }

    private List<FactureDto> fetchGroupInvoices(InvoiceSearchParams invoiceSearchParams, Pageable pageable) {
        return buildInvoicesFromTuples(getTuples(buildFinalGroupInvoiceQuery(invoiceSearchParams), pageable), true);
    }

    private GroupeFactureDto buildGroupeFactureDtoFromEntity(FactureTiersPayant factureTiersPayant) {
        GroupeTiersPayant groupeTiersPayant = factureTiersPayant.getGroupeTiersPayant();
        GroupeFactureDto groupeFactureDto = new GroupeFactureDto();
        groupeFactureDto.setName(groupeTiersPayant.getName());
        groupeFactureDto.setTelephone(groupeTiersPayant.getTelephone());
        groupeFactureDto.setAdresse(groupeTiersPayant.getAdresse());
        groupeFactureDto.setNumFacture(factureTiersPayant.getDisplayNumFacture());
        groupeFactureDto.setCreated(factureTiersPayant.getCreated());
        List<FactureTiersPayant> factureTiersPayants = factureTiersPayant.getFactureTiersPayants();
        groupeFactureDto.setFacturesTiersPayants(factureTiersPayants);
        List<FactureDto> factures = factureTiersPayants
            .stream()
            .sorted(Comparator.comparing(FactureTiersPayant::getId))
            .map(fact -> {
                FactureDto factureDto = new FactureDto();
                TiersPayant tiersPayant = fact.getTiersPayant();
                factureDto.setFactureId(fact.getId());
                factureDto.setTiersPayantName(tiersPayant.getFullName());
                List<ThirdPartySaleLine> thirdPartySaleLines = fact.getFacturesDetails();
                long montant = thirdPartySaleLines.stream().mapToLong(ThirdPartySaleLine::getMontant).sum();
                factureDto.setMontant(montant);
                int count = thirdPartySaleLines.size();
                factureDto.setItemsCount(count);
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
        TiersPayant tiersPayant = factureTiersPayant.getTiersPayant();
        factureDto.setFactureId(factureTiersPayant.getId());
        factureDto.setTiersPayantName(tiersPayant.getFullName());
        factureDto.setNumFacture(factureTiersPayant.getDisplayNumFacture());
        factureDto.setDebutPeriode(factureTiersPayant.getDebutPeriode());
        factureDto.setFinPeriode(factureTiersPayant.getFinPeriode());
        factureDto.setCreated(factureTiersPayant.getCreated());
        factureDto.setFactureProvisoire(factureTiersPayant.isFactureProvisoire());
        long remiseVente = 0;
        long montantPaye = 0;
        long montantVente = 0;
        //   boolean isCarnet = false;
        List<ThirdPartySaleLine> thirdPartySaleLines = factureTiersPayant.getFacturesDetails();
        for (ThirdPartySaleLine thirdPartySaleLine : thirdPartySaleLines) {
            ThirdPartySales sales = thirdPartySaleLine.getSale();
            //            if (sales.getNatureVente() == NatureVente.CARNET) {
            //                isCarnet = true;
            //            }
            montantVente += sales.getSalesAmount();
            factureDto.setMontant(factureDto.getMontant() + thirdPartySaleLine.getMontant());
            remiseVente += Objects.requireNonNullElse(sales.getDiscountAmount(), 0);
            montantPaye += Objects.requireNonNullElse(thirdPartySaleLine.getMontantRegle(), 0);
            factureDto.getItems().add(buildFromThirdPartySaleLine(thirdPartySaleLine, sales));
        }
        factureDto.setMontantRemiseVente(remiseVente);
        factureDto.setMontantRegle(factureTiersPayant.getMontantRegle());
        factureDto.setRemiseForfetaire(factureTiersPayant.getRemiseForfetaire());
        int count = thirdPartySaleLines.size();
        factureDto.setItemsCount(count);
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
        factureItemDto.setSaleId(sales.getId());
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
