package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.AssuredCustomer_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AssuredCustomerRepository extends JpaRepository<AssuredCustomer, Long>, JpaSpecificationExecutor<AssuredCustomer> {


    default Specification<AssuredCustomer> specialisationQueryString(String queryValue) {
        return (root, query, cb) -> cb.or(cb.like(cb.upper(root.get(AssuredCustomer_.firstName)), queryValue), cb.like(cb.upper(root.get(AssuredCustomer_.lastName)), queryValue), cb.like(cb.upper(root.get(AssuredCustomer_.code)), queryValue),
            cb.like(cb.upper(cb.concat(cb.concat(root.get(AssuredCustomer_.firstName), " "), root.get(AssuredCustomer_.lastName))), queryValue), cb.like(cb.upper(root.get(AssuredCustomer_.phone)), queryValue)
        );
    }

    default Specification<AssuredCustomer> specialisation() {
        return (root, query, cb) -> cb.equal(root.get(AssuredCustomer_.status), SalesStatut.ACTIVE);
    }

    default Specification<AssuredCustomer> specialisationCheckExist(String firstName, String lastName, String phone) {
        return (root, query, cb) -> cb.and(cb.equal(cb.upper(root.get(AssuredCustomer_.firstName)), firstName), cb.equal(cb.upper(root.get(AssuredCustomer_.lastName)), lastName), cb.equal(cb.upper(root.get(AssuredCustomer_.phone)), phone));
    }

}
