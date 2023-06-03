package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Tableau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableauRepository extends JpaRepository<Tableau, Long> {}
