package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.RemiseProduit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RemiseProduitRepository extends JpaRepository<RemiseProduit, Long> {
    Optional<RemiseProduit> findFirstByRemiseValueEquals(Float remiseValue);
}

