package com.kobe.warehouse.service.promotion;

import com.kobe.warehouse.service.dto.promotion.ChoixPerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.PackComptoirDTO;
import com.kobe.warehouse.service.dto.promotion.PerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionArticleDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionFormDTO;
import java.util.List;

/**
 * Gestion des promotions du magasin d'exploitation de l'utilisateur. Les invariants du plan (§5.3)
 * sont contrôlés à la création, à la modification et à la validation — jamais au comptoir, où un
 * refus serait découvert par la cliente.
 */
public interface PromotionService {
    /** Les promotions du magasin, les plus récentes d'abord, sans le détail des articles. */
    List<PromotionDTO> lister();

    PromotionDTO lire(Long id);

    /**
     * Les articles en promotion aujourd'hui dans le magasin : une entrée par article, celle de la
     * promotion la plus avantageuse, comme le moteur. Une promotion épuisée n'y figure pas.
     */
    List<PromotionArticleDTO> articlesEnVigueur();

    /**
     * Le pack en vigueur dans le magasin dont l'emballage porte ce code-barres : ses articles, pour que
     * le comptoir les ajoute au panier.
     *
     * @throws com.kobe.warehouse.service.errors.BadRequestAlertException si aucun pack en vigueur ne porte
     *     ce code, ou s'il est épuisé
     */
    PackComptoirDTO pack(String codeBarre);

    /** Les rayons du magasin et les familles, de quoi ajouter d'un geste tous leurs articles à une promotion. */
    ChoixPerimetreDTO choixPerimetre();

    /**
     * Les articles actifs d'un rayon du magasin ou d'une famille — l'un ou l'autre — triés entre
     * promouvables et à prix administré. Rien n'est enregistré : le formulaire les ajoute à sa liste.
     *
     * @throws com.kobe.warehouse.service.errors.BadRequestAlertException sans rayon ni famille, ou avec les deux
     */
    PerimetreDTO perimetre(Integer rayonId, Integer familleId);

    /** Crée une promotion en {@code BROUILLON} : elle ne s'applique qu'une fois validée. */
    PromotionDTO creer(PromotionFormDTO form);

    /** Modifiable en {@code BROUILLON} ou {@code SUSPENDUE} seulement : une promotion validée se suspend d'abord. */
    PromotionDTO modifier(Long id, PromotionFormDTO form);

    /** {@code BROUILLON} ou {@code SUSPENDUE} → {@code VALIDEE}, après un nouveau contrôle des prix. */
    PromotionDTO valider(Long id);

    /** {@code VALIDEE} → {@code SUSPENDUE}. */
    PromotionDTO suspendre(Long id);

    /** Définitif : une promotion annulée ne redevient jamais validée. */
    PromotionDTO annuler(Long id);
}
