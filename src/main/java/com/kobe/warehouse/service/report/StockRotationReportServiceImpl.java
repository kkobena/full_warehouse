package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du rapport de rotation de stock depuis {@code v_stock_rotation}.
 *
 * <p>La vue remplace {@code mv_stock_rotation} (supprimée en V1.3.4).
 * {@code categorie_abc} (Z-score) est supprimée — la classification utilise désormais
 * {@code produit.classe_criticite} via un LEFT JOIN.
 *
 * <p>Colonnes retournées (indices 0-15) :
 * [0] produit_id  [1] libelle  [2] code_cip  [3] famille
 * [4] stock_quantite  [5] cout_unitaire  [6] valeur_stock
 * [7] ca_30_jours  [8] qte_vendue_30_jours  [9] nb_ventes_30_jours
 * [10] ca_12_mois  [11] qte_vendue_12_mois  [12] cmm
 * [13] rotation_annuelle_qte  [14] couverture_stock_jours
 * [15] classe_criticite  (depuis produit — converti en CategorieABC)
 */
@Service
@Transactional(readOnly = true)
public class StockRotationReportServiceImpl implements StockRotationReportService {

    private static final String SELECT_COLS =
        "vsr.produit_id, vsr.libelle, vsr.code_cip, vsr.famille, " +
        "vsr.stock_quantite, vsr.cout_unitaire, vsr.valeur_stock, " +
        "vsr.ca_30_jours, vsr.qte_vendue_30_jours, vsr.nb_ventes_30_jours, " +
        "vsr.ca_12_mois, vsr.qte_vendue_12_mois, vsr.cmm, " +
        "vsr.rotation_annuelle_qte, vsr.couverture_stock_jours, " +
        "p.classe_criticite ";

    private static final String FROM_CLAUSE =
        "FROM v_stock_rotation vsr LEFT JOIN produit p ON p.id = vsr.produit_id ";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "stockRotation", key = "'all'")
    public List<StockRotationDTO> getAllStockRotation() {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + FROM_CLAUSE + "ORDER BY vsr.rotation_annuelle_qte DESC");
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'family:' + #categorie")
    public List<StockRotationDTO> getStockRotationByCategory(String categorie) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + FROM_CLAUSE +
            "WHERE vsr.famille = :famille ORDER BY vsr.rotation_annuelle_qte DESC");
        query.setParameter("famille", categorie);
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'abc:' + #categorieABC")
    public List<StockRotationDTO> getStockRotationByABCClassification(CategorieABC categorieABC) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + FROM_CLAUSE +
            "WHERE p.classe_criticite IN (:classes) ORDER BY vsr.rotation_annuelle_qte DESC");
        query.setParameter("classes", toClassesCriticite(categorieABC));
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    public Map<CategorieABC, Long> getStockRotationCountByABCClassification() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
            "SELECT p.classe_criticite, COUNT(*) " + FROM_CLAUSE +
            "GROUP BY p.classe_criticite").getResultList();

        Map<CategorieABC, Long> counts = new EnumMap<>(CategorieABC.class);
        counts.put(CategorieABC.A, 0L);
        counts.put(CategorieABC.B, 0L);
        counts.put(CategorieABC.C, 0L);

        for (Object[] row : rows) {
            String cc = (String) row[0];
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            try {
                ClasseCriticite classe = ClasseCriticite.fromString(cc);
                CategorieABC abc = CategorieABC.fromClasseCriticite(classe);
                counts.merge(abc, count, Long::sum);
            } catch (Exception ignored) {
                // classe_criticite null ou inconnue
            }
        }
        return counts;
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'slow'")
    public List<StockRotationDTO> getSlowMovingProducts() {
        // Produits de faible activité : classe C ou D (= ancien categorie_abc='C')
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + FROM_CLAUSE +
            "WHERE p.classe_criticite IN ('C', 'D') ORDER BY vsr.valeur_stock DESC");
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    public List<StockRotationDTO> getStockRotationPaginated(int page, int size) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + FROM_CLAUSE +
            "ORDER BY vsr.rotation_annuelle_qte DESC LIMIT :size OFFSET :offset");
        query.setParameter("size", size);
        query.setParameter("offset", page * size);
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    @Cacheable(value = "stockRotation", key = "'count'")
    public long getStockRotationCount() {
        return ((Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM v_stock_rotation").getSingleResult()).longValue();
    }

    @Override
    public List<StockRotationDTO> getStockRotationByABCPaginated(CategorieABC categorieABC, int page, int size) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + FROM_CLAUSE +
            "WHERE p.classe_criticite IN (:classes) ORDER BY vsr.rotation_annuelle_qte DESC LIMIT :size OFFSET :offset");
        query.setParameter("classes", toClassesCriticite(categorieABC));
        query.setParameter("size", size);
        query.setParameter("offset", page * size);
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    public long getStockRotationCountByABC(CategorieABC categorieABC) {
        Query query = entityManager.createNativeQuery(
            "SELECT COUNT(*) " + FROM_CLAUSE + "WHERE p.classe_criticite IN (:classes)");
        query.setParameter("classes", toClassesCriticite(categorieABC));
        return ((Number) query.getSingleResult()).longValue();
    }

    // ── helpers ──

    /** Traduit un CategorieABC en liste de codes ClasseCriticite pour le filtre SQL. */
    private static List<String> toClassesCriticite(CategorieABC cat) {
        return switch (cat) {
            case A -> List.of("A_PLUS", "A");
            case B -> List.of("B");
            case C -> List.of("C", "D");
        };
    }

    @SuppressWarnings("unchecked")
    private List<StockRotationDTO> mapResultsToDTO(List<Object[]> results) {
        return results.stream().map(row -> {
            String ccStr = (String) row[15];
            CategorieABC abc;
            try {
                abc = ccStr != null
                    ? CategorieABC.fromClasseCriticite(ClasseCriticite.fromString(ccStr))
                    : CategorieABC.C;
            } catch (Exception e) {
                abc = CategorieABC.C;
            }

            return new StockRotationDTO(
                row[0] != null ? ((Number) row[0]).intValue() : null,  // produitId
                (String) row[1],                                        // libelle
                (String) row[2],                                        // codeCip
                (String) row[3],                                        // famille → categorie field
                row[4] != null ? ((Number) row[4]).intValue() : 0,     // stockQuantity
                row[5] != null ? ((Number) row[5]).intValue() : 0,     // unitCost
                row[6] != null ? ((Number) row[6]).longValue() : 0L,   // stockValue
                row[7] != null ? ((Number) row[7]).intValue() : 0,     // caLast30Days
                row[8] != null ? ((Number) row[8]).intValue() : 0,     // qtySoldLast30Days
                row[9] != null ? ((Number) row[9]).intValue() : 0,     // nbSalesLast30Days
                row[10] != null ? ((Number) row[10]).intValue() : 0,   // caLast12Months
                row[11] != null ? ((Number) row[11]).intValue() : 0,   // qtySoldLast12Months
                row[13] != null ? new BigDecimal(row[13].toString()) : BigDecimal.ZERO, // rotationRateAnnual
                row[14] != null ? ((Number) row[14]).intValue() : 999, // avgDaysInStock
                abc
            );
        }).toList();
    }
}
