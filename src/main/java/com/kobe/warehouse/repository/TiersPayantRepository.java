package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TiersPayantRepository extends JpaRepository<TiersPayant, Long>, JpaSpecificationExecutor<TiersPayant> {
    Optional<TiersPayant> findOneByNameOrFullName(String name, String fullName);

    List<TiersPayant> findAllByGroupeTiersPayantId(Long groupeTiersPayantId);

    default Specification<TiersPayant> specialisationStatut(TiersPayantStatut statut) {
        return (root, query, cb) -> cb.equal(root.get(TiersPayant_.statut), statut);
    }

    default Specification<TiersPayant> specialisationByGroupAndStatut(long groupId, TiersPayantStatut statut) {
        return (root, query, cb) -> cb.and(cb.equal(root.get(TiersPayant_.statut), statut), cb.equal(root.get(TiersPayant_.groupeTiersPayant).get(GroupeTiersPayant_.id), groupId));
    }

    default Specification<TiersPayant> specialisationQueryString(String queryValue) {
        return (root, query, cb) -> cb.or(cb.like(cb.upper(root.get(TiersPayant_.name)), queryValue), cb.like(cb.upper(root.get(TiersPayant_.fullName)), queryValue), cb.like(cb.upper(root.get(TiersPayant_.codeOrganisme)), queryValue),
            cb.like(cb.upper(root.get(TiersPayant_.codeRegroupement)), queryValue));
    }
}
