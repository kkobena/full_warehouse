package com.kobe.warehouse.service.declaration_ca.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Demande d'exclusion ou de réintégration en masse.
 *
 * <p>L'ordre porte l'état cible et non une bascule : rejouer la même requête donne le même résultat.
 * Un toggle produirait l'inverse au second envoi — double-clic, reprise réseau, ou simple retour en
 * arrière du navigateur suffiraient à inverser silencieusement des dizaines de rayons.
 *
 * @param ids entités visées
 * @param exclure état cible commun à toutes
 */
public record ExclusionRequestDTO(@NotEmpty Set<Integer> ids, @NotNull Boolean exclure) {}
