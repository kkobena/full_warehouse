package com.kobe.warehouse.service.dto.controle;

import java.util.List;

/**
 * @param referentielPublie faux tant qu'aucune version du référentiel d'interactions n'est publiée :
 *                          l'écran doit alors dire que les interactions ne sont pas contrôlées
 * @param limite            périmètre du contrôle, à afficher tel quel
 */
public record ControleResultatDTO(
    List<AlerteControleDTO> alertes,
    List<ProduitNonControleDTO> nonControles,
    boolean referentielPublie,
    String limite
) {
    public static final String LIMITE = "Contrôle établi sur les achats identifiés de ce client.";
}
