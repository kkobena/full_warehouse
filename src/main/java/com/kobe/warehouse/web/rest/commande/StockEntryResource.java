package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.StockEntryService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
@Transactional //TO DO : remove this annotation
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

    @PutMapping("/commandes/entree-stock/finalize")
    public ResponseEntity<Void> finalizeSaisieEntreeStock(@Valid @RequestBody DeliveryReceiptLiteDTO deliveryReceiptLite) {
        stockEntryService.finalizeSaisieEntreeStock(deliveryReceiptLite);
        return ResponseEntity.accepted()
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, deliveryReceiptLite.getId().toString()))
            .build();
    }

    @PutMapping("/commandes/entree-stock/update-order-line-cost-amount")
    public ResponseEntity<CommandeEntryDTO> updateOrderCostAmount(@Valid @RequestBody OrderLineDTO orderLineDTO) throws URISyntaxException {
        CommandeEntryDTO result = new CommandeEntryDTO(commandService.updateOrderCostAmount(orderLineDTO));
        return ResponseEntity.created(new URI("/api/commandes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/commandes/entree-stock/update-order-line-unit-price")
    public ResponseEntity<Void> updateOrderUnitPrice(@Valid @RequestBody DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        stockEntryService.updateOrderUnitPrice(deliveryReceiptItem);
        return ResponseEntity.accepted()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, deliveryReceiptItem.getId().toString()))
            .build();
    }

    @PutMapping("/commandes/entree-stock/update-order-line-quantity-received")
    public ResponseEntity<Void> updateQuantityReceived(@Valid @RequestBody DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        stockEntryService.updateQuantityReceived(deliveryReceiptItem);
        return ResponseEntity.accepted()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, deliveryReceiptItem.getId().toString()))
            .build();
    }

    @PutMapping("/commandes/entree-stock/update-provisional-cip")
    public ResponseEntity<Void> updateCodeCip(@Valid @RequestBody OrderLineDTO orderLineDTO) {
        commandService.updateCodeCip(orderLineDTO);
        return ResponseEntity.accepted()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, orderLineDTO.getId().toString()))
            .build();
    }

    @PutMapping("/commandes/entree-stock/update-order-line-quantity-ug")
    public ResponseEntity<Void> updateQuantityUG(@Valid @RequestBody DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        stockEntryService.updateQuantityUG(deliveryReceiptItem);
        return ResponseEntity.accepted()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, deliveryReceiptItem.getId().toString()))
            .build();
    }

    @PutMapping("/commandes/entree-stock/update-order-line-tva")
    public ResponseEntity<Void> updateTva(@Valid @RequestBody DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        stockEntryService.updateTva(deliveryReceiptItem);
        return ResponseEntity.accepted()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, deliveryReceiptItem.getId().toString()))
            .build();
    }

    @PutMapping("/commandes/entree-stock/update-order-line-date-peremption")
    public ResponseEntity<Void> updateDatePeremption(@Valid @RequestBody DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        stockEntryService.updateDatePeremption(deliveryReceiptItem);
        return ResponseEntity.accepted()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, deliveryReceiptItem.getId().toString()))
            .build();
    }

    @PostMapping("/commandes/entree-stock/create")
    public ResponseEntity<DeliveryReceiptLiteDTO> createBon(@Valid @RequestBody DeliveryReceiptLiteDTO deliveryReceiptLiteDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockEntryService.createBon(deliveryReceiptLiteDTO));
    }

    @PutMapping("/commandes/entree-stock/create")
    public ResponseEntity<DeliveryReceiptLiteDTO> updateBon(@Valid @RequestBody DeliveryReceiptLiteDTO deliveryReceiptLiteDTO) {
        return ResponseEntity.accepted().body(stockEntryService.updateBon(deliveryReceiptLiteDTO));
    }

    @PostMapping(path = "/commandes/entree-stock/upload-new", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<CommandeResponseDTO> importerReponseCommande(
        @RequestPart("deliveryReceipt") UploadDeleiveryReceiptDTO deliveryReceipt,
        @RequestPart("fichier") MultipartFile file
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockEntryService.importNewBon(deliveryReceipt, file));
    }
}
