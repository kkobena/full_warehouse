package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;

public record ObjectifMensuelDTO(
    String libelle,
    double valeurActuelle,
    double valeurCible,
    String unite,
    double tauxAtteinte
) implements Serializable {}
