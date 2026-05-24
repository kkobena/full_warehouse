package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Magasin_;
import com.kobe.warehouse.domain.RetourDepot;
import com.kobe.warehouse.domain.RetourDepot_;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the RetourDepot entity.
 */
@Repository
public interface RetourDepotRepository extends JpaRepository<RetourDepot, Integer>, JpaSpecificationExecutor<RetourDepot> {
    @Query("SELECT rd FROM RetourDepot rd WHERE rd.dateMtv BETWEEN :dtStart AND :dtEnd AND rd.depot.id = :depotId ORDER BY rd.dateMtv DESC")
    Page<RetourDepot> findAllByDateRange(
        @Param("depotId") Integer depotId,
        @Param("dtStart") LocalDateTime dtStart,
        @Param("dtEnd") LocalDateTime dtEnd,
        Pageable pageable
    );

    @Query("SELECT rd FROM RetourDepot rd LEFT JOIN FETCH rd.retourDepotItems WHERE rd.id = :id")
    Optional<RetourDepot> findOneWithItems(@Param("id") Integer id);

    default Specification<RetourDepot> filterByDateRangeAndDepot(Integer depotId, LocalDateTime dtStart, LocalDateTime dtEnd) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();
            if (depotId != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get(RetourDepot_.depot).get(Magasin_.id), depotId));
            }
            if (dtStart != null && dtEnd != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.between(root.get(RetourDepot_.dateMtv), dtStart, dtEnd));
            }
            return predicates;
        };
    }
}
