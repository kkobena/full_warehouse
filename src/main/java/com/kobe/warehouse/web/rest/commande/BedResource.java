package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.BedDTO;
import com.kobe.warehouse.service.dto.BedLigneDTO;
import com.kobe.warehouse.service.dto.BedSummaryDTO;
import com.kobe.warehouse.service.stock.BedService;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class BedResource {

    private final BedService bedService;

    public BedResource(BedService bedService) {
        this.bedService = bedService;
    }

    @PostMapping("/beds")
    public ResponseEntity<BedDTO> create(@RequestBody BedDTO dto) throws URISyntaxException {
        BedDTO result = bedService.createBed(dto);
        return ResponseEntity.created(new URI("/api/beds/" + result.getId()))
            .body(result);
    }

    @GetMapping("/beds")
    public ResponseEntity<List<BedSummaryDTO>> findAll(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) MotifBed motifBed,
        @RequestParam(required = false) OrderStatut orderStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        Pageable pageable
    ) {
        Page<BedSummaryDTO> page = bedService.findAll(search, motifBed, orderStatus, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/beds/{id}")
    public ResponseEntity<BedDTO> findOne(
        @PathVariable Integer id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate
    ) {
        return ResponseEntity.ok(bedService.findById(id, orderDate));
    }

    @PostMapping("/beds/{id}/lignes")
    public ResponseEntity<BedDTO> addLigne(
        @PathVariable Integer id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate,
        @RequestBody BedLigneDTO ligne
    ) {
        return ResponseEntity.ok(bedService.addLigne(id, orderDate, ligne));
    }

    @PatchMapping("/beds/{id}/lignes/{ligneId}")
    public ResponseEntity<BedDTO> updateLigne(
        @PathVariable Integer id,
        @PathVariable Integer ligneId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ligneDate,
        @RequestBody BedLigneDTO ligne
    ) {
        return ResponseEntity.ok(bedService.updateLigne(id, orderDate, ligneId, ligneDate, ligne));
    }

    @DeleteMapping("/beds/{id}/lignes/{ligneId}")
    public ResponseEntity<Void> removeLigne(
        @PathVariable Integer id,
        @PathVariable Integer ligneId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ligneDate
    ) {
        bedService.removeLigne(id, orderDate, ligneId, ligneDate);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/beds/{id}/validate")
    public ResponseEntity<BedDTO> validate(
        @PathVariable Integer id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate,
        @RequestParam(required = false) MotifBed motif,
        @RequestParam(required = false) Integer fournisseurId,
        @RequestParam(required = false) String commentaire
    ) {
        return ResponseEntity.ok(bedService.validateBed(id, orderDate, motif, fournisseurId, commentaire));
    }

    @DeleteMapping("/beds/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Integer id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate
    ) {
        bedService.deleteBed(id, orderDate);
        return ResponseEntity.noContent().build();
    }
}
