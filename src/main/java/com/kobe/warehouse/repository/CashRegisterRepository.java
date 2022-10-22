package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {
    List<CashRegister> findOneByUserIdAndStatut(Long id, CashRegisterStatut statut);

    @Query("SELECT o FROM CashRegister  o WHERE  o.user.id=:userId AND o.statut=:statut AND o.beginTime BETWEEN :beginDate  AND :toDay ")
    List<CashRegister> findOneByUserIdAndStatutAndAndBeginTime(@Param("userId") Long id, @Param("statut") CashRegisterStatut statut,  @Param("beginDate") LocalDateTime beginDate, @Param("toDay") LocalDateTime now);
}
