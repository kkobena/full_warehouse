package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.StatutLigneFournisseurAP;
import com.kobe.warehouse.service.ap.AccountsPayableService;
import com.kobe.warehouse.service.dto.CompteFournisseurAPDTO;
import com.kobe.warehouse.service.dto.FournisseurAPSummaryDTO;
import com.kobe.warehouse.service.dto.LigneFournisseurAPDTO;
import com.kobe.warehouse.service.dto.ReglementBLDTO;
import com.kobe.warehouse.service.dto.ReglementFournisseurAPCommand;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/supplier-performance")
public class AccountsPayableResource {

    private final AccountsPayableService accountsPayableService;

    public AccountsPayableResource(AccountsPayableService accountsPayableService) {
        this.accountsPayableService = accountsPayableService;
    }

    @GetMapping("/ap")
    public ResponseEntity<List<CompteFournisseurAPDTO>> getComptes() {
        return ResponseEntity.ok(accountsPayableService.getComptes());
    }

    @GetMapping("/ap/summary")
    public ResponseEntity<FournisseurAPSummaryDTO> getSummary() {
        return ResponseEntity.ok(accountsPayableService.getSummary());
    }

    @GetMapping("/{fournisseurId}/ap/lignes")
    public ResponseEntity<List<LigneFournisseurAPDTO>> getLignes(
        @PathVariable Integer fournisseurId,
        @RequestParam(required = false) StatutLigneFournisseurAP statut,
        @PageableDefault(size = 10, sort = "dateCommande") Pageable pageable
    ) {
        Page<LigneFournisseurAPDTO> page=accountsPayableService.getLignes(fournisseurId, statut, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());

    }

    @GetMapping("/{fournisseurId}/ap/commandes/{commandeId}/reglements")
    public ResponseEntity<List<ReglementBLDTO>> getReglementsBl(
        @PathVariable Integer fournisseurId,
        @PathVariable Integer commandeId
    ) {
        return ResponseEntity.ok(accountsPayableService.getReglementsBl(fournisseurId, commandeId));
    }

    @PostMapping("/{fournisseurId}/ap/reglement")
    public ResponseEntity<Void> enregistrerReglement(
        @PathVariable Integer fournisseurId,
        @Valid @RequestBody ReglementFournisseurAPCommand command
    ) {
        accountsPayableService.enregistrerReglement(fournisseurId, command);
        return ResponseEntity.ok().build();
    }
}
