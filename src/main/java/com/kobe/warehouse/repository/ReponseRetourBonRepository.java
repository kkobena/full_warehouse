package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ReponseRetourBon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReponseRetourBonRepository extends JpaRepository<ReponseRetourBon, Integer> {

    @Query("SELECT r FROM ReponseRetourBon r WHERE r.retourBon.id = :retourBonId ORDER BY r.dateMtv DESC")
    List<ReponseRetourBon> findAllByRetourBonId(@Param("retourBonId") Integer retourBonId);
}
