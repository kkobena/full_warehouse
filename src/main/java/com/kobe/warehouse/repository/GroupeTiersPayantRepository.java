package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.enumeration.Periodicite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupeTiersPayantRepository
    extends JpaRepository<GroupeTiersPayant, Integer>, JpaSpecificationExecutor<GroupeTiersPayant> {
    Optional<GroupeTiersPayant> findOneByName(String String);

    @Query("""
        SELECT g.id FROM GroupeTiersPayant g
        WHERE g.periodiciteFactureDefinitive = :periodicite
          AND g.inclureFacturationAutoDefinitive = true
        """)
    List<Integer> findIdsForAutoGenerationDefinitive(@Param("periodicite") Periodicite periodicite);

    @Query("""
        SELECT g.id FROM GroupeTiersPayant g
        WHERE g.periodiciteFactureProvisoire = :periodicite
          AND g.inclureFacturationAutoProvisoire = true
        """)
    List<Integer> findIdsForAutoGenerationProvisoire(@Param("periodicite") Periodicite periodicite);

    default Specification<GroupeTiersPayant> specialisationQueryString(String queryValue) {
        return (root, query, cb) -> cb.or(cb.like(cb.upper(root.get(GroupeTiersPayant_.name)), queryValue));
    }

    default Specification<GroupeTiersPayant> specialisationPeriodiciteDefinitive(Periodicite periodicite) {
        return (root, _, cb) -> cb.equal(root.get(GroupeTiersPayant_.periodiciteFactureDefinitive), periodicite);
    }

    default Specification<GroupeTiersPayant> specialisationPeriodiciteProvisoire(Periodicite periodicite) {
        return (root, _, cb) -> cb.equal(root.get(GroupeTiersPayant_.periodiciteFactureProvisoire), periodicite);
    }
}
