package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ClassificationCriticiteLog;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ClassificationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO représentant un log de changement de classification.
 *
 * @param id ID du log
 * @param produitId ID du produit
 * @param libelleProduit Libellé du produit
 * @param ancienneClasse Classe avant changement
 * @param nouvelleClasse Classe après changement
 * @param vmm12Mois VMM au moment du changement
 * @param ca12Mois CA au moment du changement (en centimes)
 * @param rotationAnnuelle Rotation au moment du changement
 * @param frequenceVenteMois Fréquence au moment du changement
 * @param scoreTotal Score calculé
 * @param raisonChangement Raison du changement
 * @param classificationType Type de classification (AUTO, MANUAL, INITIAL)
 * @param userName Nom de l'utilisateur (si manuel)
 * @param createdAt Date du changement
 */
public record ClassificationLogDTO(
    Integer id,
    Integer produitId,
    String libelleProduit,
    ClasseCriticite ancienneClasse,
    ClasseCriticite nouvelleClasse,
    Integer vmm12Mois,
    Long ca12Mois,
    BigDecimal rotationAnnuelle,
    Integer frequenceVenteMois,
    BigDecimal scoreTotal,
    String raisonChangement,
    ClassificationType classificationType,
    String userName,
    LocalDateTime createdAt
) {

    /**
     * Crée un DTO depuis une entité ClassificationCriticiteLog
     *
     * @param entity L'entité à convertir
     * @return Le DTO correspondant
     */
    public static ClassificationLogDTO fromEntity(ClassificationCriticiteLog entity) {
        return new ClassificationLogDTO(
            entity.getId(),
            entity.getProduit() != null ? entity.getProduit().getId() : null,
            entity.getProduit() != null ? entity.getProduit().getLibelle() : null,
            entity.getAncienneClasse(),
            entity.getNouvelleClasse(),
            entity.getVmm12Mois(),
            entity.getCa12Mois(),
            entity.getRotationAnnuelle(),
            entity.getFrequenceVenteMois(),
            entity.getScoreTotal(),
            entity.getRaisonChangement(),
            entity.getClassificationType(),
            entity.getUser() != null ?
                (entity.getUser().getFirstName() + " " + entity.getUser().getLastName()).trim() : null,
            entity.getCreatedAt()
        );
    }

    /**
     * Vérifie si c'est une promotion
     *
     * @return true si la nouvelle classe est supérieure
     */
    public boolean isPromotion() {
        if (ancienneClasse == null || nouvelleClasse == null) {
            return false;
        }
        return getOrdreClasse(nouvelleClasse) > getOrdreClasse(ancienneClasse);
    }

    /**
     * Vérifie si c'est une rétrogradation
     *
     * @return true si la nouvelle classe est inférieure
     */
    public boolean isRetrogradation() {
        if (ancienneClasse == null || nouvelleClasse == null) {
            return false;
        }
        return getOrdreClasse(nouvelleClasse) < getOrdreClasse(ancienneClasse);
    }

    /**
     * Obtient l'ordre numérique d'une classe
     */
    private int getOrdreClasse(ClasseCriticite classe) {
        return switch (classe) {
            case A_PLUS -> 5;
            case A -> 4;
            case B -> 3;
            case C -> 2;
            case D -> 1;
        };
    }

    /**
     * Retourne une description du changement
     *
     * @return Description du changement de classe
     */
    public String getDescriptionChangement() {
        if (ancienneClasse == null) {
            return "Classification initiale: " + nouvelleClasse.getCode();
        }
        String direction = isPromotion() ? "Promotion" : (isRetrogradation() ? "Rétrogradation" : "Maintien");
        return String.format("%s: %s -> %s",
            direction,
            ancienneClasse.getCode(),
            nouvelleClasse.getCode()
        );
    }

    /**
     * Retourne le CA formaté en euros
     *
     * @return CA en euros avec 2 décimales
     */
    public String getCaFormatte() {
        if (ca12Mois == null) {
            return "N/A";
        }
        return String.format("%.2f €", ca12Mois / 100.0);
    }
}
