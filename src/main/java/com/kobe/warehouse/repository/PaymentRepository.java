package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.service.dto.projection.Recette;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllBySalesId(Long id);

    Optional<List<Payment>> findBySalesId(Long id);

    @Query(
        value = "SELECT  SUM(p.net_amount) AS montantReel,SUM(p.paid_amount) AS montantPaye,pm.libelle AS modePaimentLibelle,pm.code AS modePaimentCode FROM payment p JOIN payment_mode pm ON p.payment_mode_code = pm.code " +
        "    JOIN sales s on p.sales_id = s.id WHERE s.ca IN ('CA') AND s.statut IN('CANCELED', 'CLOSED','REMOVE') AND DATE(s.updated_at) BETWEEN :fromDate AND :toDate GROUP BY pm.code",
        nativeQuery = true
    )
    List<Recette> findRecettes(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
