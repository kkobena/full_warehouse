package com.kobe.warehouse.web.rest.stat.produit;

import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import com.kobe.warehouse.service.stat.ProductStatService;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.MalformedURLException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
