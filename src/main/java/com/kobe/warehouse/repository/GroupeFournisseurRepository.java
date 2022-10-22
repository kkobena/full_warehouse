package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.GroupeFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupeFournisseurRepository extends JpaRepository<GroupeFournisseur, Long>, JpaSpecificationExecutor<GroupeFournisseur> {
    Optional<GroupeFournisseur> findOneByLibelle(String libelle);
}
