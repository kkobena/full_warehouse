package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupeTiersPayantRepository extends JpaRepository<GroupeTiersPayant, Integer>, JpaSpecificationExecutor<GroupeTiersPayant> {
    Optional<GroupeTiersPayant> findOneByName(String String);

    default Specification<GroupeTiersPayant> specialisationQueryString(String queryValue) {
        return (root, query, cb) -> cb.or(cb.like(cb.upper(root.get(GroupeTiersPayant_.name)), queryValue));
    }
}
