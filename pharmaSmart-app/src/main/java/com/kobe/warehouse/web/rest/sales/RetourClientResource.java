package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.service.receipt.service.RetourClientReceiptService;
import com.kobe.warehouse.service.sale.RetourAvoirDashboardService;
import com.kobe.warehouse.service.sale.RetourClientService;
import com.kobe.warehouse.service.sale.dto.RetourAvoirStatsDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientResultDTO;
import com.kobe.warehouse.service.sale.dto.SaleForRetourDTO;
import com.kobe.warehouse.service.sale.impl.RetourClientPdfService;
import java.time.YearMonth;
import com.kobe.warehouse.web.rest.Utils;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.kobe.warehouse.web.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api")
public class RetourClientResource {

    private final RetourClientService retourClientService;
    private final RetourClientPdfService retourClientPdfService;
    private final RetourClientReceiptService retourClientReceiptService;
    private final RetourAvoirDashboardService dashboardService;

    public RetourClientResource(
        RetourClientService retourClientService,
        RetourClientPdfService retourClientPdfService,
        RetourClientReceiptService retourClientReceiptService,
        RetourAvoirDashboardService dashboardService
    ) {
        this.retourClientService = retourClientService;
        this.retourClientPdfService = retourClientPdfService;
        this.retourClientReceiptService = retourClientReceiptService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/sales/retours/dashboard")
    public ResponseEntity<RetourAvoirStatsDTO> getDashboard(
        @RequestParam(required = false) String mois
    ) {
        YearMonth yearMonth = mois != null ? YearMonth.parse(mois) : null;
        return ResponseEntity.ok(dashboardService.getStats(yearMonth));
    }

    @PatchMapping("/sales/retours/{id}/echange-sale")
    public ResponseEntity<RetourClientDTO> lierVenteEchange(
        @PathVariable Integer id,
        @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(retourClientService.lierVenteEchange(id, body.get("saleRef")));
    }

    @GetMapping("/sales/retours/sale")
    public ResponseEntity<SaleForRetourDTO> getSaleForRetour(@RequestParam String ref) {
        return ResponseEntity.ok(retourClientService.findSaleByRef(ref));
    }

    @GetMapping("/sales/retours/sale/{id}")
    public ResponseEntity<SaleForRetourDTO> getSaleForRetourById(
        @PathVariable Long id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate
    ) {
        return ResponseEntity.ok(retourClientService.findSaleById(id, saleDate));
    }

    @GetMapping("/sales/retours")
    public ResponseEntity<List<RetourClientDTO>> getRetours(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        Pageable pageable
    ) {
        Page<RetourClientDTO> page = retourClientService.findAll(search, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/sales/retours")
    public ResponseEntity<RetourClientResultDTO> validerRetour(@RequestBody RetourClientRequest request) {
        return ResponseEntity.ok(retourClientService.validerRetour(request));
    }

    @GetMapping("/sales/retours/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable Integer id) {
        byte[] pdf = retourClientPdfService.generatePdf(id);
        return Utils.printPDF(pdf, "retour_client_" + id + ".pdf");
    }

    @GetMapping("/sales/retours/{id}/receipt/tauri")
    public ResponseEntity<byte[]> getReceiptForTauri(@PathVariable Integer id) {
        try {
            RetourClientDTO retour = retourClientService.findById(id);
            byte[] escPosData = retourClientReceiptService.generateEscPosReceiptForTauri(retour);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt.bin\"")
                .body(escPosData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/sales/retours/{id}/receipt/print")
    public ResponseEntity<Void> printReceipt(
        @PathVariable Integer id,
        @RequestParam(required = false) String hostName
    ) {
        RetourClientDTO retour = retourClientService.findById(id);
        retourClientReceiptService.printReceipt(hostName, retour);
        return ResponseEntity.ok().build();
    }
}
