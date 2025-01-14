package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.enumeration.CodeGrilleRemise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data  repository for the GrilleRemise entity.
 */
@SuppressWarnings("unused")
@Repository
public interface GrilleRemiseRepository extends JpaRepository<GrilleRemise, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM GrilleRemise g WHERE g.remiseProduit.id=:remiseId")
    void deleteByRemiseProduitId(@Param("remiseId") Long remiseId);

    List<GrilleRemise> findAllByCodeIn(List<CodeGrilleRemise> codes);
}
