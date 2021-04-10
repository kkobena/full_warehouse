package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.InventoryTransaction}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class InventoryTransactionResource {
    private final Logger log = LoggerFactory.getLogger(InventoryTransactionResource.class);
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryTransactionResource(InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }


    @GetMapping("/inventory-transactions")
    public ResponseEntity<List<InventoryTransactionDTO>> getAllInventoryTransactions(
    		@RequestParam(name = "produitId",required = false) Long produitId,
    		Pageable pageable) {
        List<InventoryTransactionDTO> datas=new ArrayList<>();
       if(produitId!=null){
           datas = inventoryTransactionRepository.
               findByProduitId(produitId,Sort.by(Direction.ASC, "createdAt")).stream().map(InventoryTransactionDTO::new).collect(Collectors.toList());
       }else{
           datas =    inventoryTransactionRepository.findAll(Sort.by(Direction.ASC, "createdAt")).stream().map(InventoryTransactionDTO::new).collect(Collectors.toList());
       }

        return ResponseEntity.ok().body(datas);
    }


    @GetMapping("/inventory-transactions/{id}")
    public ResponseEntity<InventoryTransaction> getInventoryTransaction(@PathVariable Long id) {
        log.debug("REST request to get InventoryTransaction : {}", id);
        Optional<InventoryTransaction> inventoryTransaction = inventoryTransactionRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(inventoryTransaction);
    }

}
