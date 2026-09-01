package com.kobe.warehouse.service.cahier_recette;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/** Miroir Java de l'interface TS ScenarioRecette (cahier-recette.model.ts). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScenarioRecetteDTO(
    String id,
    String titre,
    String besoin,
    String fonctionnement,
    String prerequis,
    List<String> etapes,
    String resultatAttendu,
    Boolean hidden,
    /** Captures d'écran injectées à la génération ; null tant qu'aucune campagne n'a tourné. */
    List<CaptureEcranDTO> captures
) {
    /**
     * Associe chaque étape à la capture qui l'illustre, dans l'ordre de la marche à suivre.
     * <p>
     * Le manuel montre ainsi l'écran au moment précis où le geste est décrit : une étape sans
     * capture reste affichée, une capture sans étape correspondante (numéro hors liste) est
     * ajoutée en fin de parcours plutôt que perdue.
     */
    @JsonIgnore
    public List<EtapeIllustreeDTO> etapesIllustrees() {
        List<EtapeIllustreeDTO> illustrees = new ArrayList<>();
        List<String> textes = etapes == null ? List.of() : etapes;
        for (int index = 0; index < textes.size(); index++) {
            illustrees.add(new EtapeIllustreeDTO(index + 1, textes.get(index), captureDeLEtape(index + 1)));
        }
        if (captures != null) {
            captures.stream()
                .filter(capture -> capture.ordre() != null && capture.ordre() > textes.size())
                .forEach(capture -> illustrees.add(
                    new EtapeIllustreeDTO(capture.ordre(), capture.legende(), capture)));
        }
        return illustrees;
    }

    private CaptureEcranDTO captureDeLEtape(int numero) {
        if (captures == null) {
            return null;
        }
        return captures.stream()
            .filter(capture -> capture.ordre() != null && capture.ordre() == numero)
            .findFirst()
            .orElse(null);
    }
}
