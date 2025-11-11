package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.GroupeFournisseur;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupeFournisseurRepository extends JpaRepository<GroupeFournisseur, Integer>, JpaSpecificationExecutor<GroupeFournisseur> {
    Optional<GroupeFournisseur> findOneByLibelle(String libelle);

    List<GroupeFournisseur> findAllByOrderByOdreAsc();
}
