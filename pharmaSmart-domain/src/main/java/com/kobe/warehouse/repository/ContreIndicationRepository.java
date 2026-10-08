package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.pharmacovigilance.ContreIndication;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContreIndicationRepository extends JpaRepository<ContreIndication, Integer> {
    @Query(
        "SELECT c FROM ContreIndication c WHERE c.refDciId IN :dcis AND c.versionId IN "
        + "(SELECT v.id FROM ReferentielInteractionVersion v WHERE v.publie = TRUE)"
    )
    List<ContreIndication> findPubliees(Collection<Integer> dcis);
}
