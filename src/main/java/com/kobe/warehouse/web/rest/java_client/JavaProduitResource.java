package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.web.rest.proxy.ProduitResourceProxy;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing {@link com.kobe.warehouse.domain.Produit}. */
@RestController
@RequestMapping("/java-client")
public class JavaProduitResource extends ProduitResourceProxy {
  private final UserService userService;

  public JavaProduitResource(ProduitService produitService, UserService userService) {
    super(produitService);
    this.userService = userService;
  }

  @GetMapping("/produits/lite")
  public ResponseEntity<List<ProduitDTO>> getAllLite(
      @RequestParam(required = false, name = "search") String search,
      @RequestParam(required = false, name = "storageId") Long storageId,
      @RequestParam(required = false, name = "rayonId") Long rayonId,
      @RequestParam(required = false, name = "deconditionne") Boolean deconditionne,
      @RequestParam(required = false, name = "deconditionnable") Boolean deconditionnable,
      @RequestParam(required = false, name = "status") Status status,
      @RequestParam(required = false, name = "familleId") Long familleId,
      @RequestParam(required = false, name = "tableauId") Long tableauId,
      @RequestParam(required = false, name = "tableauNot") Long tableauNot,
      @RequestParam(required = false, name = "remiseId") Long remiseId,
      @RequestParam(required = false, name = "remiseNot") Long remiseNot,
      Pageable pageable) {

    userService
        .getUserConnectedWithAuthorities()
        .orElseThrow(() -> new RuntimeException("User could not be found"));
    return super.getAllLite(
        new ProduitCriteria()
            .setSearch(search)
            .setStatus(status)
            .setDeconditionnable(deconditionnable)
            .setDeconditionne(deconditionne)
            .setFamilleId(familleId)
            .setTableauId(tableauId)
            .setTableauNot(tableauNot)
            .setRayonId(rayonId)
            .setStorageId(storageId)
            .setRemiseNot(remiseNot)
            .setRemiseId(remiseId),
        pageable);
  }
}
