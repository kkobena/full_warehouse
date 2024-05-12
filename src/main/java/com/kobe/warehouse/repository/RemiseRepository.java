package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Remise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RemiseRepository extends JpaRepository<Remise, Long> {}
