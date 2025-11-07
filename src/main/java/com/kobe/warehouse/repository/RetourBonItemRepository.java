package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourBonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("unused")
@Repository
public interface RetourBonItemRepository extends JpaRepository<RetourBonItem, Long> {
    @Query("SELECT r FROM RetourBonItem r WHERE r.retourBon.id = :retourBonId")
    List<RetourBonItem> findAllByRetourBonId(@Param("retourBonId") Long retourBonId);

    @Query("SELECT r FROM RetourBonItem r WHERE r.orderLine.id = :orderLineId")
    List<RetourBonItem> findAllByOrderLineId(@Param("orderLineId") Long orderLineId);
}
