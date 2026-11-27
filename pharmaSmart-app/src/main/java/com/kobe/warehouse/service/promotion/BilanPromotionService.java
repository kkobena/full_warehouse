package com.kobe.warehouse.service.promotion;

import com.kobe.warehouse.service.dto.promotion.BilanPromotionDTO;

/** Le bilan d'une promotion du magasin de l'utilisateur (plan, phase 3). */
public interface BilanPromotionService {
    BilanPromotionDTO bilan(Long promotionId);

    /** Le même bilan en CSV (séparateur « ; », UTF-8 avec BOM pour Excel) : une ligne par article. */
    byte[] exporter(Long promotionId);
}
