package com.kobe.warehouse.service.financiel_transaction;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.SetJoin;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SaleSpecification {

    public Specification<Sales> builder(MvtParam mvtParam) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(
                cb.between(
                    cb.function("DATE", java.time.LocalDate.class, root.get(Sales_.updatedAt)),
                    mvtParam.getFromDate(),
                    mvtParam.getToDate()
                )
            );
            predicates.add(root.get(Sales_.statut).in(mvtParam.getStatuts()));
            if (nonNull(mvtParam.getToIgnore())) {
                SetJoin<Sales, SalesLine> salesLineSetJoin = root.joinSet(Sales_.SALES_LINES);
                predicates.add(cb.equal(salesLineSetJoin.get(SalesLine_.toIgnore), mvtParam.getToIgnore()));
            }
            predicates.add(
                root
                    .get(Sales_.type)
                    .in(mvtParam.getTypeVentes().stream().map(com.kobe.warehouse.service.cash_register.dto.TypeVente::getValue).toList())
            );
            predicates.add(root.get(Sales_.categorieChiffreAffaire).in(mvtParam.getCategorieChiffreAffaires()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
