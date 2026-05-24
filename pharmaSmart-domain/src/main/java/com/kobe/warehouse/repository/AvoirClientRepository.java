package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.AvoirClient_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AvoirClientRepository extends JpaRepository<AvoirClient, Integer>, JpaSpecificationExecutor<AvoirClient> {

    Optional<AvoirClient> findBySalesLineId(Long salesLineId);

    boolean existsByStatutAndCommandeIsNull(AvoirClientStatut statut);

    List<AvoirClient> findByStatutAndDateExpirationLessThanEqual(AvoirClientStatut statut, LocalDate date);

    List<AvoirClient> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(a), COALESCE(SUM(a.montant - a.montantUtilise), 0) FROM AvoirClient a WHERE a.statut = 'OUVERT'")
    Object[] statsAvoirsOuverts();

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(a) FROM AvoirClient a WHERE a.statut = 'OUVERT' AND a.dateExpiration IS NOT NULL AND a.dateExpiration < :seuil")
    long countAvoirsProchesExpiration(@org.springframework.data.repository.query.Param("seuil") java.time.LocalDate seuil);

    static Specification<AvoirClient> forCommande(Set<Integer> produitIds) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get(AvoirClient_.statut), AvoirClientStatut.OUVERT),
            root.get(AvoirClient_.produit).get(Produit_.id).in(produitIds),
            root.get(AvoirClient_.commande).isNull()
        );
    }

    static Specification<AvoirClient> buildSpec(String search, LocalDate fromDate, LocalDate toDate, AvoirClientStatut statut) {
        Specification<AvoirClient> spec = Specification.unrestricted();
        Set<AvoirClientStatut> statuts=Set.of(AvoirClientStatut.OUVERT,AvoirClientStatut.CLOTURE);
        if (statut != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get(AvoirClient_.statut), statut));
        }else{
            spec = spec.and((root, query, cb) -> root.get(AvoirClient_.statut).in(statuts));
        }
        if (fromDate != null && toDate != null) {
            spec = spec.and((root, query, cb) ->
                cb.between(root.get(AvoirClient_.createdAt),
                    fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()));
        }
        if (StringUtils.hasText(search)) {
            String term = "%" + search.toUpperCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.upper(root.get(AvoirClient_.produit).get(Produit_.libelle)), term),
                cb.like(cb.upper(root.get(AvoirClient_.reference)), term),
                cb.like(cb.upper(root.join(AvoirClient_.salesLine, JoinType.LEFT)
                    .join(SalesLine_.sales, JoinType.LEFT)
                    .get(Sales_.numberTransaction)), term)
            ));
        }
        return spec;
    }
}
