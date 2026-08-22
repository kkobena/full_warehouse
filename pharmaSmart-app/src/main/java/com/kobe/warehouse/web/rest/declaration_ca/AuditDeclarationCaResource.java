package com.kobe.warehouse.web.rest.declaration_ca;

import com.kobe.warehouse.service.declaration_ca.AuditDeclarationCaService;
import com.kobe.warehouse.service.declaration_ca.dto.AnomalieDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôle des invariants du chiffre d'affaires à déclarer.
 *
 * <p>L'accès exige le privilège {@code pr-declaration-ca-audit} plutôt que {@code ROLE_ADMIN} : dans
 * une officine, qui vérifie la cohérence avant de déclarer n'est pas nécessairement administrateur,
 * et lui ouvrir toute l'administration pour cela serait disproportionné. Le privilège est un item de
 * navigation de type {@code ACTION}, attribuable à n'importe quel rôle depuis « Rôles &amp;
 * Autorisations », et accordé d'office à {@code ROLE_ADMIN} par la migration.
 *
 * <p>Volontairement <strong>sans</strong> {@code @RequiresFeature} : l'audit doit rester accessible
 * même après l'expiration d'un module, précisément parce que c'est le moment où l'on veut vérifier
 * que les données restées en base sont cohérentes.
 */
@RestController
@RequestMapping("/api/declaration-ca/audit")
@PreAuthorize("hasAuthority('pr-declaration-ca-audit')")
public class AuditDeclarationCaResource {

    private final AuditDeclarationCaService auditService;

    public AuditDeclarationCaResource(AuditDeclarationCaService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AnomalieDTO>> controler(
        @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(auditService.controler(fromDate, toDate));
    }
}
