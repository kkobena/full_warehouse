package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.service.dto.CodeRemiseDTO;
import com.kobe.warehouse.service.dto.GrilleRemiseDTO;
import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.dto.RemiseProduitsDTO;
import com.kobe.warehouse.service.dto.TypeRemise;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.remise.RemiseService;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Remise}.
 */
public class RemiseResourceProxy {

    private static final String ENTITY_NAME = "remise";
    private final Logger log = LoggerFactory.getLogger(RemiseResourceProxy.class);
    private final RemiseService remiseService;

    public RemiseResourceProxy(RemiseService remiseService) {
        this.remiseService = remiseService;
    }

    @GetMapping("/remises")
    public ResponseEntity<List<RemiseDTO>> getAll(
        @RequestParam(required = false, name = "typeRemise", defaultValue = "ALL") TypeRemise typeRemise
    ) {
        return ResponseEntity.ok().body(this.remiseService.findAll(typeRemise));
    }

    public ResponseEntity<RemiseDTO> create(RemiseDTO remiseDTO) throws URISyntaxException {
        if (remiseDTO.getId() != null) {
            throw new BadRequestAlertException("A new remiseDTO cannot already have an ID", ENTITY_NAME, "idexists");
        }
        RemiseDTO result = this.remiseService.save(remiseDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    public ResponseEntity<RemiseDTO> update(RemiseDTO remiseDTO) throws URISyntaxException {
        if (remiseDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        RemiseDTO result = this.remiseService.save(remiseDTO);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    public ResponseEntity<RemiseDTO> getOne(Integer id) {
        log.debug("REST request to get Remise : {}", id);
        Optional<RemiseDTO> remiseDTO = this.remiseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(remiseDTO);
    }

    public ResponseEntity<Void> delete(Integer id) {
        log.debug("REST request to delete Remise : {}", id);

        this.remiseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<RemiseDTO> changeStatus(RemiseDTO remiseDTO) throws URISyntaxException {
        if (remiseDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        RemiseDTO result = this.remiseService.changeStatus(remiseDTO);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
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
