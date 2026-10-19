package com.kobe.warehouse.repository.exports;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Lecture en flux des exports de données ({@code ExportDonnees}) : une ligne = un tableau de valeurs, dans l'ordre des colonnes
 * déclarées par l'export. À consommer dans une transaction en lecture seule, flux fermé après usage.
 */
public interface ExportDonneesRepository {
    /**
     * @param du début de la période, ignoré par un export sans période
     * @param au fin de la période, ignorée par un export sans période
     * @param filtres valeurs retenues par filtre, en texte ; les filtres que l'export n'annonce pas sont ignorés
     */
    Stream<Object[]> lireLignes(ExportDonnees export, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres);
}
