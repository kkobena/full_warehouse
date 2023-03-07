package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.StockEntryService;
import java.net.URI;
import java.net.URISyntaxException;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
@Transactional
public class StockEntryResource {

  private static final String ENTITY_NAME = "deliveryReceipt";
  private final StockEntryService stockEntryService;
  private final CommandService commandService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public StockEntryResource(StockEntryService stockEntryService, CommandService commandService) {
    this.stockEntryService = stockEntryService;
    this.commandService = commandService;
  }

  @PutMapping("/commandes/entree-stock/save")
  public ResponseEntity<CommandeEntryDTO> saveSaisieEntreeStock(
      @Valid @RequestBody CommandeDTO commandeDTO) throws URISyntaxException {
    CommandeEntryDTO result =
        new CommandeEntryDTO(stockEntryService.saveSaisieEntreeStock(commandeDTO));
    return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  @PutMapping("/commandes/entree-stock/finalize")
  public ResponseEntity<Void> finalizeSaisieEntreeStock(
      @Valid @RequestBody CommandeDTO commandeDTO) {
    stockEntryService.finalizeSaisieEntreeStock(commandeDTO);
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, commandeDTO.getId().toString()))
        .build();
  }

  @PutMapping("/commandes/entree-stock/update-order-line-cost-amount")
  public ResponseEntity<CommandeEntryDTO> updateOrderCostAmount(
      @Valid @RequestBody OrderLineDTO orderLineDTO) throws URISyntaxException {
    CommandeEntryDTO result =
        new CommandeEntryDTO(commandService.updateOrderCostAmount(orderLineDTO));
    return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  @PutMapping("/commandes/entree-stock/update-order-line-unit-price")
  public ResponseEntity<CommandeEntryDTO> updateOrderUnitPrice(
      @Valid @RequestBody OrderLineDTO orderLineDTO) throws URISyntaxException {
    CommandeEntryDTO result =
        new CommandeEntryDTO(commandService.updateOrderUnitPrice(orderLineDTO));
    return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  @PutMapping("/commandes/entree-stock/update-order-line-quantity-received")
  public ResponseEntity<Void> updateQuantityReceived(
      @Valid @RequestBody OrderLineDTO orderLineDTO) {
    commandService.updateOrderLineQuantityReceived(orderLineDTO);
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityDeletionAlert(
                applicationName, true, ENTITY_NAME, orderLineDTO.getId().toString()))
        .build();
  }

  @PutMapping("/commandes/entree-stock/update-provisional-cip")
  public ResponseEntity<Void> updateCodeCip(@Valid @RequestBody OrderLineDTO orderLineDTO) {
    commandService.updateCodeCip(orderLineDTO);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(
                applicationName, true, ENTITY_NAME, orderLineDTO.getId().toString()))
        .build();
  }

  @PutMapping("/commandes/entree-stock/update-order-line-quantity-ug")
  public ResponseEntity<Void> updateQuantityUG(@Valid @RequestBody OrderLineDTO orderLineDTO) {
    commandService.updateOrderLineQuantityUg(orderLineDTO);
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityDeletionAlert(
                applicationName, true, ENTITY_NAME, orderLineDTO.getId().toString()))
        .build();
  }
}
