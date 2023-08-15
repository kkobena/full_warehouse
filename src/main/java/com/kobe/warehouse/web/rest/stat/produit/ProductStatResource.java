package com.kobe.warehouse.web.rest.stat.produit;

import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import com.kobe.warehouse.service.stat.ProductStatService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
  public ResponseEntity<List<ProductStatRecord>> fetchProductStat(
      @Valid ProduitRecordParamDTO produitRecordParam) {
    return ResponseEntity.ok().body(productStatService.fetchProductStat(produitRecordParam));
  }

  @GetMapping("/vingt-quantre-vingt")
  public ResponseEntity<List<ProductStatParetoRecord>> fetch20x80(
      @Valid ProduitRecordParamDTO produitRecordParam) {
    return ResponseEntity.ok().body(productStatService.fetch20x80(produitRecordParam));
  }
}
