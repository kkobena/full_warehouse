package com.kobe.warehouse.service.cahier_recette;

/**
 * Miroir Java de l'interface TS CaptureEcran (cahier-recette.model.ts).
 *
 * <p>{@code fichier} est un chemin relatif servi par l'application, par exemple
 * {@code content/captures/VTE-01/etape-1.jpg}. Il est résolu depuis le classpath au rendu du
 * PDF (voir {@code CahierRecettePdfService}), et directement par le navigateur côté Angular.
 */
public record CaptureEcranDTO(
    Integer ordre,
    String fichier,
    String legende
) {}
