package com.kobe.warehouse.service.facturation.specification;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ClientTiersPayant_;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine_;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

public class EditionDataSpecification {

    public static Specification<ThirdPartySaleLine> aThirdPartySaleLine(EditionSearchParams editionSearchParams) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<ThirdPartySaleLine, ThirdPartySales> salesJoin = root.join(ThirdPartySaleLine_.sale, JoinType.INNER);
            predicates.add(
                cb.between(salesJoin.get(ThirdPartySales_.saleDate), editionSearchParams.startDate(), editionSearchParams.endDate())
            );
            predicates.add(salesJoin.get(ThirdPartySales_.statut).in(Set.of(SalesStatut.CLOSED)));
            predicates.add(cb.isNull(salesJoin.get(ThirdPartySales_.canceledSale)));
            predicates.add(cb.isFalse(salesJoin.get(ThirdPartySales_.canceled)));

            Join<ThirdPartySaleLine, ClientTiersPayant> clientTiersPayantJoin = root.join(
                ThirdPartySaleLine_.clientTiersPayant,
                JoinType.INNER
            );
            Join<ClientTiersPayant, TiersPayant> tiersPayantJoin = clientTiersPayantJoin.join(
                ClientTiersPayant_.tiersPayant,
                JoinType.INNER
            );

            if (
                !CollectionUtils.isEmpty(editionSearchParams.tiersPayantIds()) && editionSearchParams.modeEdition() != ModeEditionEnum.GROUP
            ) {
                predicates.add(tiersPayantJoin.get(TiersPayant_.id).in(editionSearchParams.tiersPayantIds()));
            }

            if (!CollectionUtils.isEmpty(editionSearchParams.groupIds()) && editionSearchParams.modeEdition() == ModeEditionEnum.GROUP) {
                predicates.add(
                    tiersPayantJoin.get(TiersPayant_.groupeTiersPayant).get(GroupeTiersPayant_.id).in(editionSearchParams.groupIds())
                );
            }

            if (editionSearchParams.factureProvisoire()) {
                predicates.add(cb.isNull(root.get(ThirdPartySaleLine_.factureTiersPayant)));
            } else {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<FactureTiersPayant> subRoot = subquery.from(FactureTiersPayant.class);
                subquery.select(subRoot.get(FactureTiersPayant_.id));
                subquery.where(cb.isTrue(subRoot.get(FactureTiersPayant_.factureProvisoire)));
                predicates.add(
                    cb.or(
                        cb.isNull(root.get(ThirdPartySaleLine_.factureTiersPayant)),
                        root.get(ThirdPartySaleLine_.factureTiersPayant).get(FactureTiersPayant_.id).in(subquery)
                    )
                );
            }

            if (!CollectionUtils.isEmpty(editionSearchParams.categorieTiersPayants())) {
                predicates.add(tiersPayantJoin.get(TiersPayant_.categorie).in(editionSearchParams.categorieTiersPayants()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
