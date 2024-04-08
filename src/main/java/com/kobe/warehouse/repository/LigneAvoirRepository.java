package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LigneAvoir;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneAvoirRepository extends JpaRepository<LigneAvoir, Long> {}
