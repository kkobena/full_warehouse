package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public class ProduitResourceProxy {
  private final ProduitService produitService;

  public ProduitResourceProxy(ProduitService produitService) {
    this.produitService = produitService;
  }

  public ResponseEntity<List<ProduitDTO>> getAllLite(
      ProduitCriteria produitCriteria, Pageable pageable) {
    return ResponseEntity.ok().body(produitService.productsLiteList(produitCriteria, pageable));
  }
}
