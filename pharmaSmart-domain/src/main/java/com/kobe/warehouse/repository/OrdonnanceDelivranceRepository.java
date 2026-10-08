package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.ordonnance.OrdonnanceDelivrance;
import com.kobe.warehouse.service.dto.ordonnance.QuantiteDelivreeDTO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdonnanceDelivranceRepository extends JpaRepository<OrdonnanceDelivrance, Integer> {
    /**
     * Quantité délivrée par ligne : seules comptent les ventes dans le statut donné (CLOSED) et non
     * annulées. Une vente abandonnée, en attente ou annulée ne consomme donc pas l'ordonnance.
     */
    @Query(
        "select new com.kobe.warehouse.service.dto.ordonnance.QuantiteDelivreeDTO(d.ligneId, sum(sl.quantitySold)) " +
        "from OrdonnanceDelivrance d, SalesLine sl join sl.sales s " +
        "where sl.id = d.salesLineId and sl.saleDate = d.salesLineDate " +
        "and s.statut = :statut and s.canceled = false and d.ligneId in :ligneIds " +
        "group by d.ligneId"
    )
    List<QuantiteDelivreeDTO> findQuantitesDelivrees(@Param("ligneIds") Collection<Integer> ligneIds, @Param("statut") SalesStatut statut);

    /**
     * Quantité retournée par ligne d'ordonnance : retours clients validés portant sur les lignes de
     * vente liées. Un retour rend la quantité à l'ordonnance, quel que soit l'état de la vente.
     */
    @Query(
        "select new com.kobe.warehouse.service.dto.ordonnance.QuantiteDelivreeDTO(d.ligneId, sum(r.quantite)) " +
        "from OrdonnanceDelivrance d, RetourClientLine r " +
        "where r.originalSalesLineId = d.salesLineId and r.originalSalesLineDate = d.salesLineDate " +
        "and r.retourClient.validatedAt is not null and d.ligneId in :ligneIds " +
        "group by d.ligneId"
    )
    List<QuantiteDelivreeDTO> findQuantitesRetournees(@Param("ligneIds") Collection<Integer> ligneIds);

    boolean existsByLigneIdInAndSalesLineIdAndSalesLineDate(Collection<Integer> ligneIds, Long salesLineId, LocalDate salesLineDate);

    boolean existsByLigneIdAndSalesLineIdAndSalesLineDate(Integer ligneId, Long salesLineId, LocalDate salesLineDate);

    /** Supprime les délivrances que portaient les lignes de cette vente sur cette ordonnance. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "delete from OrdonnanceDelivrance d where d.ligneId in " +
        "(select l.id from OrdonnanceLigne l where l.ordonnance.id = :ordonnanceId) " +
        "and exists (select sl.id from SalesLine sl where sl.id = d.salesLineId and sl.saleDate = d.salesLineDate " +
        "and sl.sales.id = :salesId and sl.sales.saleDate = :salesDate)"
    )
    int supprimerDeLaVente(
        @Param("ordonnanceId") Integer ordonnanceId,
        @Param("salesId") Long salesId,
        @Param("salesDate") LocalDate salesDate
    );
}
