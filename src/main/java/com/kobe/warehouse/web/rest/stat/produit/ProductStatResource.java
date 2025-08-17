package com.kobe.warehouse.web.rest.stat.produit;

import com.kobe.warehouse.service.dto.HistoriqueProduitAchatMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchats;
import com.kobe.warehouse.service.dto.HistoriqueProduitAchatsSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVente;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleWrapper;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteSummary;
import com.kobe.warehouse.service.dto.ProduitHistoriqueParam;
import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import com.kobe.warehouse.service.stat.ProductStatService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/produits/stat")
public class ProductStatResource {

    private final ProductStatService productStatService;

    public ProductStatResource(ProductStatService productStatService) {
        this.productStatService = productStatService;
    }

    @GetMapping("/ca")
    public ResponseEntity<List<ProductStatRecord>> fetchProductStat(@Valid ProduitRecordParamDTO produitRecordParam) {
        return ResponseEntity.ok().body(productStatService.fetchProductStat(produitRecordParam));
    }

    @GetMapping("/vingt-quantre-vingt")
    public ResponseEntity<List<ProductStatParetoRecord>> fetch20x80(@Valid ProduitRecordParamDTO produitRecordParam) {
        return ResponseEntity.ok().body(productStatService.fetch20x80(produitRecordParam));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<ProduitAuditingState>> fetchTransactions(
        @Valid ProduitAuditingParam produitAuditingParam,
        Pageable pageable
    ) {
        Page<ProduitAuditingState> page = productStatService.fetchProduitDailyTransaction(produitAuditingParam, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/transactions/sum")
    public ResponseEntity<List<ProduitAuditingSum>> fetchTransactionsSum(@Valid ProduitAuditingParam produitAuditingParam) {
        return ResponseEntity.ok().body(productStatService.fetchProduitDailyTransactionSum(produitAuditingParam));
    }

    @PostMapping("/transactions/pdf")
    public ResponseEntity<Resource> getTransactionsPdf(
        @RequestBody @Valid ProduitAuditingParam produitAuditingParam,
        HttpServletRequest request
    ) throws MalformedURLException {
        Resource resource = this.productStatService.printToPdf(produitAuditingParam);
        return Utils.printPDF(resource, request);
    }

    @Transactional(readOnly = true)
    @GetMapping("/historique-vente")
    public ResponseEntity<List<HistoriqueProduitVente>> getProduitHistoriqueVente(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        Pageable pageable
    ) {
        Page<HistoriqueProduitVente> page = productStatService.getHistoriqueVente(
            getProduitHistoriqueParam(produitId, fromDate, toDate),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    private ProduitHistoriqueParam getProduitHistoriqueParam(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return new ProduitHistoriqueParam(produitId, fromDate, toDate);
    }

    @GetMapping("/historique-vente-mensuelle")
    public ResponseEntity<List<HistoriqueProduitVenteMensuelleWrapper>> getProduitHistoriqueVenteMensuelle(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok()
            .body(productStatService.getHistoriqueVenteMensuelle(getProduitHistoriqueParam(produitId, fromDate, toDate)));
    }

    @GetMapping("/historique-achat-mensuelle")
    public ResponseEntity<List<HistoriqueProduitAchatMensuelleWrapper>> getProduitHistoriqueAchatMensuelle(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok()
            .body(productStatService.getHistoriqueAchatMensuelle(getProduitHistoriqueParam(produitId, fromDate, toDate)));
    }

    @GetMapping("/historique-achat")
    public ResponseEntity<List<HistoriqueProduitAchats>> getProduitHistoriqueAchat(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        Pageable pageable
    ) {
        Page<HistoriqueProduitAchats> page = productStatService.getHistoriqueAchat(
            getProduitHistoriqueParam(produitId, fromDate, toDate),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/historique-vente-mensuelle-summary")
    public ResponseEntity<HistoriqueProduitVenteMensuelleSummary> getHistoriqueVenteMensuelleSummary(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok()
            .body(productStatService.getHistoriqueVenteMensuelleSummary(getProduitHistoriqueParam(produitId, fromDate, toDate)));
    }

    @GetMapping("/historique-achat-summary")
    public ResponseEntity<HistoriqueProduitAchatsSummary> getHistoriqueAchatSummary(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok()
            .body(productStatService.getHistoriqueAchatSummary(getProduitHistoriqueParam(produitId, fromDate, toDate)));
    }

    @GetMapping("/historique-vente-summary")
    public ResponseEntity<HistoriqueProduitVenteSummary> getHistoriqueVenteSummary(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok()
            .body(productStatService.getHistoriqueVenteSummary(getProduitHistoriqueParam(produitId, fromDate, toDate)));
    }

    @GetMapping("/historique-vente/pdf")
    public ResponseEntity<Resource> exportHistoriqueVenteToPdf(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        HttpServletRequest request
    ) {
        return Utils.printPDF(
            this.productStatService.exportHistoriqueVenteToPdf(getProduitHistoriqueParam(produitId, fromDate, toDate)),
            request
        );
    }

    @GetMapping("/historique-achat/pdf")
    public ResponseEntity<Resource> exportHistoriqueAchatToPdf(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        HttpServletRequest request
    ) {
        return Utils.printPDF(
            this.productStatService.exportHistoriqueAchatToPdf(getProduitHistoriqueParam(produitId, fromDate, toDate)),
            request
        );
    }

    @GetMapping("/historique-vente-mensuelle/pdf")
    public ResponseEntity<Resource> exportHistoriqueVenteMensuelleToPdf(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        HttpServletRequest request
    ) {
        return Utils.printPDF(
            this.productStatService.exportHistoriqueVenteMensuelleToPdf(getProduitHistoriqueParam(produitId, fromDate, toDate)),
            request
        );
    }

    @GetMapping("/historique-achat-mensuelle/pdf")
    public ResponseEntity<Resource> exportHistoriqueAchatMensuelToPdf(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        HttpServletRequest request
    ) {
        return Utils.printPDF(
            this.productStatService.exportHistoriqueAchatMensuelToPdf(getProduitHistoriqueParam(produitId, fromDate, toDate)),
            request
        );
    }
}
