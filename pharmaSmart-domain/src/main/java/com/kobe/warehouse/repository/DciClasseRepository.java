package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.pharmacovigilance.DciClasse;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DciClasseRepository extends JpaRepository<DciClasse, DciClasse.Id> {
    @Query("SELECT d FROM DciClasse d WHERE d.id.refDciId IN :dcis")
    List<DciClasse> findByRefDciIds(Collection<Integer> dcis);
}
