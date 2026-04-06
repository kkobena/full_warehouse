package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.domain.HistoriqueCertificationFne;
import com.kobe.warehouse.domain.PlanificationCertificationFne;
import com.kobe.warehouse.service.fne.service.PlanificationCertificationFneService;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/planification-certification-fne")
public class PlanificationCertificationFneResource {

    private final PlanificationCertificationFneService service;

    public PlanificationCertificationFneResource(PlanificationCertificationFneService service) {
        this.service = service;
    }

    @GetMapping("/first")
    public ResponseEntity<PlanificationCertificationFne> getFirst() {
        Optional<PlanificationCertificationFne> plan = service.findFirst();
        return plan.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/toggle-actif")
    public ResponseEntity<PlanificationCertificationFne> toggleActif(@PathVariable Integer id) {
        PlanificationCertificationFne plan = service.toggleActif(id);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/{id}/executer-maintenant")
    public ResponseEntity<Void> executerMaintenant(@PathVariable Integer id) {
        service.executerMaintenant(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historique")
    public ResponseEntity<java.util.List<HistoriqueCertificationFne>> getHistorique(
        @PathVariable Integer id,
        Pageable pageable
    ) {
        Page<HistoriqueCertificationFne> page = service.getHistorique(id, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
