package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThirdPartySaleLineRepository extends JpaRepository<ThirdPartySaleLine, Long> {
  long countByClientTiersPayantId(Long clientTiersPayantId);

  List<ThirdPartySaleLine> findAllBySaleId(Long saleId);

  @Query(
      value =
          "SELECT  count(o) FROM ThirdPartySaleLine o WHERE o.numBon=:numBon AND o.clientTiersPayant.id=:clientTiersPayantId AND o.sale.statut=:statut ")
  long countThirdPartySaleLineByNumBonAndClientTiersPayantId(
      @Param("numBon") String numBon,
      @Param("clientTiersPayantId") Long clientTiersPayantId,
      @Param("statut") SalesStatut statut);

  Optional<ThirdPartySaleLine> findFirstByClientTiersPayantIdAndSaleId(
      Long clientTiersPayantId, Long saleId);
}
