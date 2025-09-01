package com.kobe.warehouse.repository;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

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
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Produit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProduitRepository
    extends JpaRepository<Produit, Long>, JpaSpecificationExecutor<Produit>, SpecificationBuilder, ProduitCustomRepository {
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

    @Procedure(procedureName = "gettopqty80percentproducts")
    List<Object[]> getTopQty80PercentProducts__(LocalDate startDate, LocalDate endDate, String caList, String statutList);

    @Procedure(procedureName = "gettopamount80percentproducts")
    List<Object[]> getTopAmount80PercentProducts__(LocalDate startDate, LocalDate endDate, String caList, String statutList);

    Produit findFirstByParentId(Long parentId);

    List<Produit> findAllByParentIdIsNull();

    Optional<Produit> findOneByLibelle(String libelle);

    @Query(
        value = "SELECT p.libelle AS libelle , o.code_cip AS codeCip,p.code_ean AS codeEan FROM produit p   JOIN fournisseur_produit  o ON o.fournisseur_produit_princial_id = p.id WHERE o.produit_id =?1 ",
        nativeQuery = true
    )
    HistoriqueProduitInfo findHistoriqueProduitInfo(Long produitId);

    default Specification<Produit> filterByStock() {
        return (root, query, cb) -> {
            assert query != null;
            query.groupBy(root.get(Produit_.id));
            Join<Produit, StockProduit> stockJoin = root.join(Produit_.stockProduits);
            Expression<Integer> totalQty = cb.sum(stockJoin.get(StockProduit_.qtyStock));
            query.having(cb.greaterThan(totalQty, 0));
            return cb.conjunction();
        };
    }

    default Specification<Produit> filterByDatePeremptionNotNul() {
        return (root, _, cb) -> cb.isNotNull(root.get(Produit_.perimeAt));
    }

    default Specification<Produit> filterByProduitStatut() {
        return (root, _, cb) -> cb.equal(root.get(Produit_.status), Status.ENABLE);
    }

    default Specification<Produit> filterByProduitId(Long produitId) {
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

    default Specification<Produit> filterByDayCount(int dayCount) {
        if (dayCount <= 0) {
            return null;
        }
        return (root, _, cb) -> cb.lessThanOrEqualTo(root.get(Produit_.perimeAt), LocalDate.now().plusDays(dayCount));
    }

    default Specification<Produit> filterByFromDate(LocalDate fromDate) {
        if (isNull(fromDate)) {
            return null;
        }
        return (root, _, cb) -> cb.greaterThanOrEqualTo(root.get(Produit_.perimeAt), fromDate);
    }

    default Specification<Produit> filterByDateRange(LocalDate fromDate, LocalDate toDate) {
        if (isNull(fromDate) || isNull(toDate)) {
            return null;
        }
        return (root, _, cb) -> cb.between(root.get(Produit_.perimeAt), fromDate, toDate);
    }

    default Specification<Produit> filterByFournisseurId(Long fournisseurId) {
        if (isNull(fournisseurId)) {
            return null;
        }
        return (root, _, cb) -> {
            Join<Produit, FournisseurProduit> produitJoin = root.join(Produit_.fournisseurProduits);

            return cb.equal(produitJoin.get(FournisseurProduit_.fournisseur).get(Fournisseur_.id), fournisseurId);
        };
    }

    default Specification<Produit> filterByRayonId(Long rayonId) {
        if (isNull(rayonId)) {
            return null;
        }

        return (root, query, cb) -> {
            Join<Produit, RayonProduit> rayonJoin = root.join(Produit_.rayonProduits); // collection join

            return cb.equal(rayonJoin.get(RayonProduit_.id), rayonId);
        };
    }

    default Specification<Produit> filterByFamilleProduitId(Long familleProduitId) {
        if (isNull(familleProduitId)) {
            return null;
        }
        return (root, _, cb) -> cb.equal(root.get(Produit_.famille).get(FamilleProduit_.id), familleProduitId);
    }

    default Specification<Produit> buildCombinedSpecification(LotFilterParam param) {
        Specification<Produit> spec = filterByProduitStatut();
        spec = add(spec, filterByDatePeremptionNotNul());
        spec = add(spec, filterByStock());
        spec = add(spec, filterByProduitId(param.getProduitId()));
        spec = add(spec, filterByFournisseurId(param.getFournisseurId()));
        spec = add(spec, filterByRayonId(param.getRayonId()));
        spec = add(spec, filterBySearhTerm(param.getSearchTerm()));
        if (nonNull(param.getDayCount())) {
            spec = add(spec, filterByDayCount(param.getDayCount()));
        } else {
            if (nonNull(param.getFromDate()) && nonNull(param.getToDate())) {
                spec = add(spec, filterByDateRange(param.getFromDate(), param.getToDate()));
            } else {
                spec = add(spec, filterByFromDate(param.getFromDate()));
            }
        }
        spec = add(spec, filterByFamilleProduitId(param.getFamilleProduitId()));

        return spec;
    }

    default Specification<Produit> specialisationCritereRecherche(String queryString) {
        return (root, query, cb) -> cb.like(cb.upper(root.get(Produit_.libelle)), queryString.toUpperCase());
    }

    default Specification<Produit> specialisationTypeProduit(TypeProduit typeProduit) {
        return (root, query, cb) -> cb.equal(root.get(Produit_.typeProduit), typeProduit);
    }
}
