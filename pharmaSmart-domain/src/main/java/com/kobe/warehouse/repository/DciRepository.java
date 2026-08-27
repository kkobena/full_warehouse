package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Dci;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DciRepository extends JpaRepository<Dci, Integer> {
    Page<Dci> findAllByCodeContainingIgnoreCaseOrLibelleContainingIgnoreCaseOrderByLibelleAsc(
        String code,
        String libelle,
        Pageable pageable
    );

    Page<Dci> findAllByOrderByLibelleAsc(Pageable pageable);

    Optional<Dci> findOneByLibelleIgnoreCase(String libelle);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByLibelleIgnoreCase(String libelle);

    /**
     * Variantes excluant une ligne : à la modification, une DCI n'est pas en doublon avec
     * elle-même.
     */
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);

    boolean existsByLibelleIgnoreCaseAndIdNot(String libelle, Integer id);
}
