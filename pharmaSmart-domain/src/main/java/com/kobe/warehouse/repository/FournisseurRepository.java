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
import org.springframework.data.repository.query.Param;

public interface FournisseurRepository
        extends JpaRepository<Fournisseur, Integer>, JpaSpecificationExecutor<Fournisseur> {
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

    /**
     * Identifiant du grossiste principal dont depend ce fournisseur, ou le sien s'il en est un.
     *
     * <p>Les paliers de remise, les delais de livraison et les codes produits vivent sur le
     * principal ; une agence n'en porte generalement aucun. Resoudre le groupe est donc le
     * prealable a toute lecture de ces donnees a partir d'une commande.
     */
    @Query("SELECT COALESCE(p.id, f.id) FROM Fournisseur f LEFT JOIN f.parent p WHERE f.id = :fournisseurId")
    Optional<Integer> lireIdDuGroupe(@Param("fournisseurId") Integer fournisseurId);
}
