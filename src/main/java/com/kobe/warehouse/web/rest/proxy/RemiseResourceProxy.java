package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.remise.RemiseService;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tech.jhipster.web.util.ResponseUtil;

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

    public ResponseEntity<List<RemiseDTO>> getAll() {
        return ResponseEntity.ok().body(this.remiseService.findAll());
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

    public ResponseEntity<RemiseDTO> getOne(Long id) {
        log.debug("REST request to get Remise : {}", id);
        Optional<RemiseDTO> remiseDTO = this.remiseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(remiseDTO);
    }

    public ResponseEntity<Void> delete(Long id) {
        log.debug("REST request to delete Remise : {}", id);

        this.remiseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<Void> associer(Long id, List<Long> produitIds) {
        this.remiseService.associer(id, produitIds);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> dissocier(List<Long> produitIds) {
        this.remiseService.dissocier(produitIds);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<RemiseDTO> changeStatus(RemiseDTO remiseDTO) throws URISyntaxException {
        if (remiseDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        RemiseDTO result = this.remiseService.changeStatus(remiseDTO);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
}
