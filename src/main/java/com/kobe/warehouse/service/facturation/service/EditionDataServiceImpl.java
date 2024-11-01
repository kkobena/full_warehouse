package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.facturation.dto.DossierFactureDto;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
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
    public void deleteFacture(Set<Long> ids) {
        List<FactureTiersPayant> factureTiersPayants =
            this.facturationRepository.findAll(Specification.where(this.facturationRepository.fetchByIs(ids)));
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

    private Specification<ThirdPartySaleLine> buildFetchSpecification(EditionSearchParams editionSearchParams) {
        Specification<ThirdPartySaleLine> thirdPartySaleLineSpecification = Specification.where(
            this.thirdPartySaleLineRepository.canceledCriteria()
        );
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
        getEditionDatas(editionSearchParams, pageable).forEach(t -> {
            factureDtos.add(
                new TiersPayantDossierFactureDto(
                    t.get("tiersPayantId", Long.class),
                    t.get("tiersPayantName", String.class),
                    t.get("totalAmount", BigDecimal.class),
                    t.get("factureItemCount", Long.class).intValue()
                )
            );
        });
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
            Integer montantRegle = t.get("montantRegle", Integer.class);
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
                Long remiseForfetaire = t.get("remiseForfetaire", Long.class);
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
}
