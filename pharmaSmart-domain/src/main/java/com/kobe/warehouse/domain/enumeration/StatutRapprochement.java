package com.kobe.warehouse.domain.enumeration;

/** Qualité d'une proposition de rapprochement produit ↔ spécialité du référentiel. */
public enum StatutRapprochement {
    /** Même nom, même dosage, un seul candidat de ce score, sans conflit de DCI. */
    SUR,
    /** Nom (exact ou approché) retrouvé ; dosage, forme ou ambiguïté à relire. */
    A_VERIFIER,
    /** Rattaché par la molécule (déclarée sur le produit ou lue dans son libellé) et le dosage. */
    PAR_DCI,
    /** Aucun candidat suffisant. */
    NON_TROUVE,
}
