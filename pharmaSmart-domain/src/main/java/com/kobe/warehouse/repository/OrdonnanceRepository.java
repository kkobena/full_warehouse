package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ordonnance.Ordonnance;
import com.kobe.warehouse.domain.ordonnance.Prescripteur;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdonnanceRepository extends JpaRepository<Ordonnance, Integer> {
    List<Ordonnance> findByCustomerIdOrderByDatePrescriptionDescIdDesc(Integer customerId);

    /** Fusion de prescripteurs : les ordonnances de la fiche source passent sur la fiche conservée. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Ordonnance o set o.prescripteur = :cible where o.prescripteur = :source")
    int reaffecterPrescripteur(@Param("source") Prescripteur source, @Param("cible") Prescripteur cible);
}
