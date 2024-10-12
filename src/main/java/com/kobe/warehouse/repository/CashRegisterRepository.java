package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.User_;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterTransactionSpecialisation;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterVenteSpecialisation;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long>, JpaSpecificationExecutor<CashRegister> {
    List<CashRegister> findOneByUserIdAndStatut(Long id, CashRegisterStatut statut);

    @Query("SELECT o FROM CashRegister  o WHERE  o.user.id=:userId AND o.statut=:statut AND o.beginTime BETWEEN :beginDate  AND :toDay ")
    List<CashRegister> findOneByUserIdAndStatutAndAndBeginTime(
        @Param("userId") Long id,
        @Param("statut") CashRegisterStatut statut,
        @Param("beginDate") LocalDateTime beginDate,
        @Param("toDay") LocalDateTime now
    );

    @Query(
        value = "SELECT SUM(p.paid_amount) as paidAmount,p.payment_mode_code as paymentModeCode,md.libelle as paymentModeLibelle,s.dtype AS typeVente FROM  sales s join payment p ON s.id = p.sales_id JOIN payment_mode md ON p.payment_mode_code = md.code WHERE s.ca IN(:categorieChiffreAffaires) AND s.cash_register_id=:cashRegisterId AND s.statut IN(:statuts) GROUP BY p.payment_mode_code,md.libelle",
        nativeQuery = true
    )
    List<CashRegisterVenteSpecialisation> findCashRegisterSalesDataById(
        @Param("cashRegisterId") Long cashRegisterId,
        @Param("categorieChiffreAffaires") Set<String> categorieChiffreAffaires,
        @Param("statuts") Set<String> statuts
    );

    @Query(
        value = "SELECT SUM(p.amount) as paidAmount,p.payment_mode_code as paymentModeCode,md.libelle as paymentModeLibelle,p.type_transaction  AS typeTransaction FROM payment_transaction p JOIN cash_register cr on cr.id = p.cash_register_id" +
        " JOIN payment_mode md ON p.payment_mode_code = md.code  WHERE cr.id=:cashRegisterId AND  p.categorie_ca IN (:categorieChiffreAffaires) GROUP BY p.payment_mode_code,md.libelle",
        nativeQuery = true
    )
    List<CashRegisterTransactionSpecialisation> findCashRegisterMvtDataById(
        @Param("cashRegisterId") Long cashRegisterId,
        @Param("categorieChiffreAffaires") Set<Integer> categorieChiffreAffaires
    );

    default Specification<CashRegister> specialisation(Long userId) {
        return (root, query, cb) -> cb.equal(root.get(CashRegister_.user).get(User_.id), userId);
    }

    default Specification<CashRegister> specialisation(Set<CashRegisterStatut> statuts) {
        return (root, query, cb) -> {
            In<CashRegisterStatut> cashRegisterIn = cb.in(root.get(CashRegister_.statut));
            statuts.forEach(cashRegisterIn::value);
            return cashRegisterIn;
        };
    }

    default Specification<CashRegister> specialisation(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, cb) -> cb.between(root.get(CashRegister_.created), fromDate, toDate);
    }

    default Specification<CashRegister> specialisationBeginTime(LocalDateTime beginTime) {
        return (root, query, cb) -> cb.equal(root.get(CashRegister_.beginTime), beginTime);
    }

    default Specification<CashRegister> specialisationEndTime(LocalDateTime endTime) {
        return (root, query, cb) -> cb.equal(root.get(CashRegister_.endTime), endTime);
    }
}
