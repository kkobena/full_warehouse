package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturationRepository extends JpaRepository<FactureTiersPayant, Long>,
    JpaSpecificationExecutor<FactureTiersPayant> {


    @Query(value = "SELECT f.num_facture FROM facture_tiers_payant f  ORDER BY f.id DESC LIMIT 1", nativeQuery = true)
    String findLatestFactureNumber();

    List<FactureTiersPayant> findAllByCreatedEquals(LocalDateTime created, Sort sort);

    List<FactureTiersPayant> findAllByCreatedEqualsAndGroupeFactureTiersPayantIsNull(
        LocalDateTime created, Sort sort);

    default Specification<FactureTiersPayant> fetchByIs(Set<Long> ids) {
        return (root, _, cb) -> {
            In<Long> selectionIds = cb.in(root.get(FactureTiersPayant_.id));
            ids.forEach(selectionIds::value);
            return selectionIds;
        };
    }
}
