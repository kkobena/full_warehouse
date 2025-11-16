package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.RayonProduit_;
import com.kobe.warehouse.domain.Rayon_;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleSummary;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.SetJoin;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the SalesLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesLineRepository
    extends JpaRepository<SalesLine, SaleLineId>, JpaSpecificationExecutor<SalesLine>, SalesLineRepositoryCustom {
    List<SalesLine> findBySalesIdAndSalesSaleDateOrderByProduitLibelle(Long salesId, LocalDate saleDate);

    Optional<SalesLine> findBySalesIdAndProduitIdAndSalesSaleDate(Long salesId, Integer produitId, LocalDate saleDate);

    List<SalesLine> findAllByQuantityAvoirGreaterThan(Integer zero);

    @Query(
        value = "SELECT MAX(s.updated_at) AS updatedAt FROM sales_line o JOIN sales s ON o.sales_id = s.id WHERE o.produit_id =:produitId AND s.statut=:statut",
        nativeQuery = true
    )
    LastDateProjection findLastUpdatedAtByProduitIdAndSalesStatut(@Param("produitId") Integer produitId, @Param("statut") String statut);

    @Query(
        value = "SELECT get_historique_vente(:produitId,:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:offset,:limit)",
        nativeQuery = true
    )
    String getHistoriqueVente(
        @Param("produitId") Integer produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    @Query(
        value = "SELECT get_product_sales_summary_monthly(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:produitId)",
        nativeQuery = true
    )
    // List<HistoriqueProduitVenteMensuelle>
    String getHistoriqueVenteMensuelle(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("produitId") Integer produitId
    );

    @Query(
        value = "SELECT get_product_sales_summary(:startDate, :endDate, :statuts,:caterorieChiffreAffaire,:produitId,:groupBy)",
        nativeQuery = true
    )
    String getHistoriqueVenteSummary(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") String[] statuts,
        @Param("caterorieChiffreAffaire") String[] caterorieChiffreAffaire,
        @Param("produitId") Integer produitId,
        @Param("groupBy") int groupBy
    );

    @Query(
        value = "SELECT SUM(o.quantity_requested) AS quantite  FROM sales_line o JOIN sales s ON o.sales_id = s.id  WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND s.sale_date BETWEEN :startDate AND :endDate AND o.sales_sale_date=s.sale_date",
        nativeQuery = true
    )
    HistoriqueProduitVenteMensuelleSummary getHistoriqueVenteMensuelleSummary(
        @Param("produitId") Integer produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts
    );

    default Specification<SalesLine> filterBySearchTerm(String search) {
        return (root, query, cb) -> {
            String searchTerm = search.toUpperCase() + "%";
            Join<SalesLine, Produit> produitJoin = root.join(SalesLine_.produit);
            SetJoin<Produit, FournisseurProduit> fournisseurProduitProduitJoin = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS);
            return cb.or(
                cb.like(cb.upper(fournisseurProduitProduitJoin.get(FournisseurProduit_.codeCip)), searchTerm),
                cb.like(cb.upper(fournisseurProduitProduitJoin.get(FournisseurProduit_.codeEan)), searchTerm),
                cb.like(cb.upper(produitJoin.get(Produit_.codeEanLaboratoire)), searchTerm),
                cb.like(cb.upper(produitJoin.get(Produit_.libelle)), searchTerm)
            );
        };
    }

    default Specification<SalesLine> filterByPeriode(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> cb.between(root.get(SalesLine_.sales).get(Sales_.saleDate), fromDate, toDate);
    }

    default Specification<SalesLine> filterByStatut(EnumSet<SalesStatut> statuts) {
        return (root, query, cb) -> root.get(SalesLine_.sales).get(Sales_.statut).in(statuts);
    }

    default Specification<SalesLine> filterByCanceled(boolean canceled) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.sales).get(Sales_.canceled), canceled);
    }

    default Specification<SalesLine> filterByDiffereOnly(boolean differeOnly) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.sales).get(Sales_.imported), differeOnly);
    }

    default Specification<SalesLine> filterByStorageId(Integer magasinId) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.sales).get(Sales_.magasin).get(Magasin_.id), magasinId);
    }

    default Specification<SalesLine> filterByUserId(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.sales).get(Sales_.caissier).get(AppUser_.id), userId);
    }

    default Specification<SalesLine> filterByProduitId(Integer produitId) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.produit).get(Produit_.id), produitId);
    }

    default Specification<SalesLine> filterByRayonId(Integer rayonId) {
        return (root, query, cb) -> {
            Join<SalesLine, Produit> produitJoin = root.join(SalesLine_.produit);
            SetJoin<Produit, RayonProduit> rayonProduitProduitSetJoin = produitJoin.joinSet(Produit_.RAYON_PRODUITS);
            return cb.equal(rayonProduitProduitSetJoin.get(RayonProduit_.rayon).get(Rayon_.id), rayonId);
        };
    }

    default Specification<SalesLine> filterByCa(EnumSet<CategorieChiffreAffaire> categorieChiffreAffaires) {
        return (root, query, cb) -> root.get(SalesLine_.sales).get(Sales_.categorieChiffreAffaire).in(categorieChiffreAffaires);
    }

    default Specification<SalesLine> notImported() {
        return (root, query, cb) -> cb.isFalse(root.get(SalesLine_.sales).get(Sales_.imported));
    }
}
