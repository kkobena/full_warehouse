package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TableauPharmacienSpecification {
    private final SaleSpecification saleSpecification;

    public TableauPharmacienSpecification(SaleSpecification saleSpecification) {
        this.saleSpecification = saleSpecification;
    }

    public Specification<Sales> buildSalesSpecification(MvtParam mvtParam) {
        return saleSpecification.builder(mvtParam);
    }

    public Specification<Commande> buildAchatSpecification(MvtParam mvtParam) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get(Commande_.updatedAt), mvtParam.getFromDate().atStartOfDay(), mvtParam.getToDate().atTime(23, 59, 59)));
            predicates.add(cb.equal(root.get(Commande_.orderStatus), OrderStatut.CLOSED));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
