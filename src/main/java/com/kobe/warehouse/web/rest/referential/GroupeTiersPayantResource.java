package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.domain.Categorie;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.service.GroupeTiersPayantService;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing {@link Categorie}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class GroupeTiersPayantResource {

    private static final String ENTITY_NAME = "GroupeTiersPayant";
    private final Logger log = LoggerFactory.getLogger(GroupeTiersPayantResource.class);
    private final GroupeTiersPayantService groupeTiersPayantService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public GroupeTiersPayantResource(GroupeTiersPayantService groupeTiersPayantService) {
        this.groupeTiersPayantService = groupeTiersPayantService;
    }

    @PostMapping("/groupe-tierspayants")
    public ResponseEntity<GroupeTiersPayant> createGroupeTiersPayant(@Valid @RequestBody GroupeTiersPayant groupeTiersPayant)
        throws URISyntaxException {
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
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/groupe-tierspayants/importcsv")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importcsv") MultipartFile file) throws URISyntaxException, IOException {
        ResponseDTO responseDTO = groupeTiersPayantService.importation(file.getInputStream());
        return ResponseEntity.ok().body(responseDTO);
    }
}
