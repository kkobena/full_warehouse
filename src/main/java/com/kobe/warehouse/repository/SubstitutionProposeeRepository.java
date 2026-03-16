package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SubstitutionProposee;
import com.kobe.warehouse.domain.enumeration.SubstitutionStatut;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubstitutionProposeeRepository extends JpaRepository<SubstitutionProposee, Integer> {

    List<SubstitutionProposee> findByCommandeIdAndCommandeOrderDateOrderByCreatedAtDesc(
        Integer commandeId,
        LocalDate commandeDate
    );

    List<SubstitutionProposee> findByCommandeIdAndCommandeOrderDateAndStatutOrderByCreatedAtDesc(
        Integer commandeId,
        LocalDate commandeDate,
        SubstitutionStatut statut
    );
}
