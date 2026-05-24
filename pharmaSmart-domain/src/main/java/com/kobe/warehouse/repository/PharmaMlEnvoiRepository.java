package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PharmaMlEnvoi;
import com.kobe.warehouse.domain.enumeration.PharmaMlStatut;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PharmaMlEnvoiRepository extends JpaRepository<PharmaMlEnvoi, Integer> {

    List<PharmaMlEnvoi> findByCommandeIdAndCommandeOrderDateOrderByCreatedAtDesc(
        Integer commandeId,
        LocalDate commandeDate
    );

    Optional<PharmaMlEnvoi> findTopByCommandeIdAndCommandeOrderDateOrderByCreatedAtDesc(
        Integer commandeId,
        LocalDate commandeDate
    );

    List<PharmaMlEnvoi> findByStatutOrderByCreatedAtDesc(PharmaMlStatut statut);
}
