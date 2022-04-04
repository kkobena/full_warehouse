package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.UninsuredCustomer_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UninsuredCustomerRepository extends JpaRepository<UninsuredCustomer, Long>, JpaSpecificationExecutor<UninsuredCustomer> {
    Optional<UninsuredCustomer> findOneByCode(String code);

    default Specification<UninsuredCustomer> specialisationQueryString(String queryValue) {
        return (root, query, cb) -> cb.or(cb.like(cb.upper(root.get(UninsuredCustomer_.firstName)), queryValue), cb.like(cb.upper(root.get(UninsuredCustomer_.lastName)), queryValue), cb.like(cb.upper(root.get(UninsuredCustomer_.code)), queryValue),
            cb.like(cb.upper(cb.concat(cb.concat(root.get(UninsuredCustomer_.firstName), " "), root.get(UninsuredCustomer_.lastName))), queryValue), cb.like(cb.upper(root.get(UninsuredCustomer_.phone)), queryValue)
        );
    }

    default Specification<UninsuredCustomer> specialisation(Status status) {
        return (root, query, cb) -> cb.equal(root.get(UninsuredCustomer_.status), status);
    }

    default Specification<UninsuredCustomer> specialisationCheckExist(String firstName, String lastName, String phone) {
        return (root, query, cb) -> cb.and(cb.equal(cb.upper(root.get(UninsuredCustomer_.firstName)), firstName), cb.equal(cb.upper(root.get(UninsuredCustomer_.lastName)), lastName), cb.equal(cb.upper(root.get(UninsuredCustomer_.phone)), phone));
    }


}
