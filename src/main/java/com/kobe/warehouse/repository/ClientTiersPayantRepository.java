package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientTiersPayantRepository extends JpaRepository<ClientTiersPayant, Long> {
    List<ClientTiersPayant> findAllByAssuredCustomerId(Long customerId);

    List<ClientTiersPayant> findAllByAssuredCustomerIdAndTiersPayantCategorie(Long customerId, TiersPayantCategorie categorie);

    List<ClientTiersPayant> findAllByIdIn(Set<Long> ids);

    List<ClientTiersPayant> findAllByTiersPayantId(Long tiersPayantId);
}
