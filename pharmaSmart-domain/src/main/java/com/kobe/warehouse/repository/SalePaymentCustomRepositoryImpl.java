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

        var paymentModeJoin = root.join(SalePayment_.paymentMode);
        var cashRegisterJoin = root.join(SalePayment_.cashRegister);
        var userJoin = cashRegisterJoin.join(CashRegister_.user);

        query
            .select(
                cb.construct(
                    TicketZProjection.class,
                    paymentModeJoin.get(PaymentMode_.code),
                    paymentModeJoin.get(PaymentMode_.libelle),
                    userJoin.get(AppUser_.id),
                    userJoin.get(AppUser_.firstName),
                    userJoin.get(AppUser_.lastName),
                    cb.sumAsLong(root.get(SalePayment_.paidAmount)),
                    cb.sumAsLong(root.get(SalePayment_.reelAmount)),
                    root.get(SalePayment_.credit)
                )
            )
            .groupBy(
                userJoin.get(AppUser_.id),
                userJoin.get(AppUser_.firstName),
                userJoin.get(AppUser_.lastName),
                paymentModeJoin.get(PaymentMode_.code),
                paymentModeJoin.get(PaymentMode_.libelle),
                root.get(SalePayment_.credit)
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

        var paymentModeJoin = root.join(SalePayment_.paymentMode);

        query
            .select(
                cb.construct(
                    VenteModePaimentRecord.class,
                    paymentModeJoin.get(PaymentMode_.code),
                    paymentModeJoin.get(PaymentMode_.libelle),
                    cb.sum(root.get(SalePayment_.reelAmount)),
                    cb.sum(root.get(SalePayment_.paidAmount))
                )
            )
            .groupBy(
                paymentModeJoin.get(PaymentMode_.code),
                paymentModeJoin.get(PaymentMode_.libelle)
            );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<VenteModePaimentRecord> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }
}
