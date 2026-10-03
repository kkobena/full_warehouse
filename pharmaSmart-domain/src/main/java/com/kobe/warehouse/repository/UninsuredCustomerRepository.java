package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.UninsuredCustomer_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UninsuredCustomerRepository
    extends JpaRepository<UninsuredCustomer, Integer>, JpaSpecificationExecutor<UninsuredCustomer> {
    Optional<UninsuredCustomer> findOneByCode(String code);

    List<UninsuredCustomer> findAllByPhone(String phone);

    List<UninsuredCustomer> findAllByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    /**
     * Clients dont le nom complet ressemble à {@code nom} (similarité trigramme, pg_trgm) : une faute de
     * frappe ne crée plus un doublon silencieux. {@code nom} est en majuscules.
     */
    @Query(
        "select c from UninsuredCustomer c " +
        "where function('similarity', upper(concat(c.firstName, ' ', c.lastName)), :nom) >= :seuil " +
        "order by function('similarity', upper(concat(c.firstName, ' ', c.lastName)), :nom) desc"
    )
    List<UninsuredCustomer> findAllByNomProche(@Param("nom") String nom, @Param("seuil") double seuil, Pageable pageable);

    default Specification<UninsuredCustomer> specialisationQueryString(String queryValue) {
        return (root, query, cb) ->
            cb.or(
                cb.like(cb.upper(root.get(UninsuredCustomer_.firstName)), queryValue),
                cb.like(cb.upper(root.get(UninsuredCustomer_.lastName)), queryValue),
                cb.like(cb.upper(root.get(UninsuredCustomer_.code)), queryValue),
                cb.like(
                    cb.upper(cb.concat(cb.concat(root.get(UninsuredCustomer_.firstName), " "), root.get(UninsuredCustomer_.lastName))),
                    queryValue
                ),
                cb.like(cb.upper(root.get(UninsuredCustomer_.phone)), queryValue)
            );
    }

    default Specification<UninsuredCustomer> specialisation(Status status) {
        return (root, query, cb) -> cb.equal(root.get(UninsuredCustomer_.status), status);
    }

    default Specification<UninsuredCustomer> specialisationCheckExist(String firstName, String lastName, String phone) {
        return (root, query, cb) ->
            cb.and(
                cb.equal(cb.upper(root.get(UninsuredCustomer_.firstName)), firstName.toUpperCase()),
                cb.equal(cb.upper(root.get(UninsuredCustomer_.lastName)), lastName.toUpperCase()),
                cb.equal(cb.upper(root.get(UninsuredCustomer_.phone)), phone)
            );
    }
}
