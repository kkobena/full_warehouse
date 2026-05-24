package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourClient;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kobe.warehouse.domain.RetourClient_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public interface RetourClientRepository extends JpaRepository<RetourClient, Integer>, JpaSpecificationExecutor<RetourClient> {

    @Query("SELECT COUNT(r), COALESCE(SUM(r.montantTotal), 0) FROM RetourClient r WHERE r.createdAt >= :debut AND r.createdAt < :fin")
    Object[] statsGlobales(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT r.motif, COUNT(r) FROM RetourClient r WHERE r.createdAt >= :debut AND r.createdAt < :fin GROUP BY r.motif ORDER BY COUNT(r) DESC")
    List<Object[]> statsParMotif(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("""
        SELECT rcl.produit.id, rcl.produit.libelle,
               rcl.produit.codeEanLaboratoire, COUNT(rc)
        FROM RetourClientLine rcl JOIN rcl.retourClient rc
        WHERE rc.createdAt >= :debut AND rc.createdAt < :fin
        GROUP BY rcl.produit.id, rcl.produit.libelle, rcl.produit.codeEanLaboratoire
        HAVING COUNT(rc) > :seuil
        ORDER BY COUNT(rc) DESC
        """)
    List<Object[]> produitsEnAlerte(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin, @Param("seuil") long seuil);

    @Query("""
        SELECT rc.customer.id, rc.customer.firstName, rc.customer.lastName, COUNT(rc)
        FROM RetourClient rc
        WHERE rc.createdAt >= :debut AND rc.createdAt < :fin AND rc.customer IS NOT NULL
        GROUP BY rc.customer.id, rc.customer.firstName, rc.customer.lastName
        HAVING COUNT(rc) > :seuil
        ORDER BY COUNT(rc) DESC
        """)
    List<Object[]> clientsEnAlerte(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin, @Param("seuil") long seuil);

    static Specification<RetourClient> buildSpec(String search, LocalDate fromDate, LocalDate toDate) {
        Specification<RetourClient> spec = Specification.unrestricted();
        if (fromDate != null && toDate != null) {
            spec = spec.and((root, query, cb) ->
                cb.between(root.get(RetourClient_.createdAt),
                    fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()));
        }
        if (StringUtils.hasText(search)) {
            String term = "%" + search.toUpperCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.upper(root.get(RetourClient_.reference)), term),
                cb.like(cb.upper(root.get(RetourClient_.originalSaleRef)), term)
            ));
        }
        return spec;
    }
}
