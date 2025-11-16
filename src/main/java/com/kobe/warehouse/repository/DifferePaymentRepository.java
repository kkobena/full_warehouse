package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.DifferePayment;
import com.kobe.warehouse.domain.DifferePayment_;
import com.kobe.warehouse.domain.PaymentId;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the DifferePayment entity.
 */
@Repository
public interface DifferePaymentRepository
    extends JpaRepository<DifferePayment, PaymentId>, JpaSpecificationExecutor<DifferePayment>, DifferePaymentDataRepository {
    default Specification<DifferePayment> filterByCustomerId(Integer customerId) {
        if (customerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(DifferePayment_.differeCustomer).get(Customer_.id), customerId);
    }

    default Specification<DifferePayment> filterByPeriode(LocalDate fromDate, LocalDate toDate) {
        return (root, _, cb) -> cb.between(root.get(DifferePayment_.transactionDate), fromDate, toDate);
    }
}
