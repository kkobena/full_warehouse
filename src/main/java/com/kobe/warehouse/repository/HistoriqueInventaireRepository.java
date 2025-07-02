package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.HistoriqueInventaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoriqueInventaireRepository extends JpaRepository<HistoriqueInventaire, Long>{
    Page<HistoriqueInventaire> findAllByOrderByCreatedDesc(Pageable pageable);
}
