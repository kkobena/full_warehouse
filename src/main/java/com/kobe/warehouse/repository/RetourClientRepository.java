package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourClient;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kobe.warehouse.domain.RetourClient_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public interface RetourClientRepository extends JpaRepository<RetourClient, Integer>, JpaSpecificationExecutor<RetourClient> {

    static Specification<RetourClient> buildSpec(String search, LocalDate fromDate, LocalDate toDate) {
        Specification<RetourClient> spec = Specification.unrestricted();
        if (fromDate != null && toDate != null) {
            spec = spec.and((root, query, cb) ->
                cb.between(root.get(RetourClient_.createdAt),
                    fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()));
        }
        if (StringUtils.hasText(search)) {
            String term = "%" + search.toUpperCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.upper(root.get(RetourClient_.reference)), term),
                cb.like(cb.upper(root.get(RetourClient_.originalSaleRef)), term)
            ));
        }
        return spec;
    }
}
