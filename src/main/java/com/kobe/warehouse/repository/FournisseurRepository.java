package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Fournisseur;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long>, JpaSpecificationExecutor<Fournisseur> {
    Optional<Fournisseur> findFirstByLibelleEquals(String libelle);
}
