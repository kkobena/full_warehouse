package com.kobe.warehouse.service.cahier_recette;

/**
 * Une étape de la marche à suivre, accompagnée de l'écran qui l'illustre lorsqu'il existe.
 * <p>
 * Construit à la volée par {@link ScenarioRecetteDTO#etapesIllustrees()} : rien n'est stocké
 * sous cette forme dans {@code data/cahier-recette.json}.
 */
public record EtapeIllustreeDTO(int numero, String texte, CaptureEcranDTO capture) {
    public boolean illustree() {
        return capture != null && capture.fichier() != null && !capture.fichier().isBlank();
    }
}

