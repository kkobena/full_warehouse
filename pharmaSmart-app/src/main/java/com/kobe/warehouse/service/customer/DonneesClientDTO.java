package com.kobe.warehouse.service.customer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Tout ce que l'officine détient sur un client : réponse à une demande d'accès à ses données. */
public record DonneesClientDTO(
    LocalDateTime extraitLe,
    Identite identite,
    List<Identite> ayantsDroit,
    List<TiersPayant> tiersPayants,
    DossierSanteDTO dossierSante,
    List<DepenseDTO> achats,
    List<LigneReleveDTO> compte,
    List<RelanceDiffereDTO> relances
) {
    public record Identite(
        Integer id,
        String code,
        String prenom,
        String nom,
        String telephone,
        String email,
        LocalDate dateNaissance,
        String sexe,
        String numeroAyantDroit,
        String statut,
        LocalDateTime creeLe
    ) {}

    public record TiersPayant(String organisme, String numero, int taux, String priorite, LocalDate finValidite) {}
}
