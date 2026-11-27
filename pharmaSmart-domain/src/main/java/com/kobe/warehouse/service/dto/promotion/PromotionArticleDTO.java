package com.kobe.warehouse.service.dto.promotion;

import java.time.LocalDate;

/**
 * Un article en promotion aujourd'hui dans le magasin, avec la promotion que le comptoir lui
 * appliquera : de quoi poser un badge au catalogue et sur la grille des favoris.
 */
public record PromotionArticleDTO(
        Integer produitId,
        Long promotionId,
        String libelle,
        int prixReference,
        int prixApresPromo,
        LocalDate dateFin,
        /** La quantité qui la déclenche : N+M pour une offre. */
        int quantiteMin,
        /** « 2 achetés + 1 offert » pour une offre N+M ; nul sinon. */
        String offre) {}
