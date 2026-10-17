package com.kobe.warehouse.service.dto.etiquette;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * Étiquette de préparation magistrale, saisie au moment de l'impression.
 *
 * <p>Rien n'est conservé : l'étiquette est une sortie, pas un enregistrement. Le registre des
 * préparations est un autre chantier, qui demande que le contenu réglementaire soit arrêté avant
 * d'être figé en base.
 *
 * @param libelle nom de la préparation, celui que porte le pot
 * @param composition ce qu'il y a dedans — la seule donnée que rien d'autre ne porte
 * @param datePreparation date de réalisation, portée sur l'étiquette
 * @param peremption date limite d'utilisation
 * @param mention « à usage externe », « tenir hors de portée des enfants »…
 */
public record EtiquettePreparationDTO(
    @NotBlank String libelle,
    @NotBlank String composition,
    String patient,
    String prescripteur,
    String posologie,
    LocalDate datePreparation,
    LocalDate peremption,
    String mention
) {}
