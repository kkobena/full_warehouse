package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.service.GroupeFournisseurService;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/** REST controller for managing {@link com.kobe.warehouse.domain.GroupeFournisseur}. */
@RestController
@RequestMapping("/api")
public class GroupeFournisseurResource {

  private static final String ENTITY_NAME = "groupeFournisseur";
  private final Logger log = LoggerFactory.getLogger(GroupeFournisseurResource.class);
  private final GroupeFournisseurService groupeFournisseurService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public GroupeFournisseurResource(GroupeFournisseurService groupeFournisseurService) {
    this.groupeFournisseurService = groupeFournisseurService;
  }

  /**
   * {@code POST /groupe-fournisseurs} : Create a new groupeFournisseur.
   *
   * @param groupeFournisseurDTO the groupeFournisseurDTO to create.
   * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
   *     groupeFournisseurDTO, or with status {@code 400 (Bad Request)} if the groupeFournisseur has
   *     already an ID.
   * @throws URISyntaxException if the Location URI syntax is incorrect.
   */
  @PostMapping("/groupe-fournisseurs")
  public ResponseEntity<GroupeFournisseurDTO> createGroupeFournisseur(
      @Valid @RequestBody GroupeFournisseurDTO groupeFournisseurDTO) throws URISyntaxException {
    log.debug("REST request to save GroupeFournisseur : {}", groupeFournisseurDTO);
    if (groupeFournisseurDTO.getId() != null) {
      throw new BadRequestAlertException(
          "A new groupeFournisseur cannot already have an ID", ENTITY_NAME, "idexists");
    }
    GroupeFournisseurDTO result = groupeFournisseurService.save(groupeFournisseurDTO);
    return ResponseEntity.created(new URI("/api/groupe-fournisseurs/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  /**
   * {@code PUT /groupe-fournisseurs} : Updates an existing groupeFournisseur.
   *
   * @param groupeFournisseurDTO the groupeFournisseurDTO to update.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
   *     groupeFournisseurDTO, or with status {@code 400 (Bad Request)} if the groupeFournisseurDTO
   *     is not valid, or with status {@code 500 (Internal Server Error)} if the
   *     groupeFournisseurDTO couldn't be updated.
   * @throws URISyntaxException if the Location URI syntax is incorrect.
   */
  @PutMapping("/groupe-fournisseurs")
  public ResponseEntity<GroupeFournisseurDTO> updateGroupeFournisseur(
      @Valid @RequestBody GroupeFournisseurDTO groupeFournisseurDTO) throws URISyntaxException {
    log.debug("REST request to update GroupeFournisseur : {}", groupeFournisseurDTO);
    if (groupeFournisseurDTO.getId() == null) {
      throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
    }
    GroupeFournisseurDTO result = groupeFournisseurService.save(groupeFournisseurDTO);
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityUpdateAlert(
                applicationName, true, ENTITY_NAME, groupeFournisseurDTO.getId().toString()))
        .body(result);
  }

  /**
   * {@code GET /groupe-fournisseurs} : get all the groupeFournisseurs.
   *
   * @param pageable the pagination information.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of
   *     groupeFournisseurs in body.
   */
  @GetMapping(value = "/groupe-fournisseurs")
  public ResponseEntity<List<GroupeFournisseurDTO>> getAllGroupeFournisseurs(
      @RequestParam(value = "search", required = false) String search, Pageable pageable) {
    log.debug("REST request to get a page of GroupeFournisseurs");
    Page<GroupeFournisseurDTO> page = groupeFournisseurService.findAll(search, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  /**
   * {@code GET /groupe-fournisseurs/:id} : get the "id" groupeFournisseur.
   *
   * @param id the id of the groupeFournisseurDTO to retrieve.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the
   *     groupeFournisseurDTO, or with status {@code 404 (Not Found)}.
   */
  @GetMapping("/groupe-fournisseurs/{id}")
  public ResponseEntity<GroupeFournisseurDTO> getGroupeFournisseur(@PathVariable Long id) {
    log.debug("REST request to get GroupeFournisseur : {}", id);
    Optional<GroupeFournisseurDTO> groupeFournisseurDTO = groupeFournisseurService.findOne(id);
    return ResponseUtil.wrapOrNotFound(groupeFournisseurDTO);
  }

  /**
   * {@code DELETE /groupe-fournisseurs/:id} : delete the "id" groupeFournisseur.
   *
   * @param id the id of the groupeFournisseurDTO to delete.
   * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
   */
  @DeleteMapping("/groupe-fournisseurs/{id}")
  public ResponseEntity<Void> deleteGroupeFournisseur(@PathVariable Long id) {
    log.debug("REST request to delete GroupeFournisseur : {}", id);

    groupeFournisseurService.delete(id);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
        .build();
  }

  @PostMapping("/groupe-fournisseurs/importcsv")
  public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importcsv") MultipartFile file)
      throws URISyntaxException, IOException {
    ResponseDTO responseDTO = groupeFournisseurService.importation(file.getInputStream());
    return ResponseEntity.ok().body(responseDTO);
  }
}
