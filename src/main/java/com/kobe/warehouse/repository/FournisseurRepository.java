package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Fournisseur;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Integer>, JpaSpecificationExecutor<Fournisseur> {
    Optional<Fournisseur> findFirstByLibelleEquals(String libelle);

    List<Fournisseur> findByParentIsNullOrderByOdreAsc();

    Page<Fournisseur> findByParentIsNull(Pageable pageable);

    Page<Fournisseur> findByParentIsNull(Specification<Fournisseur> spec, Pageable pageable);

    List<Fournisseur> findByParentId(Integer parentId);

    Optional<Fournisseur> findFirstByLibelleEqualsAndParentIsNull(String libelle);
    @Query("SELECT o.parent FROM Fournisseur o WHERE o.id = ?1")
    Optional<Fournisseur> getParentByChildId(Integer childId);

    @Query("SELECT o.parent.id FROM Fournisseur o WHERE o.id = ?1")
    Optional<Integer> getParentIdByChildId(Integer childId);
}
