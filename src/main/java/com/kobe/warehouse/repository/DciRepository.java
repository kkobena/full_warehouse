package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Dci;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DciRepository extends JpaRepository<Dci, Long> {
    Page<Dci> findAllByCodeContainingIgnoreCaseOrLibelleContainingIgnoreCaseOrderByLibelleAsc(
        String code,
        String libelle,
        Pageable pageable
    );
    Page<Dci> findAllByOrderByLibelleAsc(Pageable pageable);
}
