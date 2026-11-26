package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CustomerTraitementChronique;
import com.kobe.warehouse.service.dto.controle.TraitementChroniqueRefDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerTraitementChroniqueRepository extends JpaRepository<CustomerTraitementChronique, Integer> {
    @EntityGraph(attributePaths = {"dci", "produit"})
    List<CustomerTraitementChronique> findAllByCustomerIdOrderByActifDescCreatedAtAsc(Integer customerId);

    @EntityGraph(attributePaths = {"dci", "produit"})
    List<CustomerTraitementChronique> findAllByActifTrue();

    /** Traitements actifs d'un client, non échus : par produit, ou à défaut par leur seule DCI. */
    @Query("select new com.kobe.warehouse.service.dto.controle.TraitementChroniqueRefDTO(p.id, p.libelle, d.id) "
            + "from CustomerTraitementChronique t left join t.produit p left join t.dci d "
            + "where t.customerId = :customerId and t.actif = true "
            + "and (t.dateFinOrdonnance is null or t.dateFinOrdonnance >= :aujourdhui)")
    List<TraitementChroniqueRefDTO> findActifsNonEchus(
            @Param("customerId") Integer customerId, @Param("aujourdhui") LocalDate aujourdhui);
}
