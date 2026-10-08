package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.pharmacovigilance.Interaction;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Integer> {
    /**
     * Interactions entre molécules de l'ensemble, directes ou par classe, dans les seules versions
     * publiées. Chaque côté est testé par molécule puis par classe.
     */
    @Query(
        "select i from Interaction i " +
        "where i.versionId in (select v.id from ReferentielInteractionVersion v where v.publie = true) " +
        "and (i.aRefDciId in :dcis or i.aClasseId in (select d.id.classeId from DciClasse d where d.id.refDciId in :dcis)) " +
        "and (i.bRefDciId in :dcis or i.bClasseId in (select d.id.classeId from DciClasse d where d.id.refDciId in :dcis))"
    )
    List<Interaction> findEntre(@Param("dcis") Collection<Integer> dcis);

    long countByVersionId(Integer versionId);
}
