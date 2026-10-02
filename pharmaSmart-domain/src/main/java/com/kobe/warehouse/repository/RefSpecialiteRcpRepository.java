package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RefSpecialiteRcp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSpecialiteRcpRepository extends JpaRepository<RefSpecialiteRcp, String> {}
