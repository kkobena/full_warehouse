package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.stock.StockEntryDataService;
import com.kobe.warehouse.web.rest.Utils;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api")
@Transactional
public class StockEntryDataResource {

  private final StockEntryDataService stockEntryDataServicetryService;

  public StockEntryDataResource(StockEntryDataService stockEntryDataServicetryService) {
    this.stockEntryDataServicetryService = stockEntryDataServicetryService;
  }

  @GetMapping("/commandes/data/entree-stock/list")
  public ResponseEntity<List<DeliveryReceiptDTO>> list(
      @RequestParam(required = false, name = "fromDate") LocalDate fromDate,
      @RequestParam(required = false, name = "toDate") LocalDate toDate,
      @RequestParam(required = false, name = "search") String search,
      @RequestParam(required = false, name = "fournisseurId") Long fournisseurId,
      @RequestParam(required = false, name = "userId") Long userId,
      @RequestParam(required = false, name = "statut", defaultValue = "ANY") ReceiptStatut statut,
      Pageable pageable) {
    Page<DeliveryReceiptDTO> page =
        stockEntryDataServicetryService.fetchAllReceipts(
            DeliveryReceiptFilterDTO.builder()
                .fromDate(fromDate)
                .fournisseurId(fournisseurId)
                .userId(userId)
                .search(search)
                .toDate(toDate)
                .statut(statut)
                .build(),
            pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  @GetMapping("/commandes/data/entree-stock/{id}")
  public ResponseEntity<DeliveryReceiptDTO> getOne(@PathVariable Long id) {
    return ResponseUtil.wrapOrNotFound(stockEntryDataServicetryService.findOneById(id));
  }

  @GetMapping("/commandes/data/entree-stock/etiquettes/{id}")
  public ResponseEntity<Resource> getPdf(
      @PathVariable Long id,
      @RequestParam(required = false, name = "startAt", defaultValue = "1") Integer startAt,
      HttpServletRequest request)
      throws IOException {
    final Resource resource = stockEntryDataServicetryService.printEtiquette(id, startAt);
    return Utils.printPDF(resource, request);
  }

  @GetMapping("/commandes/data/entree-stock/by-order-reference{reference}")
  public ResponseEntity<DeliveryReceiptDTO> getOne(@PathVariable String reference) {
    return ResponseUtil.wrapOrNotFound(
        stockEntryDataServicetryService.findOneByOrderReference(reference));
  }

  @GetMapping("/commandes/data/entree-stock/pdf/{id}")
  public ResponseEntity<Resource> getPdf(@PathVariable Long id, HttpServletRequest request)
      throws IOException {
    final Resource resource = stockEntryDataServicetryService.exportToPdf(id);
    return Utils.printPDF(resource, request);
  }
}
