package com.kobe.warehouse.web.rest.product_to_destroy;

import com.kobe.warehouse.service.dto.records.Keys;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyPayload;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductsToDestroyPayload;
import com.kobe.warehouse.service.product_to_destroy.service.ProductsToDestroyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/product-to-destroy")
public class ProductToDestroyResource {

    private final ProductsToDestroyService productsToDestroyService;

    public ProductToDestroyResource(ProductsToDestroyService productsToDestroyService) {
        this.productsToDestroyService = productsToDestroyService;
    }

    @PostMapping
    public ResponseEntity<Void> addLotQuantities(@Valid @RequestBody ProductsToDestroyPayload payload) {
        productsToDestroyService.addLotQuantities(payload);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductToDestroyDTO>> fetchAll(ProductToDestroyFilter productToDestroyFilter, Pageable pageable) {
        Page<ProductToDestroyDTO> page = productsToDestroyService.findAll(productToDestroyFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/sum")
    public ResponseEntity<ProductToDestroySumDTO> fetchSum(ProductToDestroyFilter productToDestroyFilter) {
        return ResponseEntity.ok().body(productsToDestroyService.getSum(productToDestroyFilter));
    }

    @PostMapping("/destroy")
    public ResponseEntity<Void> destroy(@RequestBody Keys keys) {
        productsToDestroyService.destroy(keys);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/add-product")
    public ResponseEntity<Void> addProductQuantity(@Valid @RequestBody ProductToDestroyPayload payload) {
        productsToDestroyService.addProductQuantity(payload);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/close")
    public ResponseEntity<Void> closeLastEdition() {
        productsToDestroyService.closeLastEdition();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/modify-product")
    public ResponseEntity<Void> modifyProductQuantity(@Valid @RequestBody ProductToDestroyPayload payload) {
        productsToDestroyService.modifyProductQuantity(payload);
        return ResponseEntity.accepted().build();
    }
}
