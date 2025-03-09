package com.kobe.warehouse.web.rest.activity_summary;

import com.kobe.warehouse.service.activity_summary.ActivitySummaryService;
import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.projection.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/activity-summary")
public class ActivitySummaryResource {

    private final ActivitySummaryService activitySummaryService;

    public ActivitySummaryResource(ActivitySummaryService activitySummaryService) {
        this.activitySummaryService = activitySummaryService;
    }

    @GetMapping("/ca")
    public ResponseEntity<ChiffreAffaireDTO> getCa(
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok().body(activitySummaryService.getChiffreAffaire(fromDate, toDate));
    }

    @GetMapping("/recettes")
    public ResponseEntity<List<Recette>> getRecettes(
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok().body(activitySummaryService.findRecettes(fromDate, toDate));
    }

    @GetMapping("/mouvements-caisse")
    public ResponseEntity<List<MouvementCaisse>> getMouvementsCaisse(
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok().body(activitySummaryService.findMouvementsCaisse(fromDate, toDate));
    }

    @GetMapping("/achats")
    public ResponseEntity<List<GroupeFournisseurAchat>> getAchats(
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        Pageable pageable
    ) {
        Page<GroupeFournisseurAchat> page = activitySummaryService.fetchAchats(fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/reglements-tiers-payants")
    public ResponseEntity<List<ReglementTiersPayants>> getReglementTiersPayants(
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        @RequestParam(name = "search", required = false) String search,
        Pageable pageable
    ) {
        Page<ReglementTiersPayants> page = activitySummaryService.findReglementTierspayant(fromDate, toDate, search, pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/achats-tiers-payants")
    public ResponseEntity<List<AchatTiersPayant>> getAchatsTierspayant(
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate,
        @RequestParam(name = "search", required = false) String search,
        Pageable pageable
    ) {
        Page<AchatTiersPayant> page = activitySummaryService.fetchAchatTiersPayant(fromDate, toDate, search, pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
