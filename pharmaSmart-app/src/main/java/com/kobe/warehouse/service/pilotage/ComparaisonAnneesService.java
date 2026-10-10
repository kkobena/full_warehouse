package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import java.time.LocalDate;

/** Onglet « Comparer les années » : l'année en cours et jusqu'à cinq années civiles précédentes, mois par mois. */
public interface ComparaisonAnneesService {
    /** @param aujourdhui fin de l'année en cours, qui s'arrête à cette date */
    ComparaisonAnneesDTO comparerAnnees(RequeteAnneesDTO requete, LocalDate aujourdhui);
}
