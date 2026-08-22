package com.kobe.warehouse.service.declaration_ca.dto;

/** Comment l'objectif de ponction est exprimé. */
public enum ModeCalculPonction {
    /** Un montant en francs, tel quel. */
    MONTANT_FIXE,
    /**
     * Un pourcentage du chiffre d'affaires <strong>après exclusions</strong> — celui que le
     * pharmacien lit sur ses états, et non l'assiette exonérée, qui n'est qu'une contrainte de
     * faisabilité.
     */
    POURCENTAGE,
}
