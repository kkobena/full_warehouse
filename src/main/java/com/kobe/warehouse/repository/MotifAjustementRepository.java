package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.MotifAjustement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface MotifAjustementRepository extends JpaRepository<MotifAjustement, Long> {
    Page<MotifAjustement> findAllByLibelleContainingIgnoreCaseOrderByLibelleAsc(String libelle, Pageable pageable);
}
