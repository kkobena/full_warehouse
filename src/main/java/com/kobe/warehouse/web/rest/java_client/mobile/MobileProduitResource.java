package com.kobe.warehouse.web.rest.java_client.mobile;

import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.mobile.service.MobileProduitService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client/mobile/produits")
public class MobileProduitResource {

    private final MobileProduitService mobileProduitService;

    public MobileProduitResource(MobileProduitService mobileProduitService) {
        this.mobileProduitService = mobileProduitService;
    }

    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getData(@RequestParam(name = "search") String search) {
        return ResponseEntity.ok(mobileProduitService.searchProduits(search));
    }
}
