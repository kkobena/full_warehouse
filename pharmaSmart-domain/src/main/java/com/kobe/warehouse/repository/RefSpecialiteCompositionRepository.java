package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RefSpecialiteComposition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSpecialiteCompositionRepository extends JpaRepository<RefSpecialiteComposition, Integer> {
    @Query(
        "select c from RefSpecialiteComposition c join fetch c.substance left join fetch c.dci " +
        "where c.specialite.cis = :cis order by c.nature desc, c.id"
    )
    List<RefSpecialiteComposition> findByCis(@Param("cis") String cis);
}
