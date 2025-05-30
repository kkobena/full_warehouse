package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.TiersPayantPrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TiersPayantPrixRepository extends JpaRepository<TiersPayantPrix, Long> {}
