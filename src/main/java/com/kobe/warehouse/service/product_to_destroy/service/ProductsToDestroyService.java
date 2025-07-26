package com.kobe.warehouse.service.product_to_destroy.service;

import com.kobe.warehouse.service.dto.records.Keys;
import com.kobe.warehouse.service.excel.model.ExportFormat;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyPayload;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductsToDestroyPayload;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ProductsToDestroyService {
    void addLotQuantities(ProductsToDestroyPayload productToDestroyPayload);

    Page<ProductToDestroyDTO> findAll(ProductToDestroyFilter produidToDestroyFilter, Pageable pageable);

    ProductToDestroySumDTO getSum(ProductToDestroyFilter produidToDestroyFilter);

    void destroy(Keys keys);

    void remove(Keys keys);

    /**
     * Ajout à partir d'un formulaire
     */
    void addProductQuantity(ProductToDestroyPayload addPerimePayload);

    void closeLastEdition();

    void modifyProductQuantity(ProductToDestroyPayload productToDestroyPayload);

    Page<ProductToDestroyDTO> findEditing(ProductToDestroyFilter produidToDestroyFilter, Pageable pageable);

    void export(HttpServletResponse response, ExportFormat type, ProductToDestroyFilter produidToDestroyFilter) throws IOException;

    ResponseEntity<byte[]> generatePdf(ProductToDestroyFilter produidToDestroyFilter);
}
