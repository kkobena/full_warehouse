package com.kobe.warehouse.web.rest.dci;

import com.kobe.warehouse.service.dci.dto.DciDTO;
import com.kobe.warehouse.service.dci.dto.DciProduitDTO;
import com.kobe.warehouse.service.dci.service.DciService;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/dci")
public class DciResource {

    private final DciService dciService;

    public DciResource(DciService dciService) {
        this.dciService = dciService;
    }

    @GetMapping("/unpaged")
    public ResponseEntity<List<DciDTO>> getAllUnpaged(@RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok().body(dciService.findAll(search, Pageable.unpaged()).getContent());
    }

    @GetMapping
    public ResponseEntity<List<DciDTO>> getAll(@RequestParam(name = "search", required = false) String search, Pageable pageable) {
        Page<DciDTO> page = dciService.findAll(search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DciDTO> getOne(@PathVariable Integer id) {
        return ResponseUtil.wrapOrNotFound(dciService.findOne(id));
    }

    /** Produits portant cette substance active — alimente le panneau de détail. */
    @GetMapping("/{id}/produits")
    public ResponseEntity<List<DciProduitDTO>> getProduits(@PathVariable Integer id) {
        return ResponseEntity.ok().body(dciService.findProduits(id));
    }

    @PostMapping
    public ResponseEntity<DciDTO> create(@RequestBody DciDTO dto) throws URISyntaxException {
        DciDTO result = dciService.create(dto);
        return ResponseEntity.created(new URI("/api/dci/" + result.getId())).body(result);
    }

    @PutMapping
    public ResponseEntity<DciDTO> update(@RequestBody DciDTO dto) {
        return ResponseEntity.ok().body(dciService.update(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        dciService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Importe un CSV « code;libelle ». Le libellé est obligatoire, le code facultatif : lorsqu'il
     * est absent, il est dérivé du libellé.
     */
    @PostMapping(path = "/importcsv", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importcsv") MultipartFile file) throws IOException {
        return ResponseUtil.wrapOrNotFound(Optional.of(dciService.importation(file.getInputStream())));
    }
}
