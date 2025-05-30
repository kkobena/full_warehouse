package com.kobe.warehouse.web.rest.dci;

import com.kobe.warehouse.service.dci.dto.DciDTO;
import com.kobe.warehouse.service.dci.service.DciService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

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
}
