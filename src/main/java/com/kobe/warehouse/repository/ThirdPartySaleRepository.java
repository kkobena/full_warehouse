package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ThirdPartySaleRepository
    extends JpaRepository<ThirdPartySales, SaleId>, JpaSpecificationExecutor<ThirdPartySales>, ThirdPartySaleCustomRepository {
    @Query("select sale from ThirdPartySales sale left join fetch sale.salesLines where sale.id =:id and sale.saleDate =:saleDate")
    Optional<ThirdPartySales> findOneWithEagerSalesLines(@Param("id") Long id, @Param("saleDate") LocalDate saleDate);

    ThirdPartySales findOneById(Long id);

    default Specification<ThirdPartySales> filterByCaissierId(Set<Long> caissierIds) {
        if (caissierIds == null || caissierIds.isEmpty()) {
            return null; // No filter applied
        }
        return (root, query, cb) -> root.get(ThirdPartySales_.caissier).get(AppUser_.id).in(caissierIds);
    }

    default Specification<ThirdPartySales> filterByPeriode(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, _, cb) -> cb.between(root.get(ThirdPartySales_.updatedAt), fromDate, toDate);
    }
}
