package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.MotifRetourProduit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface MotifRetourProduitRepository extends JpaRepository<MotifRetourProduit, Long> {
    Page<MotifRetourProduit> findAllByLibelleContainingIgnoreCaseOrderByLibelleAsc(String libelle, Pageable pageable);
}
