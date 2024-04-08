package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Avoir;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvoirRepository extends JpaRepository<Avoir, Long> {}
