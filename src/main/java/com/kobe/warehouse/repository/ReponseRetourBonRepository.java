package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ReponseRetourBon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReponseRetourBonRepository extends JpaRepository<ReponseRetourBon, Integer> {
    @Query("SELECT r FROM ReponseRetourBon r WHERE r.retourBon.id = :retourBonId ORDER BY r.dateMtv DESC")
    List<ReponseRetourBon> findAllByRetourBonId(@Param("retourBonId") Integer retourBonId);
}
