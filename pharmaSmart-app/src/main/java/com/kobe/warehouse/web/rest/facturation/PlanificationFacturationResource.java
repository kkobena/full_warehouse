package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.HistoriquePlanificationDto;
import com.kobe.warehouse.service.facturation.dto.PlanificationDto;
import com.kobe.warehouse.service.facturation.service.PlanificationFacturationService;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/planifications-facturation")
public class PlanificationFacturationResource {

    private final PlanificationFacturationService planificationService;

    public PlanificationFacturationResource(PlanificationFacturationService planificationService) {
        this.planificationService = planificationService;
    }

    @GetMapping
    public ResponseEntity<List<PlanificationDto>> findAll() {
        return ResponseEntity.ok(planificationService.findAll());
    }

    @PostMapping
    public ResponseEntity<PlanificationDto> create(@RequestBody PlanificationDto dto) {
        PlanificationDto result = planificationService.create(dto);
        return ResponseEntity
            .created(URI.create("/api/planifications-facturation/" + result.id()))
            .body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanificationDto> update(@PathVariable Integer id, @RequestBody PlanificationDto dto) {
        PlanificationDto result = planificationService.update(id, dto);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/toggle-actif")
    public ResponseEntity<Void> toggleActif(@PathVariable Integer id) {
        planificationService.toggleActif(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        planificationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/executer-maintenant")
    public ResponseEntity<FactureEditionResponse> executerMaintenant(@PathVariable Integer id) {
        FactureEditionResponse response = planificationService.executerMaintenant(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/historique")
    public ResponseEntity<List<HistoriquePlanificationDto>> getHistorique(
        @PathVariable Integer id,
        Pageable pageable
    ) {
        Page<HistoriquePlanificationDto> page = planificationService.getHistorique(id, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
