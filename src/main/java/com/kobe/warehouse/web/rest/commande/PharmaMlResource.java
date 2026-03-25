package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.pharmaml.dto.DispoGrossisteResultDTO;
import com.kobe.warehouse.service.pharmaml.dto.DispoMultiRequestDTO;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.InfoProduitDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmaMlEnvoiDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmamlCommandeResponse;
import com.kobe.warehouse.service.pharmaml.dto.SubstitutionProposeeDTO;
import com.kobe.warehouse.service.pharmaml.service.PharmaMlService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pharmaml")
public class PharmaMlResource {

    private final PharmaMlService pharmaMlService;

    public PharmaMlResource(PharmaMlService pharmaMlService) {
        this.pharmaMlService = pharmaMlService;
    }

    @PostMapping("/envoi")
    public ResponseEntity<PharmamlCommandeResponse> envoiCommande(
        @RequestBody EnvoiParamsDTO params) {
        return ResponseEntity.ok(pharmaMlService.envoiPharmaCommande(params));
    }

    @PostMapping("/renvoi")
    public ResponseEntity<Void> renvoiCommande(@RequestBody EnvoiParamsDTO params) {
        pharmaMlService.renvoiPharmaCommande(params);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/retour/{commandeRef}/{orderId}")
    public ResponseEntity<VerificationResponseCommandeDTO> lignesRetour(
        @PathVariable String commandeRef,
        @PathVariable String orderId
    ) {
        return ResponseEntity.ok(pharmaMlService.lignesCommandeRetour(commandeRef, orderId));
    }

    @GetMapping("/rupture/{ruptureId}")
    public ResponseEntity<VerificationResponseCommandeDTO> reponseRupture(
        @PathVariable String ruptureId) {
        return ResponseEntity.ok(pharmaMlService.reponseRupture(ruptureId));
    }

    @GetMapping("/statut/{envoiId}")
    public ResponseEntity<PharmaMlEnvoiDTO> statutEnvoi(@PathVariable Integer envoiId) {
        return ResponseEntity.ok(pharmaMlService.getStatutEnvoi(envoiId));
    }

    @GetMapping("/substitutions/{commandeId}/{orderDate}")
    public ResponseEntity<List<SubstitutionProposeeDTO>> substitutionsEnAttente(
        @PathVariable Integer commandeId,
        @PathVariable String orderDate
    ) {
        return ResponseEntity.ok(
            pharmaMlService.getSubstitutionsEnAttente(commandeId, LocalDate.parse(orderDate)));
    }

    @PutMapping("/substitution/{id}/accepter")
    public ResponseEntity<Void> accepterSubstitution(@PathVariable Integer id) {
        pharmaMlService.accepterSubstitution(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/substitution/{id}/refuser")
    public ResponseEntity<Void> refuserSubstitution(@PathVariable Integer id) {
        pharmaMlService.refuserSubstitution(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/historique/{commandeId}/{orderDate}")
    public ResponseEntity<List<PharmaMlEnvoiDTO>> historiqueEnvois(
        @PathVariable Integer commandeId,
        @PathVariable String orderDate
    ) {
        return ResponseEntity.ok(
            pharmaMlService.getHistoriqueEnvois(commandeId, LocalDate.parse(orderDate)));
    }


    @GetMapping("/disponibilite/{commandeId}/{orderDate}")
    public ResponseEntity<List<InfoProduitDTO>> demanderDisponibilite(
        @PathVariable Integer commandeId,
        @PathVariable String orderDate,
        @RequestParam(required = false) Integer grossisteId
    ) {
        return ResponseEntity.ok(
            pharmaMlService.demanderDisponibilite(commandeId, LocalDate.parse(orderDate),
                grossisteId));
    }

    @PostMapping("/disponibilite-multi")
    public ResponseEntity<List<DispoGrossisteResultDTO>> demanderDisponibiliteMulti(
        @RequestBody DispoMultiRequestDTO request
    ) {
        return ResponseEntity.ok(pharmaMlService.demanderDisponibiliteMulti(request));
    }


}
