package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.pharmacovigilance.ReferentielInteractionVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferentielInteractionVersionRepository extends JpaRepository<ReferentielInteractionVersion, Integer> {
    boolean existsBySourceAndVersion(String source, String version);

    /** Versions relues, seules utilisées par le contrôle. */
    List<ReferentielInteractionVersion> findAllByPublieTrueOrderByPublieLeDesc();
}
