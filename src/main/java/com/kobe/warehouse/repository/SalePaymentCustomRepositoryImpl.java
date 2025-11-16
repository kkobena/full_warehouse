package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.PaymentMode_;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalePayment_;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.tiketz.dto.TicketZProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SalePaymentCustomRepositoryImpl implements SalePaymentCustomRepository {

    private final EntityManager entityManager;

    public SalePaymentCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<TicketZProjection> fetchSalesPayment(Specification<SalePayment> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TicketZProjection> query = cb.createQuery(TicketZProjection.class);
        Root<SalePayment> root = query.from(SalePayment.class);
        query
            .select(
                cb.construct(
                    TicketZProjection.class,
                    root.get(SalePayment_.paymentMode).get(PaymentMode_.code),
                    root.get(SalePayment_.paymentMode).get(PaymentMode_.libelle),
                    root.get(SalePayment_.cashRegister).get(CashRegister_.user).get(AppUser_.id),
                    root.get(SalePayment_.cashRegister).get(CashRegister_.user).get(AppUser_.firstName),
                    root.get(SalePayment_.cashRegister).get(CashRegister_.user).get(AppUser_.lastName),
                    cb.sumAsLong(root.get(SalePayment_.paidAmount)),
                    cb.sumAsLong(root.get(SalePayment_.reelAmount)),
                    root.get(SalePayment_.credit)
                )
            )
            .groupBy(
                root.get(SalePayment_.cashRegister).get(CashRegister_.user).get(AppUser_.id),
                root.get(SalePayment_.cashRegister).get(CashRegister_.user).get(AppUser_.id),
                root.get(SalePayment_.paymentMode).get(PaymentMode_.code)
            );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<TicketZProjection> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    @Override
    public List<VenteModePaimentRecord> fetchVenteModePaimentRecords(Specification<SalePayment> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VenteModePaimentRecord> query = cb.createQuery(VenteModePaimentRecord.class);
        Root<SalePayment> root = query.from(SalePayment.class);
        query
            .select(
                cb.construct(
                    VenteModePaimentRecord.class,
                    root.get(SalePayment_.paymentMode).get(PaymentMode_.code),
                    root.get(SalePayment_.paymentMode).get(PaymentMode_.libelle),
                    cb.sum(root.get(SalePayment_.reelAmount)),
                    cb.sum(root.get(SalePayment_.paidAmount))
                )
            )
            .groupBy(
                root.get(SalePayment_.paymentMode).get(PaymentMode_.libelle),
                root.get(SalePayment_.paymentMode).get(PaymentMode_.code)
            );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<VenteModePaimentRecord> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }
}
