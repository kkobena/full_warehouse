package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DateDimension;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the DateDimension entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DateDimensionRepository extends JpaRepository<DateDimension, Integer> {
}
