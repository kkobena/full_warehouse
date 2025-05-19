package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.stock.CommandService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tech.jhipster.web.util.HeaderUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Commande}.
 */
@RestController
@RequestMapping("/api")
public class CommandeResource {

    private static final String ENTITY_NAME = "commande";
    private final Logger log = LoggerFactory.getLogger(CommandeResource.class);
    private final CommandService commandService;

    @Value("${jhipster.clientApp.name}")
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

    @PutMapping("/commandes/delete/order-lines/{id}")
    public ResponseEntity<Void> deleteOrderLinesByIds(@PathVariable Long id, @RequestBody List<Long> ids) {
        commandService.deleteOrderLinesByIds(id, ids);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PutMapping("/commandes/fusionner")
    public ResponseEntity<Void> fusionner(@RequestBody List<Long> ids) {
        commandService.fusionner(ids);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, "")).build();
    }

    @PutMapping("/commandes/delete-commandes")
    public ResponseEntity<Void> deleteSelectedCommandes(@RequestBody List<Long> ids) {
        commandService.deleteAll(ids);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, "")).build();
    }

    @DeleteMapping("/commandes/order-line/{id}")
    public ResponseEntity<Void> deleteOrderLineById(@PathVariable Long id) {
        commandService.deleteOrderLineById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/commandes/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        commandService.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/commandes/verification-commande-en-cours/{id}")
    public ResponseEntity<VerificationResponseCommandeDTO> importerReponseCommande(
        @PathVariable Long id,
        @RequestPart("commande") MultipartFile file
    ) {
        VerificationResponseCommandeDTO verificationResponseCommandeDTO = commandService.importerReponseCommande(id, file);
        return ResponseEntity.ok(verificationResponseCommandeDTO);
    }

    @PostMapping("/commandes/upload-new-commande/{fournisseurId}/{model}")
    public ResponseEntity<CommandeResponseDTO> uploadNewCommande(
        @PathVariable("fournisseurId") Long fournisseurId,
        @PathVariable("model") CommandeModel commandeModel,
        @RequestPart("commande") MultipartFile file
    ) {
        CommandeResponseDTO commandeResponseDTO = commandService.uploadNewCommande(fournisseurId, commandeModel, file);
        return ResponseEntity.ok(commandeResponseDTO);
    }

    @DeleteMapping("/commandes/rollback/{id}")
    public ResponseEntity<Void> rollback(@PathVariable Long id) {
        commandService.rollback(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
