package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Customer entity.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findOneByCode(String code);

    default Specification<Customer> specialisationQueryString(String queryValue) {
        return (root, query, cb) ->
            cb.or(
                cb.like(cb.upper(root.get(Customer_.firstName)), queryValue),
                cb.like(cb.upper(root.get(Customer_.lastName)), queryValue),
                cb.like(cb.upper(root.get(Customer_.code)), queryValue),
                cb.like(cb.upper(cb.concat(cb.concat(root.get(Customer_.firstName), " "), root.get(Customer_.lastName))), queryValue),
                cb.like(cb.upper(root.get(Customer_.phone)), queryValue)
            );
    }

    default Specification<Customer> specialisation(Status status) {
        return (root, query, cb) ->
            cb.and(cb.equal(root.get(Customer_.status), status), cb.equal(root.get(Customer_.typeAssure), TypeAssure.PRINCIPAL));
    }

    default Specification<Customer> specialisationCheckExist(String firstName, String lastName, String phone) {
        return (root, query, cb) ->
            cb.and(
                cb.equal(cb.upper(root.get(Customer_.firstName)), firstName),
                cb.equal(cb.upper(root.get(Customer_.lastName)), lastName),
                cb.equal(cb.upper(root.get(Customer_.phone)), phone)
            );
    }

    default Specification<Customer> specialisationDesabled() {
        return (root, query, cb) -> cb.equal(root.get(Customer_.status), Status.DISABLE);
    }
}
