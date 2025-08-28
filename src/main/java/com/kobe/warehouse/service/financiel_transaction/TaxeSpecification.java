package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Component
public class TaxeSpecification {

    public Specification<SalesLine> builder(MvtParam mvtParam) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(cb.function("DATE", java.time.LocalDate.class, root.get(SalesLine_.sales).get(Sales_.updatedAt)), mvtParam.getFromDate(), mvtParam.getToDate()));
            predicates.add(   root.get(SalesLine_.sales).get(Sales_.statut).in(mvtParam.getStatuts()));
            if (nonNull(mvtParam.getToIgnore())) {
                predicates.add(cb.equal(root.get(SalesLine_.toIgnore), mvtParam.getToIgnore()));
            }
            predicates.add(root.get(SalesLine_.sales).get(Sales_.type).in(mvtParam.getTypeVentes().stream().map(com.kobe.warehouse.service.cash_register.dto.TypeVente::getValue).toList()));
            predicates.add(root.get(SalesLine_.sales).get(Sales_.categorieChiffreAffaire).in(mvtParam.getCategorieChiffreAffaires()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
