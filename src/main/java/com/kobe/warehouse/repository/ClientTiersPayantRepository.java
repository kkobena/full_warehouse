package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClientTiersPayant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientTiersPayantRepository extends JpaRepository<ClientTiersPayant, Long> {
    List<ClientTiersPayant> findAllByAssuredCustomerId(Long customerId);
}
