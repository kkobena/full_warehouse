package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.UninsuredCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UninsuredCustomerRepository extends JpaRepository<UninsuredCustomer, Long> {
    Optional<UninsuredCustomer> findOneByCode(String code);
}
