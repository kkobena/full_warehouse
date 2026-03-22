package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ClassificationConfig;

import java.time.LocalDateTime;

/**
 * DTO représentant la configuration de classification ABC Pareto.
 */
public record ClassificationConfigDTO(
    Integer id,
    Integer seuilParetoAPlus,
    Integer seuilParetoA,
    Integer seuilParetoB,
    Integer seuilParetoC,
    Integer seuilFrequenceMinMois,
    Integer cmmSeuilAPlus,
    Integer cmmSeuilA,
    Integer cmmSeuilB,
    Integer cmmSeuilC,
    Integer changementMinPourcentage,
    Boolean activerClassificationOrdo,
    Boolean activerCorrectionSaisonniere,
    Integer indiceSaisonnaliteMin,
    Integer nbMoisSaisonAnalyse,
    Integer nbMoisMinNouveauProduit,
    Boolean autoClassificationEnabled,
    LocalDateTime updatedAt,
    String updatedByName
) {

    public static ClassificationConfigDTO fromEntity(ClassificationConfig entity) {
        return new ClassificationConfigDTO(
            entity.getId(),
            entity.getSeuilParetoAPlus(),
            entity.getSeuilParetoA(),
            entity.getSeuilParetoB(),
            entity.getSeuilParetoC(),
            entity.getSeuilFrequenceMinMois(),
            entity.getCmmSeuilAPlus(),
            entity.getCmmSeuilA(),
            entity.getCmmSeuilB(),
            entity.getCmmSeuilC(),
            entity.getChangementMinPourcentage(),
            entity.getActiverClassificationOrdo(),
            entity.getActiverCorrectionSaisonniere(),
            entity.getIndiceSaisonnaliteMin(),
            entity.getNbMoisSaisonAnalyse(),
            entity.getNbMoisMinNouveauProduit(),
            entity.getAutoClassificationEnabled(),
            entity.getUpdatedAt(),
            entity.getUpdatedBy() != null ?
                (entity.getUpdatedBy().getFirstName() + " " + entity.getUpdatedBy().getLastName()).trim() : null
        );
    }

    public boolean isSeuilsValides() {
        return seuilParetoAPlus != null && seuilParetoA != null
            && seuilParetoB != null && seuilParetoC != null
            && seuilParetoAPlus < seuilParetoA
            && seuilParetoA < seuilParetoB
            && seuilParetoB < seuilParetoC
            && seuilParetoC <= 100;
    }
}
