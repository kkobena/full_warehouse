package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.DsoOrganismeDTO;
import com.kobe.warehouse.service.dto.report.EncoursMensuelDTO;
import com.kobe.warehouse.service.dto.report.VieillissementGlobalDTO;
import com.kobe.warehouse.service.report.VieillissementCreancesService;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/vieillissement-creances")
public class VieillissementCreancesResource {

    private final VieillissementCreancesService service;

    public VieillissementCreancesResource(VieillissementCreancesService service) {
        this.service = service;
    }

    @GetMapping("/global")
    public ResponseEntity<VieillissementGlobalDTO> getAgingGlobal() {
        return ResponseEntity.ok(service.getAgingGlobal());
    }

    @GetMapping("/dso-organisme")
    public ResponseEntity<List<DsoOrganismeDTO>> getDsoByOrganisme(
        @PageableDefault(size = 15) Pageable pageable) {
        Page<DsoOrganismeDTO> page = service.getDsoByOrganisme(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/encours-evolution")
    public ResponseEntity<EncoursMensuelDTO> getEncoursMensuelEvolution() {
        return ResponseEntity.ok(service.getEncoursMensuelEvolution());
    }
}
