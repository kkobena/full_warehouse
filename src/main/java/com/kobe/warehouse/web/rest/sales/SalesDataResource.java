package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.service.ReceiptPrinterService;
import com.kobe.warehouse.service.SaleDataService;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.report.SaleInvoiceService;
import com.kobe.warehouse.service.report.SaleReceiptService;
import com.kobe.warehouse.web.rest.Utils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class SalesDataResource {

  private static final String ENTITY_NAME = "sales";
  private final Logger log = LoggerFactory.getLogger(SalesDataResource.class);
  private final SaleDataService saleDataService;
  private final SaleInvoiceService saleInvoiceService;
  private final ReceiptPrinterService receiptPrinterService;
  private final SaleReceiptService saleReceiptService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public SalesDataResource(
      SaleDataService saleDataService,
      SaleInvoiceService saleInvoiceService,
      ReceiptPrinterService receiptPrinterService,
      SaleReceiptService saleReceiptService) {
    this.saleDataService = saleDataService;
    this.saleInvoiceService = saleInvoiceService;
    this.receiptPrinterService = receiptPrinterService;
    this.saleReceiptService = saleReceiptService;
  }

  /**
   * {@code GET /sales/:id} : get the "id" sales.
   *
   * @param id the id of the sales to retrieve.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the sales, or
   *     with status {@code 404 (Not Found)}.
   */
  @GetMapping("/sales/{id}")
  public ResponseEntity<SaleDTO> getSales(@PathVariable Long id) {
    log.debug("REST request to get Sales : {}", id);
    SaleDTO sale = saleDataService.fetchPurchaseBy(id);
    return ResponseEntity.ok().body(sale);
  }

  @GetMapping("/sales/edit/{id}")
  public ResponseEntity<SaleDTO> getSalesForEdit(@PathVariable Long id) {
    log.debug("REST request to get Sales : {}", id);
    Optional<SaleDTO> saleDTO = saleDataService.fetchPurchaseForEditBy(id);
    if (saleDTO.isEmpty()) {
      return ResponseEntity.ok().build();
    }
    return ResponseEntity.ok().body(saleDTO.get());
  }

  @GetMapping("/sales/print/invoice/{id}")
  public ResponseEntity<Resource> printInvoice(@PathVariable Long id, HttpServletRequest request)
      throws IOException {
    String gereratefilePath = saleDataService.printInvoice(id);
    Path filePath = Paths.get(gereratefilePath);
    Resource resource = new UrlResource(filePath.toUri());
    String contentType = null;
    try {
      contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
    } catch (IOException ex) {
      log.info("Could not determine file type.");
    }
    if (contentType == null) {
      contentType = "application/pdf";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + resource.getFilename() + "\"")
        .body(resource);
  }

  @GetMapping("/sales/prevente")
  public ResponseEntity<List<SaleDTO>> getAllSalesPreventes(
      @RequestParam(name = "search", required = false) String search,
      @RequestParam(name = "type", required = false) String typeVente,
      @RequestParam(name = "userId", required = false) Long userId) {
    log.debug("REST request to get a page of Sales");
    List<SaleDTO> data = saleDataService.allPrevente(search, typeVente,userId);
    return ResponseEntity.ok().body(data);
  }

  @GetMapping("/sales")
  public ResponseEntity<List<SaleDTO>> getAllSales(
      @RequestParam(name = "search", required = false) String search,
      @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
      @RequestParam(name = "toDate", required = false) LocalDate toDate,
      @RequestParam(name = "fromHour", required = false) String fromHour,
      @RequestParam(name = "toHour", required = false) String toHour,
      @RequestParam(name = "global", required = false) Boolean global,
      @RequestParam(name = "userId", required = false) Long userId,
      @RequestParam(name = "type", required = false) String type,
      Pageable pageable) {
    log.debug("REST request to get a page of Sales");
    Page<SaleDTO> page =
        saleDataService.listVenteTerminees(
            search, fromDate, toDate, fromHour, toHour, global, userId, type, null, null, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  @GetMapping("/sales/print/invoices/{id}")
  public ResponseEntity<Resource> printInvoices(@PathVariable Long id, HttpServletRequest request)
      throws IOException {
    String gereratefilePath = saleReceiptService.printCashReceipt(id);
    return Utils.printPDF(gereratefilePath, request);
  }

  /*  @GetMapping("/sales/print/invoice/{id}")
  public ResponseEntity<Resource> printCashReceipt(
      @PathVariable Long id, HttpServletRequest request) throws IOException {
    String gereratefilePath = saleReceiptService.printCashReceipt(143l);
    Path filePath = Paths.get(gereratefilePath);
    Resource resource = new UrlResource(filePath.toUri());
    String contentType = null;
    try {
      contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
    } catch (IOException ex) {
      log.info("Could not determine file type.");
    }
    if (contentType == null) {
      contentType = "application/pdf";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + resource.getFilename() + "\"")
        .body(resource);
  }*/

  @GetMapping("/sales/print/VNO-receipt/{id}")
  public ResponseEntity<Void> printCashReceipt(@PathVariable Long id)  {
    receiptPrinterService.printCashSale(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/sales/print/VO-receipt/{id}")
  public ResponseEntity<Void> printVoReceipt(@PathVariable Long id)  {
    receiptPrinterService.printVoSale(id);
    return ResponseEntity.ok().build();
  }
}
