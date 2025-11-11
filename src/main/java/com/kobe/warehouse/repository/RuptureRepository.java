package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rupture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuptureRepository extends JpaRepository<Rupture, Integer> {}
