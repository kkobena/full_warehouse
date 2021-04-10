package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.AjustementService;
import com.kobe.warehouse.service.dto.AjustementDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Ajustement}.
 */
@RestController
@RequestMapping("/api")
public class AjustementResource {

    private final Logger log = LoggerFactory.getLogger(AjustementResource.class);

    private final AjustementService ajustementService;

    public AjustementResource(AjustementService ajustementService) {
        this.ajustementService = ajustementService;
    }

    @GetMapping("/ajustements")
    public List<AjustementDTO> getAllAjustements(@RequestParam(required = false,name = "id") Long id) {
        log.debug("REST request to get all Ajustements");
        if(id==null){
            return ajustementService.findAll().stream().map(AjustementDTO::new).collect(Collectors.toList());
        }
        return ajustementService.findAll(id).stream().map(AjustementDTO::new).collect(Collectors.toList());

    }
    @GetMapping("/ajustements/all")
    public List<AjustementDTO> allAjustements(
       ) {

            return ajustementService.findAllSaved().stream().map(AjustementDTO::new).collect(Collectors.toList());

    }
    @PostMapping("/ajustements")
    public ResponseEntity<AjustementDTO> createAjustementDTO(@Valid @RequestBody AjustementDTO ajustementDTO)  {
        log.debug("REST request to save ajustementDTO : {}", ajustementDTO);
        ajustementDTO=   ajustementService.save(ajustementDTO);
        return ResponseEntity.ok().body(ajustementDTO);

    }
    @PutMapping ("/ajustements")
    public ResponseEntity<AjustementDTO> updateAjustementDTO(@Valid @RequestBody AjustementDTO ajustementDTO)  {
        log.debug("REST request to save ajustementDTO : {}", ajustementDTO);
        ajustementDTO=  ajustementService.update(ajustementDTO);
        return ResponseEntity.ok().body(ajustementDTO);

    }
    @PostMapping("/ajustements/save")
    public ResponseEntity<Void> saveAjustementDTO(@Valid @RequestBody AjustementDTO ajustementDTO)  {
        log.debug("REST request to save ajustementDTO : {}", ajustementDTO);
        ajustementService.save(ajustementDTO.getAjustId());
        return ResponseEntity.ok().build();

    }
}
