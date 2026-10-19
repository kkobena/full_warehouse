package com.kobe.warehouse.domain.enumeration;

import java.util.List;

/**
 * Ce qu'une entrée du catalogue des exports produit : soit des lignes de données lues en flux ({@link ExportDonnees}), soit une
 * édition déjà mise en forme par le service qui la bâtit ({@link RapportExport}). L'historique, les modèles et l'écran ne
 * connaissent que ce contrat ; le nom de la constante est ce qui circule et ce qui est rangé en base.
 */
public sealed interface SujetExport permits ExportDonnees, RapportExport {
    /** Fourni par l'énumération ; c'est le code qui circule entre l'écran, l'API et la colonne {@code export}. */
    String name();

    String getLibelle();

    String getDescription();

    /** Intitulé du groupe sous lequel l'écran range l'entrée. */
    String getLibelleRubrique();

    /** Lu sur une période (du / au) ; sinon, l'état du jour ou l'exercice civil. */
    boolean isPeriodique();

    List<FiltreExport> getFiltres();

    /** Formats que l'entrée sait produire, dans l'ordre où l'écran les propose. */
    List<FormatExport> getFormats();

    /** Codes {@code nav_item} qui ouvrent l'entrée : en détenir un seul suffit. */
    List<String> getDroits();

    /**
     * Enregistrable dans un modèle programmé. Faux pour une édition : son service la bâtit avec les droits de qui la demande, et
     * une exécution programmée n'a personne derrière elle.
     */
    boolean estProgrammable();
}
