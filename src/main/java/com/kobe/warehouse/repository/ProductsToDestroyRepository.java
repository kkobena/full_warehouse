package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.ProductsToDestroy;
import com.kobe.warehouse.domain.ProductsToDestroy_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.RayonProduit_;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

@Repository
public interface ProductsToDestroyRepository
    extends
    JpaRepository<ProductsToDestroy, Integer>,
    JpaSpecificationExecutor<ProductsToDestroy>,
    SpecificationBuilder,
    ProductsToDestroyCustomRepository {
    @Query("SELECT o FROM ProductsToDestroy o WHERE o.editing  AND FUNCTION('DATE',o.created) =:toDay AND o.user.id =:userId")
    List<ProductsToDestroy> findAllByEditingTrueAndCreatedEquals( LocalDate toDay,Integer userId);

    Optional<ProductsToDestroy> findByNumLotAndFournisseurProduitProduitId(String numLot, Integer produitId);

    default Specification<ProductsToDestroy> isDestroyed(Boolean destroyed) {
        if (nonNull(destroyed)) {
            return (root, _, cb) -> cb.equal(root.get(ProductsToDestroy_.destroyed), destroyed);
        }
        return null;
    }

    default Specification<ProductsToDestroy> isEditing(Boolean editing) {
        if (nonNull(editing)) {
            return (root, _, cb) -> cb.equal(root.get(ProductsToDestroy_.editing), editing);
        }
        return null;
    }

    default Specification<ProductsToDestroy> filterBySearchTerm(String searchTerm) {
        if (hasText(searchTerm)) {
            String search = searchTerm.trim().toUpperCase() + "%";
            return (root, _, cb) -> {
                Join<ProductsToDestroy, FournisseurProduit> produitJoin = root.join(ProductsToDestroy_.fournisseurProduit);
                Join<FournisseurProduit, Produit> produitFournJoin = produitJoin.join(FournisseurProduit_.produit);
                return cb.or(
                    cb.like(produitJoin.get(FournisseurProduit_.codeCip), search),
                    cb.like(root.get(ProductsToDestroy_.numLot), search),
                    cb.like(cb.upper(produitFournJoin.get(Produit_.libelle)), search)
                );
            };
        }
        return null;
    }

    default Specification<ProductsToDestroy> filterByFournisseurId(Integer fournisseurId) {
        if (isNull(fournisseurId)) {
            return null;
        }

        return (root, _, cb) ->
            cb.equal(
                root.get(ProductsToDestroy_.fournisseurProduit).get(FournisseurProduit_.fournisseur).get(Fournisseur_.id),
                fournisseurId
            );
    }

    default Specification<ProductsToDestroy> filterByUserId(Integer userId) {
        if (isNull(userId)) {
            return null;
        }

        return (root, _, cb) -> cb.equal(root.get(ProductsToDestroy_.user).get(AppUser_.id), userId);
    }

    default Specification<ProductsToDestroy> filterByRayonId(Integer rayonId) {
        if (isNull(rayonId)) {
            return null;
        }

        return (root, _, cb) -> {
            Join<ProductsToDestroy, FournisseurProduit> produitJoin = root.join(ProductsToDestroy_.fournisseurProduit);
            Join<FournisseurProduit, Produit> produitFourn = produitJoin.join(FournisseurProduit_.produit);
            Join<Produit, RayonProduit> rayonJoin = produitFourn.join(Produit_.rayonProduits);

            return cb.equal(rayonJoin.get(RayonProduit_.id), rayonId);
        };
    }

    default Specification<ProductsToDestroy> filterByDateRange(LocalDate fromDate, LocalDate toDate) {
        if (isNull(fromDate) || isNull(toDate)) {
            return null;
        }
        LocalDateTime createdFrom = fromDate.atStartOfDay();
        LocalDateTime createdTo = toDate.atTime(LocalTime.MAX);
        return (root, _, cb) -> cb.between(root.get(ProductsToDestroy_.created), createdFrom, createdTo);
    }

    default Specification<ProductsToDestroy> filterByMagasinId(Integer magasinId) {
        if (isNull(magasinId)) {
            return null; // No filter if id is null
        }

        return (root, _, cb) -> cb.equal(root.get(ProductsToDestroy_.magasin).get(Magasin_.id), magasinId);
    }

    default Specification<ProductsToDestroy> buildCombinedSpecification(ProductToDestroyFilter filter) {
        Specification<ProductsToDestroy> spec = isDestroyed(filter.destroyed());
        spec = add(spec, filterBySearchTerm(filter.searchTerm()));
        spec = add(spec, filterByFournisseurId(filter.fournisseurId()));
        spec = add(spec, filterByUserId(filter.userId()));
        spec = add(spec, filterByRayonId(filter.rayonId()));
        spec = add(spec, isEditing(filter.editing()));
        if (nonNull(filter.fromDate()) && nonNull(filter.toDate())) {
            spec = add(spec, filterByDateRange(filter.fromDate(), filter.toDate()));
        }
        spec = add(spec, filterByMagasinId(filter.magasinId()));

        return spec;
    }

    default Specification<ProductsToDestroy> buildEditing(Integer userId, String searchTerm) {
        Specification<ProductsToDestroy> spec = filterByUserId(userId);
        spec = add(spec, filterBySearchTerm(searchTerm));
        spec = add(spec, isEditing(true));
        var now = LocalDate.now();
        spec = add(spec, filterByDateRange(now, now));
        return spec;
    }
}
