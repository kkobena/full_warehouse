package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Categorie;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.repository.CategorieRepository;
import com.kobe.warehouse.service.GroupeTiersPayantService;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link Categorie}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class GroupeTiersPayantResource {

    private final Logger log = LoggerFactory.getLogger(GroupeTiersPayantResource.class);

    private static final String ENTITY_NAME = "GroupeTiersPayant";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final GroupeTiersPayantService groupeTiersPayantService;

    public GroupeTiersPayantResource(GroupeTiersPayantService groupeTiersPayantService) {
        this.groupeTiersPayantService = groupeTiersPayantService;
    }


    @PostMapping("/groupe-tierspayants")
    public ResponseEntity<GroupeTiersPayant> createGroupeTiersPayant(@Valid @RequestBody GroupeTiersPayant groupeTiersPayant) throws URISyntaxException {
        log.debug("REST request to save Categorie : {}", groupeTiersPayant);
        if (groupeTiersPayant.getId() != null) {
            throw new BadRequestAlertException("A new groupe tiers payant cannot already have an ID", ENTITY_NAME, "idexists");
        }
        GroupeTiersPayant result = groupeTiersPayantService.create(groupeTiersPayant);
        return ResponseEntity.created(new URI("/api/groupe-tierspayants/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }


    @PutMapping("/groupe-tierspayants")
    public ResponseEntity<GroupeTiersPayant> update(@Valid @RequestBody GroupeTiersPayant groupeTiersPayant) throws URISyntaxException {
        log.debug("REST request to update GroupeTiersPayant : {}", groupeTiersPayant);
        if (groupeTiersPayant.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        GroupeTiersPayant result = groupeTiersPayantService.update(groupeTiersPayant);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupeTiersPayant.getId().toString()))
            .body(result);
    }


    @GetMapping("/groupe-tierspayants")
    public ResponseEntity<List<GroupeTiersPayant>> getAll(@RequestParam(value = "search", required = false) String search) {
        log.debug("REST request to get a page of GroupeTiersPayant");
        List<GroupeTiersPayant> list = groupeTiersPayantService.list(search);

        return ResponseEntity.ok().body(list);
    }


    @GetMapping("/groupe-tierspayants/{id}")
    public ResponseEntity<GroupeTiersPayant> getOne(@PathVariable Long id) {
        log.debug("REST request to get GroupeTiersPayant : {}", id);
        Optional<GroupeTiersPayant> groupeTiersPayant = groupeTiersPayantService.getOne(id);
        return ResponseUtil.wrapOrNotFound(groupeTiersPayant);
    }

    @DeleteMapping("/groupe-tierspayants/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to delete GroupeTiersPayant : {}", id);
        groupeTiersPayantService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }
    @PostMapping("/groupe-tierspayants/importcsv")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importcsv") MultipartFile file) throws URISyntaxException, IOException {
        ResponseDTO responseDTO = groupeTiersPayantService.importation(file.getInputStream());
        return ResponseEntity.ok().body(responseDTO);
    }
}
