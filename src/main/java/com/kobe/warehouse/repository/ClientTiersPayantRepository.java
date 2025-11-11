package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientTiersPayantRepository extends JpaRepository<ClientTiersPayant, Integer> {
    List<ClientTiersPayant> findAllByAssuredCustomerId(Integer customerId);

    List<ClientTiersPayant> findAllByAssuredCustomerIdAndTiersPayantCategorie(Integer customerId, TiersPayantCategorie categorie);

    List<ClientTiersPayant> findAllByIdIn(Set<Integer> ids);

    List<ClientTiersPayant> findAllByTiersPayantId(Integer tiersPayantId);
}
