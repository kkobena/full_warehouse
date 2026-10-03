package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.service.sale.dto.ProduitSignauxDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mêmes règles que la recherche produit (V2.1.22), pour les produits déjà dans le panier. */
@Service
public class ProduitSignauxService {

    private static final String SQL =
        """
        SELECT p.id,
               p.statut_legal,
               (SELECT s.type_generique
                FROM produit_ref_specialite r
                       JOIN ref_specialite s ON s.cis = r.cis
                WHERE r.produit_id = p.id
                  AND r.decision IN ('AUTO', 'VALIDE')
                  AND s.type_generique IS NOT NULL),
               (SELECT l.num_lot
                FROM lot l
                WHERE l.produit_id = p.id
                  AND l.current_quantity > 0
                  AND l.statut = 'AVAILABLE'
                  AND l.expiry_date <= CURRENT_DATE + COALESCE(
                           (SELECT NULLIF(c.value, '')::int FROM app_configuration c
                            WHERE c.name = 'APP_EXPIRY_ALERT_DAYS_BEFORE'), 90)
                ORDER BY l.expiry_date, l.id
                LIMIT 1),
               (SELECT to_char(l.expiry_date, 'YYYY-MM-DD')
                FROM lot l
                WHERE l.produit_id = p.id
                  AND l.current_quantity > 0
                  AND l.statut = 'AVAILABLE'
                  AND l.expiry_date <= CURRENT_DATE + COALESCE(
                           (SELECT NULLIF(c.value, '')::int FROM app_configuration c
                            WHERE c.name = 'APP_EXPIRY_ALERT_DAYS_BEFORE'), 90)
                ORDER BY l.expiry_date, l.id
                LIMIT 1),
               to_char(CURRENT_DATE + COALESCE(
                 (SELECT NULLIF(c.value, '')::int FROM app_configuration c
                  WHERE c.name = 'APP_EXPIRY_ALERT_DAYS_BEFORE'), 90), 'YYYY-MM-DD'),
               (SELECT COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) FROM stock_produit sp WHERE sp.produit_id = p.id),
               COALESCE(p.qty_seuil_mini, 0)
        FROM produit p
        WHERE p.id IN (:ids)
        """;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<ProduitSignauxDTO> chargerSignaux(Collection<Integer> produitIds) {
        if (produitIds == null || produitIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> lignes = em.createNativeQuery(SQL).setParameter("ids", produitIds).getResultList();
        return lignes
            .stream()
            .map(l -> new ProduitSignauxDTO((Integer) l[0], (String) l[1], (String) l[2], (String) l[3], (String) l[4], (String) l[5], ((Number) l[6]).intValue(), ((Number) l[7]).intValue()))
            .toList();
    }
}
