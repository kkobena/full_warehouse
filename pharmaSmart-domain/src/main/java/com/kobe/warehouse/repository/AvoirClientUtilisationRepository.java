package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirClientUtilisation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvoirClientUtilisationRepository extends JpaRepository<AvoirClientUtilisation, Integer> {

    List<AvoirClientUtilisation> findByAvoirClientIdOrderByUtiliseLeDesc(Integer avoirClientId);
}
