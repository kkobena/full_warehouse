package com.kobe.warehouse.repository;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

import com.kobe.warehouse.domain.FamilleProduit_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Lot_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLine_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.RayonProduit_;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import jakarta.persistence.criteria.Join;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long>, JpaSpecificationExecutor<Lot>, SpecificationBuilder, LotCustomRepository {
    @Query(
        "SELECT o FROM Lot o JOIN o.orderLine ord JOIN ord.fournisseurProduit fp WHERE fp.produit.id =:produitId AND o.expiryDate >:dateLimit  AND o.quantity > 0  ORDER BY o.expiryDate ASC"
    )
    List<Lot> findByProduitId(Long produitId, LocalDate dateLimit);

    @Query(
        "SELECT o FROM Lot o JOIN o.orderLine ord JOIN ord.fournisseurProduit fp WHERE fp.produit.id =:produitId   AND o.quantity > 0  ORDER BY o.expiryDate ASC"
    )
    List<Lot> findByProduitId(Long produitId);

    default Specification<Lot> filterByStock() {
        return (root, _, cb) -> cb.greaterThan(root.get(Lot_.quantity), 0);
    }

    default Specification<Lot> filterProduitStatut() {
        return (root, _, cb) ->
            cb.equal(
                root.get(Lot_.orderLine).get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.status),
                Status.ENABLE
            );
    }

    default Specification<Lot> filterByProduitId(Long produitId) {
        if (isNull(produitId)) {
            return null; // No filter if id is null
        }
        return (root, _, cb) ->
            cb.equal(
                root.get(Lot_.orderLine).get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.id),
                produitId
            );
    }

    default Specification<Lot> filterByNumLot(String numLot) {
        if (!hasText(numLot)) {
            return null; // No filter if numLot is empty or null
        }
        return (root, _, cb) -> cb.like(cb.upper(root.get(Lot_.numLot)), "%" + numLot.toLowerCase() + "%");
    }

    default Specification<Lot> filterBySearhTerm(String searhTerm) {
        if (!hasText(searhTerm)) {
            return null; // No filter if numLot is empty or null
        }
        return (root, _, cb) ->
            cb.or(
                cb.like(cb.upper(root.get(Lot_.numLot)), "%" + searhTerm.toUpperCase() + "%"),
                cb.like(
                    cb.upper(
                        root.get(Lot_.orderLine).get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.libelle)
                    ),
                    "%" + searhTerm.toUpperCase() + "%"
                ),
                cb.like(
                    cb.upper(root.get(Lot_.orderLine).get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.codeCip)),
                    "%" + searhTerm.toUpperCase() + "%"
                ),
                cb.like(
                    cb.upper(
                        root.get(Lot_.orderLine).get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.codeEan)
                    ),
                    "%" + searhTerm.toUpperCase() + "%"
                )
            );
    }

    default Specification<Lot> filterByDayCount(int dayCount) {
        if (dayCount <= 0) {
            return null; // No filter if dayCount is negative
        }
        return (root, _, cb) -> cb.lessThanOrEqualTo(root.get(Lot_.expiryDate), LocalDate.now().plusDays(dayCount));
    }

    default Specification<Lot> filterByFromDate(LocalDate fromDate) {
        if (isNull(fromDate)) {
            return null; // No filter if fromDate is null
        }
        return (root, _, cb) -> cb.greaterThanOrEqualTo(root.get(Lot_.expiryDate), fromDate);
    }

    default Specification<Lot> filterByDateRange(LocalDate fromDate, LocalDate toDate) {
        if (isNull(fromDate) || isNull(toDate)) {
            return null; // No filter if fromDate or toDate is null
        }
        return (root, _, cb) -> cb.between(root.get(Lot_.expiryDate), fromDate, toDate);
    }

    default Specification<Lot> filterByFournisseurId(Long fournisseurId) {
        if (isNull(fournisseurId)) {
            return null; // No filter if fournisseurId is null
        }
        return (root, _, cb) ->
            cb.equal(
                root.get(Lot_.orderLine).get(OrderLine_.fournisseurProduit).get(FournisseurProduit_.fournisseur).get(Fournisseur_.id),
                fournisseurId
            );
    }

    default Specification<Lot> filterByRayonId(Long rayonId) {
        if (isNull(rayonId)) {
            return null; // No filter if rayonId is null
        }

        return (root, query, cb) -> {
            Join<Lot, OrderLine> orderLineJoin = root.join(Lot_.orderLine);
            Join<OrderLine, FournisseurProduit> fournisseurProduitJoin = orderLineJoin.join(OrderLine_.fournisseurProduit);
            Join<FournisseurProduit, Produit> produitJoin = fournisseurProduitJoin.join(FournisseurProduit_.produit);
            Join<Produit, RayonProduit> rayonJoin = produitJoin.join(Produit_.rayonProduits); // collection join

            return cb.equal(rayonJoin.get(RayonProduit_.id), rayonId);
        };
    }

    default Specification<Lot> filterByFamilleProduitId(Long familleProduitId) {
        if (isNull(familleProduitId)) {
            return null; // No filter if familleProduitId is null
        }
        return (root, _, cb) ->
            cb.equal(
                root
                    .get(Lot_.orderLine)
                    .get(OrderLine_.fournisseurProduit)
                    .get(FournisseurProduit_.produit)
                    .get(Produit_.famille)
                    .get(FamilleProduit_.id),
                familleProduitId
            );
    }

    default Specification<Lot> buildCombinedSpecification(LotFilterParam param) {
        Specification<Lot> spec = filterProduitStatut();
        spec = add(spec, filterByStock());
        spec = add(spec, filterByProduitId(param.getProduitId()));
        spec = add(spec, filterByFournisseurId(param.getFournisseurId()));
        spec = add(spec, filterByRayonId(param.getRayonId()));
        spec = add(spec, filterByNumLot(param.getNumLot()));
        spec = add(spec, filterBySearhTerm(param.getSearchTerm()));
        if (param.getDayCount() > 0) {
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
}
