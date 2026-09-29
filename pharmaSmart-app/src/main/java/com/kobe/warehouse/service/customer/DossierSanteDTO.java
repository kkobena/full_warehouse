package com.kobe.warehouse.service.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Dossier de sécurité d'un client (fiche client, lot 2). */
public record DossierSanteDTO(
    List<Allergie> allergies,
    List<String> pathologies,
    boolean grossesse,
    LocalDate dateTerme,
    boolean allaitement,
    BigDecimal poidsKg,
    LocalDate datePesee,
    String note,
    LocalDateTime updatedAt,
    String updatedBy
) {
    /** Allergie à une molécule ({@code dciId}) ou, à défaut, à un libellé libre. */
    public record Allergie(Integer id, Integer dciId, String dciLibelle, String libelle, String reaction) {}

    public static DossierSanteDTO vide() {
        return new DossierSanteDTO(List.of(), List.of(), false, null, false, null, null, null, null, null);
    }
}
