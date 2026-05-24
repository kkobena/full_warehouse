package com.kobe.warehouse.service.dto.dashboard;

import java.time.LocalDateTime;

/**
 * DTO pour l'état de la caisse du préparateur.
 * N'expose PAS l'écart (réservé au manager).
 */
public record CaisseStatusDTO(
    Long fondOuverture,           // init_amount — fond de départ
    Long encaissementsEspeces,    // somme cash_register_item CASH du jour
    Long especesTheoriques,       // fondOuverture + encaissementsEspeces
    String heureOuverture,        // begin_time formaté "HH:mm"
    String etat,                  // "OUVERTE" ou "FERMEE"
    LocalDateTime derniereFermeture // end_time de la dernière session fermée
) {}
