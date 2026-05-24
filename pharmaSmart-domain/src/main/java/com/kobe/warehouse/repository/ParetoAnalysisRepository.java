package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Accès en lecture seule à la vue {@code v_abc_pareto_analysis}.
 *
 * <p>Séparé de tout repository JPA : aucune entité mappée sur cette vue.
 * Les deux méthodes retournent des {@code Object[]} directement consommables
 * par {@link com.kobe.warehouse.service.classification.ClassificationBatchProcessor.ParetoScore}.
 *
 * <h3>Colonnes — {@code loadAllParetoScores()} (6 colonnes)</h3>
 * <ol>
 *   <li>[0] {@code produit_id}     — Integer</li>
 *   <li>[1] {@code ca_cumule_pct}  — BigDecimal</li>
 *   <li>[2] {@code rang}           — Integer</li>
 *   <li>[3] {@code ca_total}       — Long (CA 12 mois, centimes)</li>
 *   <li>[4] {@code frequence_mois} — Integer</li>
 *   <li>[5] {@code qte_vendue}     — Long (quantité vendue 12 mois)</li>
 * </ol>
 *
 * <h3>Colonnes — {@code findByProduitId()} (7 colonnes, ajout stock)</h3>
 * <ol>
 *   <li>[0–5] identiques à ci-dessus</li>
 *   <li>[6] {@code stock_actuel}   — Long (stock total tous magasins)</li>
 * </ol>
 */
@Repository
@Transactional(readOnly = true)
public class ParetoAnalysisRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Charge tous les scores Pareto en une seule requête.
     * À appeler une fois avant la boucle de classification par batch.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> loadAllParetoScores() {
        return entityManager.createNativeQuery("""
            SELECT produit_id, ca_cumule_pct, rang, ca_total, frequence_mois, qte_vendue
            FROM v_abc_pareto_analysis
            ORDER BY rang
            """).getResultList();
    }

    /**
     * Charge le score Pareto d'un produit + son stock actuel (tous magasins).
     * Utilisé par {@code ClassificationCriticiteService.calculerScore()} (API publique).
     */
    @SuppressWarnings("unchecked")
    public Optional<Object[]> findByProduitId(Integer produitId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
            SELECT p.produit_id,
                   p.ca_cumule_pct,
                   p.rang,
                   p.ca_total,
                   p.frequence_mois,
                   p.qte_vendue,
                   COALESCE(sq.stock_total, 0) AS stock_actuel
            FROM v_abc_pareto_analysis p
            LEFT JOIN (
                SELECT produit_id, SUM(qty_stock + qty_ug) AS stock_total
                FROM stock_produit
                GROUP BY produit_id
            ) sq ON sq.produit_id = p.produit_id
            WHERE p.produit_id = :produitId
            """)
            .setParameter("produitId", produitId)
            .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
