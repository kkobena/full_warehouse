package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.DeliveryTotalsDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptItemProjection;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import com.kobe.warehouse.service.stock.StockEntryDataService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.security.navaccess.NavAction;

@RestController
@RequestMapping("/api")
@RequiresNavAccess({ "commande", "depot.achat-depot" })
public class StockEntryDataResource {

    private final StockEntryDataService stockEntryDataServicetryService;

    public StockEntryDataResource(StockEntryDataService stockEntryDataServicetryService) {
        this.stockEntryDataServicetryService = stockEntryDataServicetryService;
    }

    /** Un format absent de la requête vaut le format par défaut. */
    private FormatEtiquette formatOuDefaut(FormatEtiquette format) {
        return format == null ? FormatEtiquette.parDefaut() : format;
    }

    /** 203 dpi tant que l'appelant ne dit rien : la résolution des rouleaux d'entrée de gamme. */
    private ResolutionEtiquette resolutionOuDefaut(ResolutionEtiquette resolution) {
        return resolution == null ? ResolutionEtiquette.parDefaut() : resolution;
    }

    @GetMapping("/commandes/data/entree-stock/list")
    public ResponseEntity<List<DeliveryReceiptDTO>> fetch(DeliveryReceiptFilterDTO receiptFilter, Pageable pageable) {
        Page<DeliveryReceiptDTO> page = stockEntryDataServicetryService.fetchAllReceipts(receiptFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/commandes/data/entree-stock/{id}/{orderDate}")
    public ResponseEntity<DeliveryReceiptDTO> getOne(@PathVariable Integer id, @PathVariable LocalDate orderDate) {
        return ResponseUtil.wrapOrNotFound(stockEntryDataServicetryService.findOneById(new CommandeId(id, orderDate)));
    }

    @GetMapping("/commandes/data/entree-stock/etiquettes/{id}/{orderDate}")
    public ResponseEntity<byte[]> getEtiquettesPdf(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate,
        @RequestParam(required = false, name = "startAt", defaultValue = "1") Integer startAt,
        @RequestParam(required = false) FormatEtiquette format
    ) {
        String fileName = "etiquettes_" + id + "_" + orderDate + ".pdf";
        return Utils.printPDF(
            stockEntryDataServicetryService.printEtiquette(new CommandeId(id, orderDate), startAt, formatOuDefaut(format)),
            fileName
        );
    }

    /** Étiquettes « lot » du bon : une par lot reçu, avec le GS1 DataMatrix. */
    @GetMapping("/commandes/data/entree-stock/etiquettes-lot/{id}/{orderDate}")
    public ResponseEntity<byte[]> getEtiquettesLotPdf(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate,
        @RequestParam(required = false, name = "startAt", defaultValue = "1") Integer startAt,
        @RequestParam(required = false) FormatEtiquette format
    ) {
        String fileName = "etiquettes_lot_" + id + "_" + orderDate + ".pdf";
        return Utils.printPDF(
            stockEntryDataServicetryService.printEtiquetteLot(new CommandeId(id, orderDate), startAt, formatOuDefaut(format)),
            fileName
        );
    }

    /**
     * {@code GET …/etiquettes/{id}/{orderDate}/zpl} : le même bon, en ZPL, pour une imprimante à
     * rouleau sans pilote exploitable. Le poste envoie le flux tel quel à l'imprimante.
     */
    @GetMapping(value = "/commandes/data/entree-stock/etiquettes/{id}/{orderDate}/zpl", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getEtiquettesZpl(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate,
        @RequestParam(required = false) FormatEtiquette format,
        @RequestParam(required = false) ResolutionEtiquette resolution
    ) {
        return ResponseEntity.ok()
            .body(
                stockEntryDataServicetryService.printEtiquetteZpl(
                    new CommandeId(id, orderDate),
                    formatOuDefaut(format),
                    resolutionOuDefaut(resolution)
                )
            );
    }

    /** Étiquettes « lot » du bon en ZPL : même chemin, avec le GS1 DataMatrix. */
    @GetMapping(value = "/commandes/data/entree-stock/etiquettes-lot/{id}/{orderDate}/zpl", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getEtiquettesLotZpl(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate,
        @RequestParam(required = false) FormatEtiquette format,
        @RequestParam(required = false) ResolutionEtiquette resolution
    ) {
        return ResponseEntity.ok()
            .body(
                stockEntryDataServicetryService.printEtiquetteLotZpl(
                    new CommandeId(id, orderDate),
                    formatOuDefaut(format),
                    resolutionOuDefaut(resolution)
                )
            );
    }

    @GetMapping("/commandes/data/entree-stock/pdf/{id}/{orderDate}")
    @RequiresNavAccess(value = { "commande", "depot.achat-depot" }, action = NavAction.EXPORT)
    public ResponseEntity<byte[]> getDeliveryReceiptPdf(@PathVariable Integer id, @PathVariable LocalDate orderDate) {
        String fileName = "bon_livraison_" + id + "_" + orderDate + ".pdf";
        return Utils.printPDF(stockEntryDataServicetryService.exportToPdf(new CommandeId(id, orderDate)), fileName);
    }

    @GetMapping("/commandes/data/entree-stock/list-bon-livraison")
    public ResponseEntity<List<DeliveryReceiptProjection>> fetchListBons(@RequestParam("search") String search) {
        return ResponseEntity.ok().body(stockEntryDataServicetryService.fetchAllReceipts(search).getContent());
    }

    @GetMapping("/commandes/data/entree-stock/filter-items/{id}/{orderDate}")
    public ResponseEntity<List<DeliveryReceiptItemProjection>> fetchDetailBons(
        @PathVariable Integer id,
        @PathVariable LocalDate orderDate
    ) {
        return ResponseEntity.ok()
            .body(stockEntryDataServicetryService.findAllByCommandeIdAndCommandeOrderDate(new CommandeId(id, orderDate)));
    }

    @GetMapping("/commandes")
    public ResponseEntity<List<DeliveryReceiptDTO>> fetchAll(DeliveryReceiptFilterDTO receiptFilter, Pageable pageable) {
        Page<DeliveryReceiptDTO> page = stockEntryDataServicetryService.fetchAllWithoutDetail(receiptFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/commandes/totaux")
    public ResponseEntity<DeliveryTotalsDTO> computeTotals(DeliveryReceiptFilterDTO receiptFilter) {
        return ResponseEntity.ok(stockEntryDataServicetryService.computeTotals(receiptFilter));
    }

    @GetMapping("/commandes/count")
    public ResponseEntity<Long> countByStatut(@RequestParam("statut") OrderStatut statut) {
        return ResponseEntity.ok(stockEntryDataServicetryService.countByOrderStatusAndType(statut));
    }
}
