package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClientTiersPayant_;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine_;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.dto.projection.AchatTiersPayant;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ThirdPartySaleLineRepository
    extends JpaRepository<ThirdPartySaleLine, Long>, JpaSpecificationExecutor<ThirdPartySaleLine>, ThirdPartySaleLineCustomRepository {
    long countByClientTiersPayantId(Long clientTiersPayantId);

    List<ThirdPartySaleLine> findAllBySaleId(Long saleId);

    @Query(
        value = "SELECT  count(o) FROM ThirdPartySaleLine o WHERE o.numBon=:numBon AND o.clientTiersPayant.id=:clientTiersPayantId AND o.sale.statut=:statut "
    )
    long countThirdPartySaleLineByNumBonAndClientTiersPayantId(
        @Param("numBon") String numBon,
        @Param("clientTiersPayantId") Long clientTiersPayantId,
        @Param("statut") SalesStatut statut
    );

    @Query(
        value = "SELECT  count(o) FROM ThirdPartySaleLine o WHERE o.numBon=:numBon AND o.clientTiersPayant.id=:clientTiersPayantId AND o.sale.statut=:statut AND o.sale.id <>:saleId "
    )
    long countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
        @Param("numBon") String numBon,
        @Param("clientTiersPayantId") Long saleId,
        @Param("saleId") Long clientTiersPayantId,
        @Param("statut") SalesStatut statut
    );

    @Query(
        value = "SELECT  SUM(s.montant)-SUM(s.montant_regle) AS montantAttendu FROM third_party_sale_line s WHERE s.facture_tiers_payant_id=:factureTiersPayantId",
        nativeQuery = true
    )
    long sumMontantAttenduByFactureTiersPayantId(@Param("factureTiersPayantId") Long factureTiersPayantId);

    @Query(
        value = "SELECT  SUM(s.montant)-SUM(s.montant_regle) AS montantAttendu FROM third_party_sale_line s JOIN facture_tiers_payant f ON s.facture_tiers_payant_id = f.id  WHERE f.groupe_tiers_payant_id =:factureTiersPayantId",
        nativeQuery = true
    )
    long sumMontantAttenduGroupeFacture(@Param("factureTiersPayantId") Long factureTiersPayantId);

    Optional<ThirdPartySaleLine> findFirstByClientTiersPayantIdAndSaleId(Long clientTiersPayantId, Long saleId);

    default Specification<ThirdPartySaleLine> periodeCriteria(LocalDate startDate, LocalDate endDate) {
        return (root, _, cb) ->
            cb.between(
                cb.function("DATE", LocalDate.class, root.get(ThirdPartySaleLine_.sale).get(ThirdPartySales_.updatedAt)),
                cb.literal(startDate),
                cb.literal(endDate)
            );
    }

    default Specification<ThirdPartySaleLine> selectionBonCriteria(Set<Long> ids) {
        return (root, _, cb) -> {
            In<Long> selectionIds = cb.in(root.get(ThirdPartySaleLine_.id));
            ids.forEach(selectionIds::value);
            return selectionIds;
        };
    }

    default Specification<ThirdPartySaleLine> tiersPayantIdsCriteria(Set<Long> tiersPayantIds) {
        return (root, _, cb) -> {
            In<Long> selectionIds = cb.in(
                root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.id)
            );
            tiersPayantIds.forEach(selectionIds::value);
            return selectionIds;
        };
    }

    default Specification<ThirdPartySaleLine> groupIdsCriteria(Set<Long> groupIds) {
        return (root, _, cb) -> {
            In<Long> selectionIds = cb.in(
                root
                    .get(ThirdPartySaleLine_.clientTiersPayant)
                    .get(ClientTiersPayant_.tiersPayant)
                    .get(TiersPayant_.groupeTiersPayant)
                    .get(GroupeTiersPayant_.id)
            );
            groupIds.forEach(selectionIds::value);
            return selectionIds;
        };
    }

    default Specification<ThirdPartySaleLine> saleStatutsCriteria(Set<SalesStatut> salesStatuts) {
        return (root, _, cb) -> {
            In<SalesStatut> salesStatutIn = cb.in(root.get(ThirdPartySaleLine_.sale).get(ThirdPartySales_.statut));
            salesStatuts.forEach(salesStatutIn::value);
            return salesStatutIn;
        };
    }

    default Specification<ThirdPartySaleLine> canceledCriteria() {
        return (root, _, cb) ->
            cb.and(
                cb.isNull(root.get(ThirdPartySaleLine_.sale).get(ThirdPartySales_.canceledSale)),
                cb.isFalse(root.get(ThirdPartySaleLine_.sale).get(ThirdPartySales_.canceled))
            );
    }

    default Specification<ThirdPartySaleLine> notBilledCriteria() {
        return (root, _, cb) -> {
            Join<ThirdPartySaleLine, FactureTiersPayant> factureTiersPayantJoin = root.join(
                ThirdPartySaleLine_.factureTiersPayant,
                JoinType.LEFT
            );
            return cb.or(cb.isNull(factureTiersPayantJoin), cb.isTrue(factureTiersPayantJoin.get(FactureTiersPayant_.factureProvisoire)));
        };
    }

    default Specification<ThirdPartySaleLine> factureProvisoireCriteria() {
        return (root, _, cb) -> {
            Join<ThirdPartySaleLine, FactureTiersPayant> factureTiersPayantJoin = root.join(
                ThirdPartySaleLine_.factureTiersPayant,
                JoinType.LEFT
            );
            return cb.isNull(factureTiersPayantJoin);
        };
    }

    default Specification<ThirdPartySaleLine> categorieTiersPayantCriteria(Set<TiersPayantCategorie> categorieTiersPayants) {
        return (root, _, cb) -> {
            In<TiersPayantCategorie> tiersPayantCategorieIn = cb.in(
                root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.categorie)
            );
            categorieTiersPayants.forEach(tiersPayantCategorieIn::value);
            return tiersPayantCategorieIn;
        };
    }

    @Query(
        value = "SELECT o.clientTiersPayant.tiersPayant.fullName AS libelle,o.clientTiersPayant.tiersPayant.categorie AS categorie,COUNT(o) AS bonsCount,SUM(o.montant) AS montant,COUNT( DISTINCT o.clientTiersPayant.id)  AS clientCount FROM ThirdPartySaleLine o WHERE FUNCTION('DATE',o.sale.updatedAt)  BETWEEN :fromDate AND :toDate AND o.sale.statut=:statut AND (o.clientTiersPayant.tiersPayant.name like %:search% or o.clientTiersPayant.tiersPayant.fullName like %:search% ) GROUP BY o.clientTiersPayant.tiersPayant.id",
        countQuery = "SELECT COUNT(DISTINCT o.clientTiersPayant.id) FROM ThirdPartySaleLine o WHERE FUNCTION('DATE',o.sale.updatedAt)  BETWEEN :fromDate AND :toDate AND o.sale.statut=:statut AND (o.clientTiersPayant.tiersPayant.name like %:search% or o.clientTiersPayant.tiersPayant.fullName like %:search% )"
    )
    Page<AchatTiersPayant> fetchAchatTiersPayant(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("search") String search,
        @Param("statut") SalesStatut Statut,
        Pageable pageable
    );
}
