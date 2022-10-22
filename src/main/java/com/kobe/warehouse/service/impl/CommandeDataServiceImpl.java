package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.CustomizedCommandeService;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.CommandeDataService;
import com.kobe.warehouse.service.csv.ExportationCsvService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeFilterDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.report.CommandeReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class CommandeDataServiceImpl implements CommandeDataService {
  private final CommandeRepository commandeRepository;
  private final ExportationCsvService exportationCsvService;
  private final CommandeReportService commandeReportService;
  private final CustomizedCommandeService customizedCommandeService;
  private final OrderLineRepository orderLineRepository;

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
    return new CommandeDTO(this.commandeRepository.getOne(id));
  }

  @Override
  public Resource exportCommandeToCsv(Long id) throws IOException {

    return getResource(
        exportationCsvService.exportCommandeToCsv(this.commandeRepository.getOne(id)));
  }

  @Override
  public Resource exportCommandeToPdf(Long id) throws IOException {
    return getResource(this.commandeReportService.printCommandeEnCours(this.findOneById(id)));
  }

  @Override
  public List<OrderLineDTO> filterCommandeLines(CommandeFilterDTO commandeFilter) {
    Set<OrderLine> orderLines =
        this.commandeRepository.getOne(commandeFilter.getCommandeId()).getOrderLines();

    if (StringUtils.isNotEmpty(commandeFilter.getSearch())) {
      if (commandeFilter.getFilterCommaneEnCours() != null
          && commandeFilter.getFilterCommaneEnCours()
              != CommandeFilterDTO.FilterCommaneEnCours.ALL) {
        switch (commandeFilter.getFilterCommaneEnCours()) {
          case NOT_EQUAL:
            return orderLines.stream()
                .filter(
                    orderLine ->
                        this.searchFilterPrix.test(
                            orderLine, commandeFilter.getFilterCommaneEnCours()))
                .filter(
                    orderLine -> this.searchPredicate.test(orderLine, commandeFilter.getSearch()))
                .map(OrderLineDTO::new)
                .collect(Collectors.toList());

          case PROVISOL_CIP:
            return orderLines.stream()
                .filter(
                    orderLine ->
                        this.searchFilterCip.test(
                            orderLine, commandeFilter.getFilterCommaneEnCours()))
                .filter(
                    orderLine -> this.searchPredicate.test(orderLine, commandeFilter.getSearch()))
                .map(OrderLineDTO::new)
                .collect(Collectors.toList());
        }
      }
      return orderLines.stream()
          .filter(orderLine -> this.searchPredicate.test(orderLine, commandeFilter.getSearch()))
          .map(OrderLineDTO::new)
          .collect(Collectors.toList());
    }
    if (commandeFilter.getFilterCommaneEnCours() != null
        && commandeFilter.getFilterCommaneEnCours() != CommandeFilterDTO.FilterCommaneEnCours.ALL) {

      switch (commandeFilter.getFilterCommaneEnCours()) {
        case NOT_EQUAL:
          return orderLines.stream()
              .filter(
                  orderLine ->
                      this.searchFilterPrix.test(
                          orderLine, commandeFilter.getFilterCommaneEnCours()))
              .map(OrderLineDTO::new)
              .collect(Collectors.toList());

        case PROVISOL_CIP:
          return orderLines.stream()
              .filter(
                  orderLine ->
                      this.searchFilterCip.test(
                          orderLine, commandeFilter.getFilterCommaneEnCours()))
              .map(OrderLineDTO::new)
              .collect(Collectors.toList());
      }
    }
    return orderLines.stream().map(OrderLineDTO::new).collect(Collectors.toList());
  }

  @Override
  public Page<CommandeLiteDTO> fetchCommandes(
      CommandeFilterDTO commandeFilterDTO, Pageable pageable) {
    long count = this.customizedCommandeService.countfetchCommandes(commandeFilterDTO);
    if (count == 0) {
      new PageImpl<>(Collections.emptyList(), pageable, count);
    }
    return new PageImpl<>(
        this.customizedCommandeService.fetchCommandes(commandeFilterDTO, pageable).stream()
            .map(
                commande ->
                    new CommandeLiteDTO(
                        commande, this.orderLineRepository.countByCommandeId(commande.getId())))
            .collect(Collectors.toList()),
        pageable,
        count);
  }

  @Override
  public Page<OrderLineDTO> filterCommandeLines(Long commandeId, Pageable pageable) {
    return this.orderLineRepository.findByCommandeId(commandeId, pageable).map(OrderLineDTO::new);
  }

  @Override
  public Resource getRuptureCsv(String reference) throws IOException {
    return this.exportationCsvService.getRutureFileByOrderReference(reference);
  }

  private Resource getResource(String path) throws MalformedURLException {
    return new UrlResource(Paths.get(path).toUri());
  }

  private final BiPredicate<OrderLine, String> searchPredicate =
      (orderLine, s) ->
          StringUtils.isEmpty(s)
              || (orderLine
                      .getFournisseurProduit()
                      .getProduit()
                      .getLibelle()
                      .contains(s.toUpperCase())
                  || orderLine.getFournisseurProduit().getCodeCip().contains(s));

  private final BiPredicate<OrderLine, CommandeFilterDTO.FilterCommaneEnCours> searchFilterCip =
      (orderLine, filterCommaneEnCours) -> orderLine.getProvisionalCode();
  private final BiPredicate<OrderLine, CommandeFilterDTO.FilterCommaneEnCours> searchFilterPrix =
      (orderLine, filterCommaneEnCours) ->
          orderLine.getOrderCostAmount().compareTo(orderLine.getCostAmount()) != 0;
}
