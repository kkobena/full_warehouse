package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourDepotItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the RetourDepotItem entity.
 */
@Repository
public interface RetourDepotItemRepository extends JpaRepository<RetourDepotItem, Integer> {
    List<RetourDepotItem> findAllByRetourDepotId(Long retourDepotId);
}
