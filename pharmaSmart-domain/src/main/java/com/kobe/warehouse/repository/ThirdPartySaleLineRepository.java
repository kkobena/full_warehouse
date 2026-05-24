package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AssuranceSaleId;
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
import org.springframework.util.StringUtils;

@Repository
public interface ThirdPartySaleLineRepository
    extends
        JpaRepository<ThirdPartySaleLine, AssuranceSaleId>,
        JpaSpecificationExecutor<ThirdPartySaleLine>,
        ThirdPartySaleLineCustomRepository {
    long countByClientTiersPayantId(Integer clientTiersPayantId);

    List<ThirdPartySaleLine> findAllBySaleIdAndSaleSaleDate(Long saleId, LocalDate saleDate);

    @Query(
        value = "SELECT  count(o) FROM ThirdPartySaleLine o WHERE o.numBon=:numBon AND o.clientTiersPayant.id=:clientTiersPayantId AND o.sale.statut=:statut AND o.saleDate>=:startOfDay  "
    )
    long countThirdPartySaleLineByNumBonAndClientTiersPayantId(
        @Param("numBon") String numBon,
        @Param("clientTiersPayantId") Integer clientTiersPayantId,
        @Param("statut") SalesStatut statut,
        @Param("startOfDay") LocalDate startOfDay
    );

    @Query(
        value = "SELECT  count(o) FROM ThirdPartySaleLine o WHERE o.numBon=:numBon AND o.clientTiersPayant.id=:clientTiersPayantId AND o.sale.statut=:statut AND o.sale.id <>:saleId AND o.saleDate>=:startOfDay "
    )
    long countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
        @Param("numBon") String numBon,
        @Param("clientTiersPayantId") Long saleId,
        @Param("saleId") Integer clientTiersPayantId,
        @Param("statut") SalesStatut statut,
        @Param("startOfDay") LocalDate startOfDay
    );

    @Query(
        "SELECT o FROM ThirdPartySaleLine o WHERE o.clientTiersPayant.id=:clientTiersPayantId AND o.sale.id=:saleId AND o.saleDate=:saleDate"
    )
    Optional<ThirdPartySaleLine> findFirstByClientTiersPayantIdAndSaleIdAndSaleSaleDate(
        Integer clientTiersPayantId,
        Long saleId,
        LocalDate saleDate
    );

    default Specification<ThirdPartySaleLine> periodeCriteria(LocalDate startDate, LocalDate endDate) {
        return (root, _, cb) -> cb.between(root.get(ThirdPartySaleLine_.sale).get(ThirdPartySales_.saleDate), startDate, endDate);
    }

    default Specification<ThirdPartySaleLine> selectionBonCriteria(Set<Long> ids) {
        return (root, _, cb) -> {
            In<Long> selectionIds = cb.in(root.get(ThirdPartySaleLine_.id));
            ids.forEach(selectionIds::value);
            return selectionIds;
        };
    }

    default Specification<ThirdPartySaleLine> tiersPayantIdsCriteria(Set<Integer> tiersPayantIds) {
        return (root, _, cb) -> {
            In<Integer> selectionIds = cb.in(
                root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.id)
            );
            tiersPayantIds.forEach(selectionIds::value);
            return selectionIds;
        };
    }

    default Specification<ThirdPartySaleLine> groupIdsCriteria(Set<Integer> groupIds) {
        return (root, _, cb) -> {
            In<Integer> selectionIds = cb.in(
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

    default Specification<ThirdPartySaleLine> filterBySearchTerm(String searchTerm) {
        if (StringUtils.hasLength(searchTerm)) {
            return (root, _, cb) -> {
                String likePattern = "%" + searchTerm.toLowerCase() + "%";
                return cb.or(
                    cb.like(
                        cb.lower(
                            root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.name)
                        ),
                        likePattern
                    ),
                    cb.like(
                        cb.lower(
                            root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.fullName)
                        ),
                        likePattern
                    )
                );
            };
        }
        return (root, _, cb) -> cb.conjunction();
    }
}
