package com.kobe.warehouse.web.rest.stat.produit;

import com.kobe.warehouse.service.dto.*;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import com.kobe.warehouse.service.stat.ProductStatService;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;

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
    public ResponseEntity<List<ProduitAuditingState>> fetchTransactions(@Valid ProduitAuditingParam produitAuditingParam) {
        return ResponseEntity.ok().body(productStatService.fetchProduitDailyTransaction(produitAuditingParam));
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

    private ProduitHistoriqueParam getProduitHistoriqueParam(
        Long produitId,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        return new ProduitHistoriqueParam(produitId, fromDate, toDate);

    }


    @GetMapping("/historique-vente-mensuelle")
    public ResponseEntity<List<HistoriqueProduitVenteMensuelleWrapper>> getProduitHistoriqueVenteMensuelle(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate

    ) {

        return ResponseEntity.ok().body(
            productStatService.getHistoriqueVenteMensuelle(getProduitHistoriqueParam(produitId, fromDate, toDate))
        );
    }


    @GetMapping("/historique-achat-mensuelle")
    public ResponseEntity<List<HistoriqueProduitAchatMensuelleWrapper>> getProduitHistoriqueAchatMensuelle(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate

    ) {

        return ResponseEntity.ok().body(
            productStatService.getHistoriqueAchatMensuelle(getProduitHistoriqueParam(produitId, fromDate, toDate))
        );
    }


    @Transactional(readOnly = true)
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
}
