package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RefDci;
import com.kobe.warehouse.service.dto.controle.MoleculeDTO;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefDciRepository extends JpaRepository<RefDci, Integer> {
    /** Molécules du référentiel reliées à ces DCI du catalogue. */
    @Query("select new com.kobe.warehouse.service.dto.controle.MoleculeDTO(r.id, r.libelle) from RefDci r where r.dci.id in :dciIds")
    List<MoleculeDTO> findByDciIds(@Param("dciIds") Collection<Integer> dciIds);
}
