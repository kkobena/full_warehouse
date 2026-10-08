package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.pharmacovigilance.ClasseInteraction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClasseInteractionRepository extends JpaRepository<ClasseInteraction, Integer> {
    Optional<ClasseInteraction> findByVersionIdAndLibelle(Integer versionId, String libelle);
}
