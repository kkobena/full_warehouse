package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Spring Data  repository for the DifferePayment entity.
 */
@Repository
public interface DifferePaymentRepository extends JpaRepository<DifferePayment, Long>, JpaSpecificationExecutor<DifferePayment>,DifferePaymentDataRepository{
    default Specification<DifferePayment> filterByCustomerId(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(DifferePayment_.differeCustomer).get(Customer_.id), customerId);
    }

    default Specification<DifferePayment> filterByPeriode(LocalDate fromDate, LocalDate toDate) {
        return (root, _, cb) ->
            cb.between(cb.function("DATE", LocalDate.class, root.get(DifferePayment_.createdAt)), cb.literal(fromDate), cb.literal(toDate));
    }
}
