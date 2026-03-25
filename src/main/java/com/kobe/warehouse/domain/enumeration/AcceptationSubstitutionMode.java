package com.kobe.warehouse.domain.enumeration;

/**
 * Mode d'acceptation des substitutions PharmaML (EP — Équivalent Proposé).
 *
 * <ul>
 *   <li>{@link #AUTO} : les EP sont acceptés automatiquement, comme les EL/RL,
 *       et la paire est enregistrée dans la table {@code substitut} pour mémorisation.</li>
 *   <li>{@link #MANUEL} : les EP restent en attente de validation par le pharmacien ;
 *       un indicateur visuel signale si la paire est déjà connue dans la table {@code substitut}.</li>
 * </ul>
 */
public enum AcceptationSubstitutionMode {
    AUTO,
    MANUEL
}
