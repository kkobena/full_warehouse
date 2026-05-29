package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FamilleProduit_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.RayonProduit_;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.StockProduit_;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.produit.HistoriqueProduitInfo;
import com.kobe.warehouse.service.dto.projection.Id;
import com.kobe.warehouse.service.scheduler.dto.SemoisEligibleItem;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.hasText;

/**
 * Spring Data repository for the Produit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProduitRepository
    extends JpaRepository<Produit, Integer>, JpaSpecificationExecutor<Produit>, SpecificationBuilder, ProduitCustomRepository {
    @Query(value = "SELECT * FROM gettopqty80percentproducts(:startDate, :endDate, :caList, :statutList)", nativeQuery = true)
    List<Object[]> getTopQty80PercentProducts(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("caList") String caList,
        @Param("statutList") String statutList
    );

    @Query(value = "SELECT * FROM gettopamount80percentproducts(:startDate, :endDate, :caList, :statutList)", nativeQuery = true)
    List<Object[]> getTopAmount80PercentProducts(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("caList") String caList,
        @Param("statutList") String statutList
    );

    @Query(value = "SELECT search_produits_json(:qtext, :magasin, :limitResult)", nativeQuery = true)
    String searchProduitsJson(@Param("qtext") String qtext, @Param("magasin") Integer magasin, @Param("limitResult") Integer limitResult);

    @Query(value = "SELECT search_produits_by_storage_json(:qtext, :storageId, :limitResult)", nativeQuery = true)
    String searchProductsByStorage(@Param("qtext") String qtext, @Param("storageId") Integer storageId, @Param("limitResult") Integer limitResult);

    Produit findFirstByParentId(Integer parentId);

    List<Produit> findAllByParentId(Integer parentId);

    List<Produit> findAllByParentIdIsNull();

    Optional<Produit> findOneByLibelle(String libelle);

    @Query(
        value = "SELECT p.libelle AS libelle , o.code_cip AS codeCip,p.code_ean_labo AS codeEan FROM produit p   JOIN fournisseur_produit  o ON p.fournisseur_produit_principal_id = o.id WHERE o.produit_id =?1 ",
        nativeQuery = true
    )
    HistoriqueProduitInfo findHistoriqueProduitInfo(Integer produitId);

    default Specification<Produit> filterByStock() {
        return (root, query, cb) -> {
            query.groupBy(root.get(Produit_.id));
            Join<Produit, StockProduit> stockJoin = root.join(Produit_.stockProduits);
            Expression<Integer> totalQty = cb.sum(stockJoin.get(StockProduit_.qtyStock));
            query.having(cb.greaterThan(totalQty, 0));
            return cb.conjunction();
        };
    }

    default Specification<Produit> filterByProduitStatut() {
        return (root, _, cb) -> cb.equal(root.get(Produit_.status), Status.ENABLE);
    }

    default Specification<Produit> filterByProduitId(Integer produitId) {
        if (isNull(produitId)) {
            return null;
        }
        return (root, _, cb) -> cb.equal(root.get(Produit_.id), produitId);
    }

    default Specification<Produit> filterBySearhTerm(String searhTerm) {
        if (!hasText(searhTerm)) {
            return null;
        }
        return (root, _, cb) -> {
            Join<Produit, FournisseurProduit> produitJoin = root.join(Produit_.fournisseurProduits);
            var seatchT = "%" + searhTerm.toUpperCase() + "%";
            return cb.or(
                cb.like(cb.upper(produitJoin.get(FournisseurProduit_.codeCip)), seatchT),
                cb.like(cb.upper(produitJoin.get(FournisseurProduit_.codeEan)), seatchT),
                cb.like(cb.upper(root.get(Produit_.libelle)), seatchT),
                cb.like(cb.upper(root.get(Produit_.codeEanLaboratoire)), seatchT)
            );
        };
    }

    default Specification<Produit> filterByFournisseurId(Integer fournisseurId) {
        if (isNull(fournisseurId)) {
            return null;
        }
        return (root, _, cb) -> {
            Join<Produit, FournisseurProduit> produitJoin = root.join(Produit_.fournisseurProduits);

            return cb.equal(produitJoin.get(FournisseurProduit_.fournisseur).get(Fournisseur_.id), fournisseurId);
        };
    }

    default Specification<Produit> filterByRayonId(Integer rayonId) {
        if (isNull(rayonId)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<Produit, RayonProduit> rayonJoin = root.join(Produit_.rayonProduits); // collection join

            return cb.equal(rayonJoin.get(RayonProduit_.id), rayonId);
        };
    }

    default Specification<Produit> filterByFamilleProduitId(Integer familleProduitId) {
        if (isNull(familleProduitId)) {
            return null;
        }
        return (root, _, cb) -> cb.equal(root.get(Produit_.famille).get(FamilleProduit_.id), familleProduitId);
    }

    default Specification<Produit> buildCombinedSpecification(LotFilterParam param) {
        Specification<Produit> spec = filterByProduitStatut();
        spec = add(spec, filterByStock());
        spec = add(spec, filterByProduitId(param.getProduitId()));
        spec = add(spec, filterByFournisseurId(param.getFournisseurId()));
        spec = add(spec, filterByRayonId(param.getRayonId()));
        spec = add(spec, filterBySearhTerm(param.getSearchTerm()));
        spec = add(spec, filterByFamilleProduitId(param.getFamilleProduitId()));

        return spec;
    }

    default Specification<Produit> specialisationCritereRecherche(String queryString) {
        return (root, query, cb) -> cb.like(cb.upper(root.get(Produit_.libelle)), queryString.toUpperCase());
    }

    default Specification<Produit> specialisationTypeProduit(TypeProduit typeProduit) {
        return (root, query, cb) -> cb.equal(root.get(Produit_.typeProduit), typeProduit);
    }

    /**
     * Compte les produits actifs par classe de criticité
     *
     * @return Liste de [classe_criticite, count]
     */
    @Query(value = """
        SELECT p.classe_criticite, COUNT(*)
        FROM produit p
        WHERE p.status = 'ENABLE' AND p.type_produit != 'DETAIL'
        GROUP BY p.classe_criticite
        """, nativeQuery = true)
    List<Object[]> countByClasseCriticite();

    /**
     * Compte les produits actifs non-DETAIL (pour dimensionner le batch SEMOIS).
     */
    @Query("SELECT COUNT(p) FROM Produit p WHERE p.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE AND p.typeProduit <> com.kobe.warehouse.domain.enumeration.TypeProduit.DETAIL")
    long countActiveNonDetail();

    /**
     * Charge une page de produits actifs non-DETAIL avec eager fetch du fournisseur principal
     * et sa chaîne (groupe_fournisseur) pour le calcul du délai de livraison en cascade.
     */
    @Query(value = """
        SELECT DISTINCT p FROM Produit p
        LEFT JOIN FETCH p.fournisseurProduitPrincipal fp
        LEFT JOIN FETCH fp.fournisseur f
        LEFT JOIN FETCH f.parent
        WHERE p.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE
          AND p.typeProduit <> com.kobe.warehouse.domain.enumeration.TypeProduit.DETAIL
        """,
        countQuery = """
        SELECT COUNT(p) FROM Produit p
        WHERE p.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE
          AND p.typeProduit <> com.kobe.warehouse.domain.enumeration.TypeProduit.DETAIL
        """)
    Page<Produit> findActiveNonDetailWithFournisseur(Pageable pageable);

    /**
     * Retourne les IDs des produits actifs non-DETAIL présents dans la collection donnée.
     * Utilisé par le batch SEMOIS pour le bulk-load des SemoisConfiguration.
     */
    @Query("SELECT p.id FROM Produit p WHERE p.id IN :ids")
    List<Integer> findIdsByIdIn(@Param("ids") Collection<Integer> ids);

    /**
     * Charge les produits éligibles au batch SEMOIS sous forme de projection légère paginée.
     * Agrège le stock physique par magasin en SQL (évite le chargement des collections)
     * et inclut les données SemoisConfiguration via LEFT JOIN pour éliminer le N+1 batch-load.
     */
    @Query("""
        SELECT new com.kobe.warehouse.service.scheduler.dto.SemoisEligibleItem(
            p.id, fp.id, f.id, fp.qteColis, fp.qteMinimaleCommande,
            COALESCE(SUM(CASE WHEN st.magasin.id = :magasinId THEN (sp.qtyStock + sp.qtyUG) ELSE 0 END), 0),
            sc.stockObjectifCalcule, p.qtySeuilMini, p.qtyAppro, sc.exclusionDate, sc.exclusionDureeJours
        )
        FROM Produit p
        JOIN p.fournisseurProduitPrincipal fp
        JOIN fp.fournisseur f
        LEFT JOIN p.stockProduits sp
        LEFT JOIN sp.storage st
        LEFT JOIN SemoisConfiguration sc ON sc.produit = p
        WHERE p.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE
          AND p.typeProduit = com.kobe.warehouse.domain.enumeration.TypeProduit.PACKAGE
        GROUP BY p.id, fp.id, f.id, fp.qteColis, fp.qteMinimaleCommande,
                 sc.stockObjectifCalcule, p.qtySeuilMini, p.qtyAppro,
                 sc.exclusionDate, sc.exclusionDureeJours
        """)
    Slice<SemoisEligibleItem> findSemoisEligibleItemsSlice(
        @Param("magasinId") Integer magasinId, Pageable pageable);
}
