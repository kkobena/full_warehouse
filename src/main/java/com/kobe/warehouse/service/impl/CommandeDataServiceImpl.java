package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.CustomizedCommandeService;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.csv.ExportationCsvService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.FilterCommaneEnCours;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.Sort;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import com.kobe.warehouse.service.report.CommandeReportService;
import com.kobe.warehouse.service.stock.CommandeDataService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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

    private final CommandeRepository commandeRepository;
    private final ExportationCsvService exportationCsvService;
    private final CommandeReportService commandeReportService;
    private final CustomizedCommandeService customizedCommandeService;
    private final OrderLineRepository orderLineRepository;

    private final BiPredicate<OrderLine, String> searchPredicate =
        (orderLine, s) ->
            StringUtils.isEmpty(s)
                || (orderLine
                .getFournisseurProduit()
                .getProduit()
                .getLibelle()
                .contains(s.toUpperCase())
                || orderLine.getFournisseurProduit().getCodeCip().contains(s));
    private final BiPredicate<OrderLine, FilterCommaneEnCours> searchFilterCip =
        (orderLine, _) -> orderLine.getProvisionalCode();
    private final BiPredicate<OrderLine, FilterCommaneEnCours> searchFilterPrix =
        (orderLine, _) ->
            orderLine.getOrderCostAmount().compareTo(orderLine.getCostAmount()) != 0;
    private final Comparator<OrderLineDTO> comparingByProduitLibelle =
        Comparator.comparing(OrderLineDTO::getProduitLibelle);
    private final Comparator<OrderLineDTO> comparingByProduitCip =
        Comparator.comparing(OrderLineDTO::getProduitCip);
    private final Comparator<OrderLineDTO> comparingByDateUpdated =
        Comparator.comparing(OrderLineDTO::getUpdatedAt, Comparator.reverseOrder());

    public CommandeDataServiceImpl(
        CommandeRepository commandeRepository,
        ExportationCsvService exportationCsvService,
        CommandeReportService commandeReportService,
        CustomizedCommandeService customizedCommandeService,
        OrderLineRepository orderLineRepository) {
        this.commandeRepository = commandeRepository;
        this.exportationCsvService = exportationCsvService;
        this.commandeReportService = commandeReportService;
        this.customizedCommandeService = customizedCommandeService;
        this.orderLineRepository = orderLineRepository;
    }

    @Override
    public CommandeDTO findOneById(Long id) {
        return new CommandeDTO(commandeRepository.getReferenceById(id));
    }

    @Override
    public Optional<Commande> getOneById(Long id) {
        return commandeRepository.findById(id);
    }

    @Override
    public Optional<CommandeEntryDTO> getCommandeById(Long id) {
        return commandeRepository.findById(id).map(CommandeEntryDTO::new);
    }

    @Override
    public Resource exportCommandeToCsv(Long id) throws IOException {

        return getResource(
            exportationCsvService.exportCommandeToCsv(commandeRepository.getReferenceById(id)));
    }

    @Override
    public Resource exportCommandeToPdf(Long id) throws IOException {
        return getResource(commandeReportService.printCommandeEnCours(findOneById(id)));
    }

    @Override
    public List<OrderLineDTO> filterCommandeLines(CommandeFilterDTO commandeFilter) {
        Set<OrderLine> orderLines =
            commandeRepository.getReferenceById(commandeFilter.getCommandeId()).getOrderLines();

        if (StringUtils.isNotEmpty(commandeFilter.getSearch())) {
            if (commandeFilter.getFilterCommaneEnCours() != null
                && commandeFilter.getFilterCommaneEnCours() != FilterCommaneEnCours.ALL) {
                switch (commandeFilter.getFilterCommaneEnCours()) {
                    case NOT_EQUAL:
                        return orderLines.stream()
                            .filter(
                                orderLine ->
                                    searchFilterPrix.test(orderLine,
                                        commandeFilter.getFilterCommaneEnCours()))
                            .filter(orderLine -> searchPredicate.test(orderLine,
                                commandeFilter.getSearch()))
                            .map(OrderLineDTO::new)
                            .sorted(getSort(commandeFilter.getOrderBy()))
                            .collect(Collectors.toList());

                    case PROVISOL_CIP:
                        return orderLines.stream()
                            .filter(
                                orderLine ->
                                    searchFilterCip.test(orderLine,
                                        commandeFilter.getFilterCommaneEnCours()))
                            .filter(orderLine -> searchPredicate.test(orderLine,
                                commandeFilter.getSearch()))
                            .map(OrderLineDTO::new)
                            .sorted(getSort(commandeFilter.getOrderBy()))
                            .collect(Collectors.toList());
                }
            }
            return orderLines.stream()
                .filter(orderLine -> searchPredicate.test(orderLine, commandeFilter.getSearch()))
                .map(OrderLineDTO::new)
                .sorted(getSort(commandeFilter.getOrderBy()))
                .collect(Collectors.toList());
        }
        if (commandeFilter.getFilterCommaneEnCours() != null
            && commandeFilter.getFilterCommaneEnCours() != FilterCommaneEnCours.ALL) {

            switch (commandeFilter.getFilterCommaneEnCours()) {
                case NOT_EQUAL:
                    return orderLines.stream()
                        .filter(
                            orderLine ->
                                searchFilterPrix.test(orderLine,
                                    commandeFilter.getFilterCommaneEnCours()))
                        .map(OrderLineDTO::new)
                        .sorted(getSort(commandeFilter.getOrderBy()))
                        .collect(Collectors.toList());

                case PROVISOL_CIP:
                    return orderLines.stream()
                        .filter(
                            orderLine ->
                                searchFilterCip.test(orderLine,
                                    commandeFilter.getFilterCommaneEnCours()))
                        .map(OrderLineDTO::new)
                        .sorted(getSort(commandeFilter.getOrderBy()))
                        .collect(Collectors.toList());
            }
        }
        return orderLines.stream()
            .map(OrderLineDTO::new)
            .sorted(getSort(commandeFilter.getOrderBy()))
            .collect(Collectors.toList());
    }

    @Override
    public Page<CommandeLiteDTO> fetchCommandes(
        CommandeFilterDTO commandeFilterDTO, Pageable pageable) {
        long count = customizedCommandeService.countfetchCommandes(commandeFilterDTO);
        if (count == 0) {
            new PageImpl<>(Collections.emptyList(), pageable, count);
        }
        return new PageImpl<>(
            customizedCommandeService.fetchCommandes(commandeFilterDTO, pageable).stream()
                .map(
                    commande ->
                        new CommandeLiteDTO(
                            commande, orderLineRepository.countByCommandeId(commande.getId())))
                .collect(Collectors.toList()),
            pageable,
            count);
    }

    @Override
    public Page<OrderLineDTO> filterCommandeLines(Long commandeId, Pageable pageable) {
        return orderLineRepository.findByCommandeId(commandeId, pageable).map(OrderLineDTO::new);
    }

    @Override
    public Resource getRuptureCsv(String reference) {
        return exportationCsvService.getRutureFileByOrderReference(reference);
    }

    @Override
    public Optional<CommandeDTO> findOneByOrderReference(String orderReference) {
        return this.commandeRepository.getFirstByOrderRefernce(orderReference)
            .map(CommandeDTO::new);
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
