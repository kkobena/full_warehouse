package com.kobe.warehouse.service.sale.dto;

import java.util.List;

/**
 * Résultat d'une demande de retour client.
 * - retour : le retour persisté (null si toutes les lignes sont rejetées)
 * - lignesRejetees : lignes non traitées (STUPEFIANTS/PSO — retour interdit)
 * - lignesNonRestockees : lignes traitées financièrement mais non remises en stock (thermosensibles)
 * - partiel : true si au moins une ligne a été rejetée
 */
public record RetourClientResultDTO(
    RetourClientDTO retour,
    List<RetourLigneRejeteeDTO> lignesRejetees,
    List<RetourLigneRejeteeDTO> lignesNonRestockees,
    boolean partiel,
    EchangeContextDTO echangeContext
) {
    public static RetourClientResultDTO total(RetourClientDTO retour) {
        return new RetourClientResultDTO(retour, List.of(), List.of(), false, null);
    }

    public static RetourClientResultDTO totalAvecEchange(RetourClientDTO retour, EchangeContextDTO echangeContext) {
        return new RetourClientResultDTO(retour, List.of(), List.of(), false, echangeContext);
    }

    public static RetourClientResultDTO partiel(
        RetourClientDTO retour,
        List<RetourLigneRejeteeDTO> lignesRejetees,
        List<RetourLigneRejeteeDTO> lignesNonRestockees
    ) {
        return new RetourClientResultDTO(retour, lignesRejetees, lignesNonRestockees, true, null);
    }

    public static RetourClientResultDTO partielAvecEchange(
        RetourClientDTO retour,
        List<RetourLigneRejeteeDTO> lignesRejetees,
        List<RetourLigneRejeteeDTO> lignesNonRestockees,
        EchangeContextDTO echangeContext
    ) {
        return new RetourClientResultDTO(retour, lignesRejetees, lignesNonRestockees, true, echangeContext);
    }
}
