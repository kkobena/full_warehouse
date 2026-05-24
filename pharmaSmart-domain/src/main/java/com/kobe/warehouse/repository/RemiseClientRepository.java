package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RemiseClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RemiseClientRepository extends JpaRepository<RemiseClient, Integer> {}
