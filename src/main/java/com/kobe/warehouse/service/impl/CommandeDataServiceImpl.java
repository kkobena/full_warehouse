package com.kobe.warehouse.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.PharmaMlEnvoi;
import com.kobe.warehouse.domain.enumeration.PharmaMlStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.CustomizedCommandeService;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.PharmaMlEnvoiRepository;
import com.kobe.warehouse.service.csv.ExportationCsvService;
import com.kobe.warehouse.service.dto.CommandeDashboardDTO;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.CommandeResumeeDTO;
import com.kobe.warehouse.service.dto.FilterCommaneEnCours;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.PharmaMlEnvoiResumeeDTO;
import com.kobe.warehouse.service.dto.Sort;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.report.CommandeReportReportService;
import com.kobe.warehouse.service.stock.CommandeDataService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommandeDataServiceImpl implements CommandeDataService {

    private static final Logger LOG = LoggerFactory.getLogger(CommandeDataServiceImpl.class);
    private final CommandeRepository commandeRepository;
    private final ExportationCsvService exportationCsvService;
    private final CommandeReportReportService commandeReportService;
    private final CustomizedCommandeService customizedCommandeService;
    private final OrderLineRepository orderLineRepository;
    private final ObjectMapper objectMapper;
    private final PharmaMlEnvoiRepository pharmaMlEnvoiRepository;

    private final BiPredicate<OrderLine, String> searchPredicate = (orderLine, s) ->
        StringUtils.isEmpty(s) ||
        (orderLine.getFournisseurProduit().getProduit().getLibelle().contains(s.toUpperCase()) ||
            orderLine.getFournisseurProduit().getCodeCip().contains(s));
    private final BiPredicate<OrderLine, FilterCommaneEnCours> searchFilterCip = (orderLine, _) -> orderLine.getProvisionalCode();
    private final BiPredicate<OrderLine, FilterCommaneEnCours> searchFilterPrix = (orderLine, _) ->
        orderLine.getOrderCostAmount().compareTo(orderLine.getFournisseurProduit().getPrixAchat()) != 0;
    private final Comparator<OrderLineDTO> comparingByProduitLibelle = Comparator.comparing(OrderLineDTO::getProduitLibelle);
    private final Comparator<OrderLineDTO> comparingByProduitCip = Comparator.comparing(OrderLineDTO::getProduitCip);
    private final Comparator<OrderLineDTO> comparingByDateUpdated = Comparator.comparing(
        OrderLineDTO::getUpdatedAt,
        Comparator.reverseOrder()
    );

    public CommandeDataServiceImpl(
        CommandeRepository commandeRepository,
        ExportationCsvService exportationCsvService,
        CommandeReportReportService commandeReportService,
        CustomizedCommandeService customizedCommandeService,
        OrderLineRepository orderLineRepository,
        ObjectMapper objectMapper,
        PharmaMlEnvoiRepository pharmaMlEnvoiRepository
    ) {
        this.commandeRepository = commandeRepository;
        this.exportationCsvService = exportationCsvService;
        this.commandeReportService = commandeReportService;
        this.customizedCommandeService = customizedCommandeService;
        this.orderLineRepository = orderLineRepository;
        this.objectMapper = objectMapper;
        this.pharmaMlEnvoiRepository = pharmaMlEnvoiRepository;
    }

    private Commande findId(CommandeId id) {
        return commandeRepository.getReferenceById(id);
    }

    @Override
    public CommandeDTO findOneById(CommandeId id) {
        return new CommandeDTO(findId(id));
    }

    @Override
    public Optional<CommandeEntryDTO> getCommandeById(CommandeId id) {
        return Optional.ofNullable(findId(id)).map(CommandeEntryDTO::new);
    }

    @Override
    public Resource exportCommandeToCsv(CommandeId id) throws IOException {
        return getResource(exportationCsvService.exportCommandeToCsv(findId(id)));
    }

    @Override
    public byte[] exportCommandeToPdf(CommandeId id) {
        return commandeReportService.export(findOneById(id));
    }

    @Override
    public List<OrderLineDTO> filterCommandeLines(CommandeFilterDTO commandeFilter) {
        List<OrderLine> orderLines = findId(new CommandeId(commandeFilter.getCommandeId(), commandeFilter.getOrderDate())).getOrderLines();

        if (StringUtils.isNotEmpty(commandeFilter.getSearch())) {
            if (commandeFilter.getFilterCommaneEnCours() != null && commandeFilter.getFilterCommaneEnCours() != FilterCommaneEnCours.ALL) {
                switch (commandeFilter.getFilterCommaneEnCours()) {
                    case NOT_EQUAL:
                        return orderLines
                            .stream()
                            .filter(orderLine -> searchFilterPrix.test(orderLine, commandeFilter.getFilterCommaneEnCours()))
                            .filter(orderLine -> searchPredicate.test(orderLine, commandeFilter.getSearch()))
                            .map(OrderLineDTO::new)
                            .sorted(getSort(commandeFilter.getOrderBy()))
                            .toList();
                    case PROVISOL_CIP:
                        return orderLines
                            .stream()
                            .filter(orderLine -> searchFilterCip.test(orderLine, commandeFilter.getFilterCommaneEnCours()))
                            .filter(orderLine -> searchPredicate.test(orderLine, commandeFilter.getSearch()))
                            .map(OrderLineDTO::new)
                            .sorted(getSort(commandeFilter.getOrderBy()))
                            .toList();
                }
            }
            return orderLines
                .stream()
                .filter(orderLine -> searchPredicate.test(orderLine, commandeFilter.getSearch()))
                .map(OrderLineDTO::new)
                .sorted(getSort(commandeFilter.getOrderBy()))
                .toList();
        }
        if (commandeFilter.getFilterCommaneEnCours() != null && commandeFilter.getFilterCommaneEnCours() != FilterCommaneEnCours.ALL) {
            switch (commandeFilter.getFilterCommaneEnCours()) {
                case NOT_EQUAL:
                    return orderLines
                        .stream()
                        .filter(orderLine -> searchFilterPrix.test(orderLine, commandeFilter.getFilterCommaneEnCours()))
                        .map(OrderLineDTO::new)
                        .sorted(getSort(commandeFilter.getOrderBy()))
                        .toList();
                case PROVISOL_CIP:
                    return orderLines
                        .stream()
                        .filter(orderLine -> searchFilterCip.test(orderLine, commandeFilter.getFilterCommaneEnCours()))
                        .map(OrderLineDTO::new)
                        .sorted(getSort(commandeFilter.getOrderBy()))
                        .toList();
            }
        }
        return orderLines.stream().map(OrderLineDTO::new).sorted(getSort(commandeFilter.getOrderBy())).toList();
    }

    @Override
    public Page<CommandeLiteDTO> fetchCommandes(CommandeFilterDTO commandeFilterDTO, Pageable pageable) {
        long count = customizedCommandeService.countfetchCommandes(commandeFilterDTO);
        if (count == 0) {
            new PageImpl<>(Collections.emptyList(), pageable, count);
        }
        return new PageImpl<>(
            customizedCommandeService
                .fetchCommandes(commandeFilterDTO, pageable)
                .stream()
                .map(commande -> new CommandeLiteDTO(commande, orderLineRepository.countByCommande((commande))))
                .toList(),
            pageable,
            count
        );
    }

    @Override
    public Page<OrderLineDTO> filterCommandeLines(CommandeId commandeId, Pageable pageable) {
        return orderLineRepository
            .findByCommandeIdAndCommandeOrderDate(commandeId.getId(), commandeId.getOrderDate(), pageable)
            .map(OrderLineDTO::new);
    }

    @Override
    public Resource getRuptureCsv(String reference) {
        return exportationCsvService.getRutureFileByOrderReference(reference);
    }

    @Override
    public List<AchatDTO> fetchReportTableauPharmacienData(MvtParam mvtParam) {
        try {
            String jsonResult;
            if ("month".equals(mvtParam.getGroupeBy())) {
                jsonResult = commandeRepository.fetchTableauPharmacienReportMensuel(
                    mvtParam.getFromDate(),
                    mvtParam.getToDate(),
                    OrderStatut.CLOSED.name()
                );
            } else {
                jsonResult = commandeRepository.fetchTableauPharmacienReport(
                    mvtParam.getFromDate(),
                    mvtParam.getToDate(),
                    OrderStatut.CLOSED.name()
                );
            }
            if (StringUtils.isEmpty(jsonResult)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(jsonResult, new TypeReference<>() {});
        } catch (Exception e) {
            LOG.error(null, e);
            return new ArrayList<>();
        }
    }

    @Override
    public CommandeDashboardDTO getDashboard() {
        List<Commande> allRequested = commandeRepository.findAll(
            (root, query, cb) -> cb.equal(root.get(Commande_.orderStatus), OrderStatut.REQUESTED)
        );
        List<Commande> allReceived = commandeRepository.findAll(
            (root, query, cb) -> cb.equal(root.get(Commande_.orderStatus), OrderStatut.RECEIVED)
        );
        List<PharmaMlEnvoi> envoisPending = pharmaMlEnvoiRepository.findByStatutOrderByCreatedAtDesc(PharmaMlStatut.PENDING);

        List<CommandeResumeeDTO> requestedDTOs = allRequested.stream()
            .sorted(Comparator.comparing(Commande::getOrderDate).reversed())
            .limit(20)
            .map(this::toResumee)
            .toList();

        List<CommandeResumeeDTO> receivedDTOs = allReceived.stream()
            .sorted(Comparator.comparing(Commande::getOrderDate).reversed())
            .limit(20)
            .map(this::toResumee)
            .toList();

        List<PharmaMlEnvoiResumeeDTO> envoisDTOs = envoisPending.stream()
            .limit(50)
            .map(this::toEnvoiResumee)
            .toList();

        return new CommandeDashboardDTO(
            allRequested.size(),
            allReceived.size(),
            envoisPending.size(),
            requestedDTOs,
            receivedDTOs,
            envoisDTOs
        );
    }

    private CommandeResumeeDTO toResumee(Commande c) {
        return new CommandeResumeeDTO(
            c.getId().getId(),
            c.getId().getOrderDate().toString(),
            c.getOrderReference(),
            c.getFournisseur().getLibelle(),
            c.getGrossAmount() != null ? c.getGrossAmount() : 0,
            c.getOrderStatus().name(),
            c.getReliquatDeCommandeId()
        );
    }

    private PharmaMlEnvoiResumeeDTO toEnvoiResumee(PharmaMlEnvoi e) {
        Commande c = e.getCommande();
        CommandeId commandeId = c.getId();
        return new PharmaMlEnvoiResumeeDTO(
            e.getId(),
            commandeId.getId(),
            commandeId.getOrderDate().toString(),
            c.getOrderReference(),
            e.getFournisseur().getLibelle(),
            e.getStatut().name(),
            e.getDerniereTentative() != null ? e.getDerniereTentative() : e.getCreatedAt()
        );
    }

    private Resource getResource(String path) throws MalformedURLException {
        return new UrlResource(Paths.get(path).toUri());
    }

    private Comparator<OrderLineDTO> getSort(Sort sort) {
        return switch (sort) {
            case PRODUIT_LIBELLE -> comparingByProduitLibelle;
            case PRODUIT_CIP -> comparingByProduitCip;
            case UPDATE -> comparingByDateUpdated;
        };
    }
}
