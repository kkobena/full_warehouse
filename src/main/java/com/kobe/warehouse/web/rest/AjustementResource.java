package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.repository.AjustService;
import com.kobe.warehouse.service.AjustementService;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.dto.filter.AjustementFilterRecord;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/** REST controller for managing {@link com.kobe.warehouse.domain.Ajustement}. */
@RestController
@RequestMapping("/api")
public class AjustementResource {

  private final Logger log = LoggerFactory.getLogger(AjustementResource.class);
  private final AjustService ajustService;
  private final AjustementService ajustementService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public AjustementResource(AjustService ajustService, AjustementService ajustementService) {
    this.ajustService = ajustService;
    this.ajustementService = ajustementService;
  }

  @GetMapping("/ajustements")
  public List<AjustementDTO> getAllAjustements(
      @RequestParam(name = "ajustementId") Long id,
      @RequestParam(required = false, name = "search") String search) {
    log.debug("REST request to get all Ajustements");

    return ajustementService.findAll(id, search).stream()
        .map(AjustementDTO::new)
        .collect(Collectors.toList());
  }

  @PostMapping("/ajustements")
  public ResponseEntity<AjustDTO> create(@Valid @RequestBody AjustDTO ajustDTO) {
    log.debug("REST request to save ajustDto : {}", ajustDTO);
    return ResponseEntity.ok().body(ajustementService.createAjsut(ajustDTO));
  }

  @PutMapping("/ajustements/item")
  public ResponseEntity<AjustementDTO> update(@Valid @RequestBody AjustementDTO ajustementDTO) {
    log.debug("REST request to save ajustementDTO : {}", ajustementDTO);
    ajustementDTO = ajustementService.update(ajustementDTO);
    return ResponseEntity.ok().body(ajustementDTO);
  }

  @PostMapping("/ajustements/save")
  public ResponseEntity<Void> save(@Valid @RequestBody AjustDTO ajustDto) {
    log.debug("REST request to save ajustDto : {}", ajustDto);
    ajustementService.saveAjust(ajustDto);
    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/ajustements/ajust")
  public ResponseEntity<List<AjustDTO>> getAllAjustts(
      AjustementFilterRecord ajustementFilterRecord, Pageable pageable) {
    Page<AjustDTO> page = ajustService.loadAll(ajustementFilterRecord, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  @DeleteMapping("/ajustements/item/{id}")
  public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
    ajustementService.deleteItem(id);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/ajustements/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    ajustementService.delete(id);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(
                applicationName, true, "ajustement", id.toString()))
        .build();
  }

  @PostMapping("/ajustements/item/add")
  public ResponseEntity<Void> addItem(@Valid @RequestBody AjustementDTO ajustement) {
    log.debug("REST request to save ajustement : {}", ajustement);
    ajustementService.createOrUpdate(ajustement);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/ajustements/delete/items")
  public ResponseEntity<Void> deleteAll(@RequestBody List<Long> ids) {
    ajustementService.deleteAll(ids);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/ajustements/pdf/{id}")
  public ResponseEntity<Resource> getPdf(@PathVariable Long id, HttpServletRequest request)
      throws IOException {
    final Resource resource = this.ajustService.exportToPdf(id);
    return Utils.printPDF(resource, request);
  }

  @GetMapping("/ajustements/{id}")
  public ResponseEntity<AjustDTO> getOne(@PathVariable Long id) {
    return ResponseUtil.wrapOrNotFound(ajustService.getOneById(id));
  }
}
