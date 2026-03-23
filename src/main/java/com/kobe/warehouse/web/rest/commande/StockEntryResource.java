package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.PriceHistoryDTO;
import com.kobe.warehouse.service.dto.PutawayPreviewItemDTO;
import com.kobe.warehouse.service.dto.StockEntryResultDTO;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.StockEntryService;
import com.kobe.warehouse.web.util.HeaderUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Transactional //TO DO : remove this annotation
public class StockEntryResource {

    private static final String ENTITY_NAME = "deliveryReceipt";
    private final StockEntryService stockEntryService;
    private final CommandService commandService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public StockEntryResource(StockEntryService stockEntryService, CommandService commandService) {
        this.stockEntryService = stockEntryService;
        this.commandService = commandService;
    }

    @PutMapping("/commandes/entree-stock/finalize")
    public ResponseEntity<StockEntryResultDTO> finalizeSaisieEntreeStock(@Valid @RequestBody DeliveryReceiptLiteDTO deliveryReceiptLite) {
        return ResponseEntity.accepted().body(stockEntryService.finalizeSaisieEntreeStock(deliveryReceiptLite));
    }

    /**
     * Prévisualisation de la répartition automatique rayon → réserve avant validation finale.
     * Retourne la liste des produits dont le stock rayon dépasse {@code stockMaxi}.
     * Utilisé par le frontend en mode MANUAL pour alimenter le modal de confirmation.
     */
    @GetMapping("/commandes/entree-stock/putaway-preview/{commandeId}/{orderDate}")
    public ResponseEntity<List<PutawayPreviewItemDTO>> getPutawayPreview(@PathVariable Integer commandeId,@PathVariable LocalDate orderDate) {
        return ResponseEntity.ok(stockEntryService.getPutawayPreview(commandeId,orderDate));
    }

    /**
     * Retourne les lignes dont la variation de prix dépasse le seuil configuré (APP_SEUIL_VARIATION_PRIX).
     * À appeler avant finalisation pour alerter l'utilisateur côté frontend.
     * Opère sur TOUTES les lignes (sans pagination).
     */
    @GetMapping("/commandes/entree-stock/prix-historique/{fournisseurProduitId}")
    public ResponseEntity<List<PriceHistoryDTO>> getPriceHistory(@PathVariable Integer fournisseurProduitId) {
        return ResponseEntity.ok(stockEntryService.getPriceHistory(fournisseurProduitId));
    }

    @GetMapping("/commandes/entree-stock/check-price-variation")
    public ResponseEntity<List<OrderLineDTO>> checkPriceVariation(
        @RequestParam Integer commandeId,
        @RequestParam String orderDate
    ) {
        List<OrderLineDTO> lignes = stockEntryService.findLignesAvecEcartPrix(commandeId, LocalDate.parse(orderDate));
        return ResponseEntity.ok(lignes);
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
