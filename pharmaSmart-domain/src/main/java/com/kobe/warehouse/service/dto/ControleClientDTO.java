package com.kobe.warehouse.service.dto;

import java.util.List;

/**
 * Contrôle anticipé de la création d'un client comptant, avant l'enregistrement.
 *
 * @param clientExistant un client porte déjà ces nom, prénom ET téléphone : la création est bloquée,
 *                       le caissier peut sélectionner ce client plutôt que d'en créer un second
 * @param proches        clients voisins (même téléphone, même nom, nom proche à une faute de frappe près) :
 *                       un avertissement, jamais un blocage
 */
public record ControleClientDTO(UninsuredCustomerDTO clientExistant, List<ClientProcheDTO> proches) {
    /** @param motif « même téléphone », « même nom » ou « nom proche » */
    public record ClientProcheDTO(UninsuredCustomerDTO client, String motif) {}
}
