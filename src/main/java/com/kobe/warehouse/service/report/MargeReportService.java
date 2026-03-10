package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.MargeDTO;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service de rapport de marges produit, sans classification BCG.
 * <p>
 * Contrairement à {@link ProfitabilityReportService} qui repose sur
 * {@code EntityManager} et renvoie des listes non paginées, ce service :
 * <ul>
 *   <li>expose une pagination native via {@link Page}</li>
 *   <li>accepte des filtres optionnels famille / rayon</li>
 *   <li>utilise un seuil de marge configurable</li>
 * </ul>
 */
public interface MargeReportService {

    /**
     * Retourne tous les produits avec leurs indicateurs de marge, paginés.
     *
     * @param familleProduitId filtre optionnel sur la famille produit (null = tous)
     * @param search           filtre optionnel sur le libellé ou le code CIP (null = tous)
     * @param pageable         paramètres de pagination et de tri
     * @return page de {@link MargeDTO}
     */
    Page<MargeDTO> getMarges(Integer familleProduitId, String search, Pageable pageable);

    /**
     * Retourne les produits dont le taux de marge est inférieur au seuil donné.
     *
     * @param seuilPct seuil en pourcentage (ex. 10 pour < 10 %)
     * @param pageable pagination
     * @return page de {@link MargeDTO}
     */
    Page<MargeDTO> getProduitsMargeInsuffisante(int seuilPct, Pageable pageable);

    /**
     * Retourne les N produits les plus rentables (marge brute décroissante).
     *
     * @param limit    nombre de produits
     * @param pageable pagination
     * @return page de {@link MargeDTO}
     */
    Page<MargeDTO> getTopProduitsParMarge(int limit, Pageable pageable);

    /**
     * Retourne le résumé global des marges, avec filtres optionnels.
     *
     * @param familleProduitId filtre optionnel famille (null = tous)
     * @param seuilBasPct      seuil bas en % pour comptage « marge insuffisante » (défaut 10)
     * @param seuilHautPct     seuil haut en % pour comptage « bonne marge » (défaut 20)
     * @return {@link MargeSummaryDTO}
     */
    MargeSummaryDTO getMargeSummary(Integer familleProduitId, int seuilBasPct, int seuilHautPct);

    byte[] export(Integer familleProduitId, String search);
}

