package com.kobe.warehouse.web.rest.referential.remise;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.dto.CodeRemiseDTO;
import com.kobe.warehouse.service.dto.GrilleRemiseDTO;
import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.dto.RemiseProduitsDTO;
import com.kobe.warehouse.service.dto.TypeRemise;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.remise.RemiseService;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link PaymentMode}.
 */
@RestController
@RequestMapping("/api")
public class RemiseResource {
    private final RemiseService remiseService;

    public RemiseResource(RemiseService remiseService) {
        this.remiseService = remiseService;
    }

    @PostMapping("/remises")
    public ResponseEntity<RemiseDTO> create(@Valid @RequestBody RemiseDTO remiseDTO) throws URISyntaxException {
        if (remiseDTO.getId() != null) {
            throw new BadRequestAlertException("A new remiseDTO cannot already have an ID");
        }
        RemiseDTO result = this.remiseService.save(remiseDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/remises")
    public ResponseEntity<RemiseDTO> update(@Valid @RequestBody RemiseDTO remiseDTO) throws URISyntaxException {
        if (remiseDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id");
        }
        RemiseDTO result = this.remiseService.save(remiseDTO);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @GetMapping("/remises/{id}")
    public ResponseEntity<RemiseDTO> getOne(@PathVariable Integer id) {
        Optional<RemiseDTO> remiseDTO = this.remiseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(remiseDTO);
    }

    @DeleteMapping("/remises/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        this.remiseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/remises/change-status")
    public ResponseEntity<RemiseDTO> changeStatus(@Valid @RequestBody RemiseDTO remiseDTO) throws URISyntaxException {
        if (remiseDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id");
        }
        RemiseDTO result = this.remiseService.changeStatus(remiseDTO);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @GetMapping("/remises")
    public ResponseEntity<List<RemiseDTO>> getAll(
        @RequestParam(required = false, name = "typeRemise", defaultValue = "ALL") TypeRemise typeRemise
    ) {
        return ResponseEntity.ok().body(this.remiseService.findAll(typeRemise));
    }

    @GetMapping("/remises/codes")
    public ResponseEntity<List<CodeRemiseDTO>> getCodesRemise() {
        return ResponseEntity.ok().body(this.remiseService.findAllCodeRemise());
    }

    @GetMapping("/remises/grilles")
    public ResponseEntity<List<GrilleRemiseDTO>> findAllGrilles() {
        return ResponseEntity.ok().body(this.remiseService.findAllGrilles());
    }

    @GetMapping("/remises/full-codes")
    public ResponseEntity<List<CodeRemiseDTO>> queryFullCodes() {
        return ResponseEntity.ok().body(this.remiseService.queryFullCodes());
    }

    @PostMapping("/remises/associer")
    public ResponseEntity<Void> assosier(@Valid @RequestBody RemiseProduitsDTO remiseProduits) {
        this.remiseService.assosier(remiseProduits);
        return ResponseEntity.ok().build();
    }
}
