package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.RayonProduit_;
import com.kobe.warehouse.domain.Rayon_;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.User_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.HistoriqueProduitVente;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelle;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteSummary;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.SetJoin;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    extends JpaRepository<SalesLine, Long>, JpaSpecificationExecutor<SalesLine>, SalesLineRepositoryCustom {
    List<SalesLine> findBySalesIdOrderByProduitLibelle(Long salesId);

    Optional<SalesLine> findBySalesIdAndProduitId(Long salesId, Long produitId);

    Optional<List<SalesLine>> findAllBySalesId(Long salesId);

    List<SalesLine> findAllByQuantityAvoirGreaterThan(Integer zero);

    @Query(
        value = "SELECT MAX(s.updated_at) AS updatedAt FROM sales_line o JOIN sales s ON o.sales_id = s.id WHERE o.produit_id =:produitId AND s.statut=:statut",
        nativeQuery = true
    )
    LastDateProjection findLastUpdatedAtByProduitIdAndSalesStatut(@Param("produitId") Long produitId, @Param("statut") String statut);

    @Query(
        value = "SELECT s.updated_at AS mvtDate,s.number_transaction AS reference,o.quantity_requested AS quantite," +
        "o.regular_unit_price AS prixUnitaire,o.ht_amount AS montantHt,o.net_amount AS montantNet,o.sales_amount AS montantTtc," +
        "o.discount_amount AS montantRemise,o.tax_amount AS montantTva,u.first_name AS firstName,u.last_name AS lastName    FROM sales_line o JOIN sales s ON o.sales_id = s.id JOIN user u ON s.caissier_id =u.id WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate ORDER BY s.updated_at DESC",
        nativeQuery = true
    )
    Page<HistoriqueProduitVente> getHistoriqueVente(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts,
        Pageable pageable
    );

    @Query(
        value = "SELECT YEAR(s.updated_at) AS annee,SUM(o.quantity_requested) AS quantite,MONTH(s.updated_at) AS mois    FROM sales_line o JOIN sales s ON o.sales_id = s.id  WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate GROUP BY YEAR(s.updated_at),MONTH(s.updated_at) ORDER BY YEAR(s.updated_at) DESC",
        nativeQuery = true
    )
    List<HistoriqueProduitVenteMensuelle> getHistoriqueVenteMensuelle(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts
    );

    @Query(
        value = "SELECT SUM(o.quantity_requested) AS quantite, SUM(o.sales_amount) AS montantTtc, SUM(o.ht_amount) AS montantHt, SUM(o.discount_amount) AS montantRemise, SUM(o.tax_amount) AS montantTva,SUM(o.net_amount) AS montantNet FROM sales_line o JOIN sales s ON o.sales_id = s.id WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate",
        nativeQuery = true
    )
    HistoriqueProduitVenteSummary getHistoriqueVenteSummary(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts
    );

    @Query(
        value = "SELECT SUM(o.quantity_requested) AS quantite  FROM sales_line o JOIN sales s ON o.sales_id = s.id  WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate",
        nativeQuery = true
    )
    HistoriqueProduitVenteMensuelleSummary getHistoriqueVenteMensuelleSummary(
        @Param("produitId") long produitId,
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
                cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), searchTerm),
                cb.like(cb.upper(produitJoin.get(Produit_.libelle)), searchTerm),
                cb.like(cb.upper(fournisseurProduitProduitJoin.get(FournisseurProduit_.codeCip)), searchTerm)
            );
        };
    }

    default Specification<SalesLine> filterByPeriode(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) ->
            cb.between(cb.function("DATE", LocalDate.class, root.get(SalesLine_.sales).get(Sales_.updatedAt)), fromDate, toDate);
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

    default Specification<SalesLine> filterByStorageId(Long magasinId) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.sales).get(Sales_.magasin).get(Magasin_.id), magasinId);
    }

    default Specification<SalesLine> filterByUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.sales).get(Sales_.caissier).get(User_.id), userId);
    }

    default Specification<SalesLine> filterByProduitId(Long produitId) {
        return (root, query, cb) -> cb.equal(root.get(SalesLine_.produit).get(Produit_.id), produitId);
    }

    default Specification<SalesLine> filterByRayonId(Long rayonId) {
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
