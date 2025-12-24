package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ClassificationConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO représentant la configuration de classification dynamique.
 *
 * @param id ID de la configuration
 * @param poidsCa Poids du CA dans le calcul (0.0 à 1.0)
 * @param poidsRotation Poids de la rotation dans le calcul (0.0 à 1.0)
 * @param poidsFrequence Poids de la fréquence dans le calcul (0.0 à 1.0)
 * @param seuilAPlus Seuil de score pour classe A+ (0-100)
 * @param seuilA Seuil de score pour classe A (0-100)
 * @param seuilB Seuil de score pour classe B (0-100)
 * @param seuilC Seuil de score pour classe C (0-100)
 * @param rotationAPlus Seuil de rotation annuelle pour A+
 * @param rotationA Seuil de rotation annuelle pour A
 * @param rotationB Seuil de rotation annuelle pour B
 * @param rotationC Seuil de rotation annuelle pour C
 * @param nbMoisAnalyse Nombre de mois d'historique à analyser
 * @param nbMoisMinNouveauProduit Mois minimum pour considérer un produit comme nouveau
 * @param changementMinScore Écart minimum de score pour changer de classe
 * @param autoClassificationEnabled Si la classification automatique est activée
 * @param updatedAt Date de dernière mise à jour
 * @param updatedByName Nom de l'utilisateur ayant mis à jour
 */
public record ClassificationConfigDTO(
    Integer id,
    BigDecimal poidsCa,
    BigDecimal poidsRotation,
    BigDecimal poidsFrequence,
    Integer seuilAPlus,
    Integer seuilA,
    Integer seuilB,
    Integer seuilC,
    BigDecimal rotationAPlus,
    BigDecimal rotationA,
    BigDecimal rotationB,
    BigDecimal rotationC,
    Integer nbMoisAnalyse,
    Integer nbMoisMinNouveauProduit,
    Integer changementMinScore,
    Boolean autoClassificationEnabled,
    LocalDateTime updatedAt,
    String updatedByName
) {

    /**
     * Crée un DTO depuis une entité ClassificationConfig
     *
     * @param entity L'entité à convertir
     * @return Le DTO correspondant
     */
    public static ClassificationConfigDTO fromEntity(ClassificationConfig entity) {
        return new ClassificationConfigDTO(
            entity.getId(),
            entity.getPoidsCa(),
            entity.getPoidsRotation(),
            entity.getPoidsFrequence(),
            entity.getSeuilAPlus(),
            entity.getSeuilA(),
            entity.getSeuilB(),
            entity.getSeuilC(),
            entity.getRotationAPlus(),
            entity.getRotationA(),
            entity.getRotationB(),
            entity.getRotationC(),
            entity.getNbMoisAnalyse(),
            entity.getNbMoisMinNouveauProduit(),
            entity.getChangementMinScore(),
            entity.getAutoClassificationEnabled(),
            entity.getUpdatedAt(),
            entity.getUpdatedBy() != null ?
                (entity.getUpdatedBy().getFirstName() + " " + entity.getUpdatedBy().getLastName()).trim() : null
        );
    }

    /**
     * Vérifie si les poids sont valides (somme = 1.0)
     *
     * @return true si la somme des poids est égale à 1.0
     */
    public boolean isPoidsValide() {
        if (poidsCa == null || poidsRotation == null || poidsFrequence == null) {
            return false;
        }
        BigDecimal somme = poidsCa.add(poidsRotation).add(poidsFrequence);
        return somme.compareTo(BigDecimal.ONE) == 0;
    }

    /**
     * Vérifie si les seuils sont cohérents (décroissants)
     *
     * @return true si les seuils sont dans le bon ordre
     */
    public boolean isSeuilsValides() {
        return seuilAPlus != null && seuilA != null && seuilB != null && seuilC != null
            && seuilAPlus > seuilA && seuilA > seuilB && seuilB > seuilC;
    }

    /**
     * Vérifie si la configuration est valide
     *
     * @return true si tous les paramètres sont valides
     */
    public boolean isValide() {
        return isPoidsValide() && isSeuilsValides();
    }
}
