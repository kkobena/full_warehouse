package com.kobe.warehouse.domain.enumeration;

/** Vie d'un fichier d'export : généré en tâche de fond, téléchargeable jusqu'à expiration. */
public enum StatutExport {
    EN_ATTENTE,
    EN_COURS,
    TERMINE,
    ECHEC,
    EXPIRE,
}
