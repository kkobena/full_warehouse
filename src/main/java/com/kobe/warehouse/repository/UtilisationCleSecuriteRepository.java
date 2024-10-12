package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.UtilisationCleSecurite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilisationCleSecuriteRepository extends JpaRepository<UtilisationCleSecurite, Long> {}
