package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.AvoirFournisseur_;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvoirFournisseurRepository extends JpaRepository<AvoirFournisseur, Integer>, JpaSpecificationExecutor<AvoirFournisseur> {


    @Query("""
        SELECT a.fournisseur.id, a.fournisseur.libelle, SUM(a.montant)
        FROM AvoirFournisseur a
        WHERE a.statut =:statut
        GROUP BY a.fournisseur.id, a.fournisseur.libelle
        ORDER BY SUM(a.montant) DESC
        """)
    List<Object[]> sumParFournisseur(@Param("statut") AvoirFournisseurStatut statut);

    default Specification<AvoirFournisseur> hasStatut(@NotNull AvoirFournisseurStatut statut) {
        return (root, query, cb) -> statut == null ? null : cb.equal(root.get(AvoirFournisseur_.statut), statut);
    }

    default Specification<AvoirFournisseur> hasFournisseurId(@NotNull Integer fournisseurId) {
        return (root, query, cb) -> fournisseurId == null ? null : cb.equal(root.get(AvoirFournisseur_.fournisseur).get("id"), fournisseurId);
    }

    default Specification<AvoirFournisseur> dateMtvBetween(LocalDateTime dtStart, LocalDateTime dtEnd) {
        return (root, query, cb) -> {
            if (dtStart != null && dtEnd != null) {
                return cb.between(root.get(AvoirFournisseur_.dateMtv), dtStart, dtEnd);
            } else if (dtStart != null) {
                return cb.greaterThanOrEqualTo(root.get(AvoirFournisseur_.dateMtv), dtStart);
            } else if (dtEnd != null) {
                return cb.lessThanOrEqualTo(root.get(AvoirFournisseur_.dateMtv), dtEnd);
            } else {
                return null;

            }
        };
    }

    default Specification<AvoirFournisseur> byReference(@NotNull String reference) {

        return (root, query, cb) -> cb.like(root.get(AvoirFournisseur_.reference), reference + "%");
    }


    @Query(
        "SELECT a FROM AvoirFournisseur a " +
        "JOIN FETCH a.fournisseur " +
        "ORDER BY a.dateMtv DESC"
    )
    List<AvoirFournisseur> findAllWithFournisseur();

    long countByStatut(AvoirFournisseurStatut statut);

    default Specification<AvoirFournisseur> buildSpecification(AvoirFournisseurStatut statut, String reference, Integer fournisseurId, LocalDateTime dtStart, LocalDateTime dtEnd) {

        Specification<AvoirFournisseur> spec = Specification.unrestricted();
        if (statut != null) {
            spec = spec.and(hasStatut(statut));
        }
        if (StringUtils.hasText(reference)) {
            spec = spec.and(byReference(reference));
        }
        if (fournisseurId != null) {
            spec = spec.and(hasFournisseurId(fournisseurId));
        }
        if (dtStart != null || dtEnd != null) {
            spec = spec.and(dateMtvBetween(dtStart, dtEnd));
        }
        return spec;
    }

}
