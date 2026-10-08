package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ordonnance.OrdonnanceVente;
import com.kobe.warehouse.service.dto.ordonnance.VenteLieeDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdonnanceVenteRepository extends JpaRepository<OrdonnanceVente, OrdonnanceVente.Id> {
    @Query(
        "select new com.kobe.warehouse.service.dto.ordonnance.VenteLieeDTO(s.id, s.saleDate, s.statut, s.canceled) " +
        "from OrdonnanceVente ov, Sales s " +
        "where s.id = ov.id.salesId and s.saleDate = ov.id.salesDate and ov.id.ordonnanceId = :ordonnanceId " +
        "order by s.saleDate desc, s.id desc"
    )
    List<VenteLieeDTO> findVentesLiees(@Param("ordonnanceId") Integer ordonnanceId);

    /** La vente est-elle rattachée à au moins une ordonnance ? */
    @Query("select count(ov) > 0 from OrdonnanceVente ov where ov.id.salesId = :salesId and ov.id.salesDate = :salesDate")
    boolean existsParVente(@Param("salesId") Long salesId, @Param("salesDate") LocalDate salesDate);

    /** La ligne de vente appartient-elle à une vente déjà rattachée à cette ordonnance ? */
    @Query(
        "select count(sl) > 0 from SalesLine sl, OrdonnanceVente ov " +
        "where sl.id = :salesLineId and sl.saleDate = :salesLineDate and ov.id.ordonnanceId = :ordonnanceId " +
        "and ov.id.salesId = sl.sales.id and ov.id.salesDate = sl.sales.saleDate"
    )
    boolean existsLigneDeVenteRattachee(
        @Param("ordonnanceId") Integer ordonnanceId,
        @Param("salesLineId") Long salesLineId,
        @Param("salesLineDate") LocalDate salesLineDate
    );
}
