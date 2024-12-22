package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Banque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BanqueRepository extends JpaRepository<Banque, Long> {}
