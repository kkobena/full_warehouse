package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ImportationEchoue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the ImportationEchoue entity.
 */
@Repository
public interface ImportationEchoueRepository extends JpaRepository<ImportationEchoue, Integer> {}
