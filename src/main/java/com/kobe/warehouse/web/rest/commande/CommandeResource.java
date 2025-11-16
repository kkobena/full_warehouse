package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.web.util.HeaderUtil;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Commande}.
 */
@RestController
@RequestMapping("/api")
public class CommandeResource {

    private static final String ENTITY_NAME = "commande";

    private final CommandService commandService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public CommandeResource(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/commandes")
    public ResponseEntity<CommandeLiteDTO> createCommande(@Valid @RequestBody CommandeDTO commandeDTO) throws URISyntaxException {
        CommandeLiteDTO commande = commandService.createNewCommandeFromCommandeDTO(commandeDTO);
        return ResponseEntity.created(new URI("/api/commandes/" + commande.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, commande.getId().toString()))
            .body(commande);
    }

    @PostMapping("/commandes/add-order-line")
    public ResponseEntity<CommandeLiteDTO> addOrderLine(@Valid @RequestBody OrderLineDTO orderLineDTO) throws URISyntaxException {
        CommandeLiteDTO result = commandService.createOrUpdateOrderLine(orderLineDTO);
        return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/commandes/update-order-line-quantity-requested")
    public ResponseEntity<CommandeLiteDTO> updateQuantityRequested(@Valid @RequestBody OrderLineDTO orderLineDTO)
        throws URISyntaxException {
        CommandeLiteDTO result = commandService.updateQuantityRequested(orderLineDTO);
        return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/commandes/update-order-line-cost-amount")
    public ResponseEntity<CommandeLiteDTO> updateOrderCostAmount(@Valid @RequestBody OrderLineDTO orderLineDTO) throws URISyntaxException {
        CommandeLiteDTO result = new CommandeLiteDTO(commandService.updateOrderCostAmount(orderLineDTO));
        return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/commandes/update-order-line-unit-price")
    public ResponseEntity<CommandeLiteDTO> updateOrderUnitPrice(@Valid @RequestBody OrderLineDTO orderLineDTO) throws URISyntaxException {
        CommandeLiteDTO result = new CommandeLiteDTO(commandService.updateOrderUnitPrice(orderLineDTO));
        return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/commandes/update-provisional-cip")
    public ResponseEntity<Void> updateCodeCip(@Valid @RequestBody OrderLineDTO orderLineDTO) {
        commandService.updateCodeCip(orderLineDTO);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, orderLineDTO.getId().toString()))
            .build();
    }

    @PutMapping("/commandes/delete/order-lines/{id}/{orderDate}")
    public ResponseEntity<Void> deleteOrderLinesByIds(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate,
        @RequestBody List<OrderLineId> ids
    ) {
        commandService.deleteOrderLinesByIds(new CommandeId(id, orderDate), ids);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PutMapping("/commandes/fusionner")
    public ResponseEntity<Void> fusionner(@RequestBody List<CommandeId> ids) {
        commandService.fusionner(ids);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, "")).build();
    }

    @PutMapping("/commandes/delete-commandes")
    public ResponseEntity<Void> deleteSelectedCommandes(@RequestBody List<CommandeId> ids) {
        commandService.deleteAll(ids);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, "")).build();
    }

    @DeleteMapping("/commandes/order-line/{id}/{orderDate}")
    public ResponseEntity<Void> deleteOrderLineById(@PathVariable Integer id, @PathVariable LocalDate orderDate) {
        commandService.deleteOrderLineById(new OrderLineId(id, orderDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/commandes/{id}/{orderDate}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id, @PathVariable LocalDate orderDate) {
        commandService.deleteById(new CommandeId(id, orderDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/commandes/verification-commande-en-cours/{id}/{orderDate}")
    public ResponseEntity<VerificationResponseCommandeDTO> importerReponseCommande(
        @PathVariable("id") Integer id,
        @PathVariable("orderDate") LocalDate orderDate,
        @RequestPart("commande") MultipartFile file
    ) {
        VerificationResponseCommandeDTO verificationResponseCommandeDTO = commandService.importerReponseCommande(
            new CommandeId(id, orderDate),
            file
        );
        return ResponseEntity.ok(verificationResponseCommandeDTO);
    }

    @PostMapping("/commandes/upload-new-commande/{fournisseurId}/{model}")
    public ResponseEntity<CommandeResponseDTO> uploadNewCommande(
        @PathVariable("fournisseurId") Integer fournisseurId,
        @PathVariable("model") CommandeModel commandeModel,
        @RequestPart("commande") MultipartFile file
    ) {
        CommandeResponseDTO commandeResponseDTO = commandService.uploadNewCommande(fournisseurId, commandeModel, file);
        return ResponseEntity.ok(commandeResponseDTO);
    }

    @DeleteMapping("/commandes/rollback/{id}/{orderDate}")
    public ResponseEntity<Void> rollback(@PathVariable Integer id, @PathVariable LocalDate orderDate) {
        commandService.rollback(new CommandeId(id, orderDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PutMapping("/commandes/change-grossiste")
    public ResponseEntity<Void> changeGrossiste(@RequestBody CommandeDTO commandeDTO) {
        commandService.changeGrossiste(commandeDTO);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/commandes/update-order-line-quantity-ug")
    public ResponseEntity<Void> updateQuantityUG(@Valid @RequestBody OrderLineDTO orderLineDTO) {
        commandService.updateOrderLineQuantityUg(orderLineDTO);
        return ResponseEntity.accepted().build();
    }
}
