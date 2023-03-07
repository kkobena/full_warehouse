package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.WarehouseSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseSequenceRepository extends JpaRepository<WarehouseSequence, String> {}
