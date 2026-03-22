package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SemoisClasseConfig;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemoisClasseConfigRepository extends JpaRepository<SemoisClasseConfig, ClasseCriticite> {}
