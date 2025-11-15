package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourDepotItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for the RetourDepotItem entity.
 */
@Repository
public interface RetourDepotItemRepository extends JpaRepository<RetourDepotItem, Integer> {

    List<RetourDepotItem> findAllByRetourDepotId(Long retourDepotId);
}
