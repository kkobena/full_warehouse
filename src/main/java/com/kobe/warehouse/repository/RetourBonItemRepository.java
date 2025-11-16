package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourBonItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RetourBonItemRepository extends JpaRepository<RetourBonItem, Integer> {
    @Query("SELECT r FROM RetourBonItem r WHERE r.retourBon.id = :retourBonId")
    List<RetourBonItem> findAllByRetourBonId(@Param("retourBonId") Integer retourBonId);

    @Query("SELECT r FROM RetourBonItem r WHERE r.orderLine.id = :orderLineId")
    List<RetourBonItem> findAllByOrderLineId(@Param("orderLineId") Integer orderLineId);
}
