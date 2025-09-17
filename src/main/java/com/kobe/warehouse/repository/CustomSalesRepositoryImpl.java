package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.PaymentMode_;
import com.kobe.warehouse.domain.PaymentTransaction_;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalePayment_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.enumeration.StatGroupBy;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.financiel_transaction.SaleSpecification;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.reglement.differe.dto.Differe;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.Solde;
import com.kobe.warehouse.service.tiketz.dto.TicketZCreditProjection;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Repository
@Transactional(readOnly = true)
public class CustomSalesRepositoryImpl implements CustomSalesRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CustomSalesRepositoryImpl.class);
    private final EntityManager entityManager;
    private final SaleSpecification saleSpecification;

    public CustomSalesRepositoryImpl(EntityManager entityManager, SaleSpecification saleSpecification) {
        this.entityManager = entityManager;
        this.saleSpecification = saleSpecification;
    }

    @Override
    public Page<DiffereItem> getDiffereItems(Specification<Sales> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DiffereItem> query = cb.createQuery(DiffereItem.class);
        Root<Sales> root = query.from(Sales.class);

        query
            .select(
                cb.construct(
                    DiffereItem.class,
                    root.get(Sales_.cashRegister).get(CashRegister_.user).get(AppUser_.firstName),
                    root.get(Sales_.cashRegister).get(CashRegister_.user).get(AppUser_.lastName),
                    root.get(Sales_.numberTransaction),
                    root.get(Sales_.salesAmount),
                    root.get(Sales_.payrollAmount),
                    root.get(Sales_.restToPay),
                    root.get(Sales_.updatedAt),
                    root.get(Sales_.id),
                    root.get(Sales_.customer).get(Customer_.id)
                )
            )
            .orderBy(cb.asc(root.get(Sales_.effectiveUpdateDate)));

        Predicate predicate = specification.toPredicate(root, query, cb);
        if (nonNull(predicate)) {
            query.where(predicate);
        }

        TypedQuery<DiffereItem> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        List<DiffereItem> results = typedQuery.getResultList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(results);
        }

        return new PageImpl<>(results, pageable, count(specification));
    }

    @Override
    public Page<Differe> getDiffere(Specification<Sales> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Differe> query = cb.createQuery(Differe.class);
        Root<Sales> root = query.from(Sales.class);

        query
            .select(
                cb.construct(
                    Differe.class,
                    root.get(Sales_.customer).get(Customer_.id),
                    root.get(Sales_.customer).get(Customer_.firstName),
                    root.get(Sales_.customer).get(Customer_.lastName),
                    cb.sumAsLong(root.get(Sales_.salesAmount)),
                    cb.sumAsLong(root.get(Sales_.payrollAmount)),
                    cb.sumAsLong(root.get(Sales_.restToPay))
                )
            )
            .groupBy(root.get(Sales_.customer).get(Customer_.id))
            .orderBy(
                cb.desc(root.get(Sales_.customer).get(Customer_.firstName)),
                cb.desc(root.get(Sales_.customer).get(Customer_.lastName))
            );

        if (nonNull(specification)) {
            Predicate predicate = specification.toPredicate(root, query, cb);
            query.where(predicate);
        }

        TypedQuery<Differe> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        List<Differe> results = typedQuery.getResultList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(results);
        }

        return new PageImpl<>(results, pageable, countDifferes(specification));
    }

    private Long countDifferes(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Sales> root = countQuery.from(Sales.class);
        countQuery.select(cb.countDistinct(root.get(Sales_.customer)));

        if (nonNull(specification)) {
            Predicate predicate = specification.toPredicate(root, countQuery, cb);
            countQuery.where(predicate);
        }
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private Long count(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Sales> root = countQuery.from(Sales.class);
        countQuery.select(cb.count(root));

        if (nonNull(specification)) {
            Predicate predicate = specification.toPredicate(root, countQuery, cb);
            countQuery.where(predicate);
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    @Override
    public DiffereSummary getDiffereSummary(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DiffereSummary> query = cb.createQuery(DiffereSummary.class);
        Root<Sales> root = query.from(Sales.class);
        query.select(
            cb.construct(
                DiffereSummary.class,
                cb.sumAsLong(root.get(Sales_.salesAmount)),
                cb.sumAsLong(root.get(Sales_.payrollAmount)),
                cb.sumAsLong(root.get(Sales_.restToPay))
            )
        );

        if (nonNull(specification)) {
            Predicate predicate = specification.toPredicate(root, query, cb);
            query.where(predicate);
        }

        TypedQuery<DiffereSummary> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }

    @Override
    public Solde getSolde(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Solde> query = cb.createQuery(Solde.class);
        Root<Sales> root = query.from(Sales.class);
        query.select(cb.construct(Solde.class, cb.sumAsLong(root.get(Sales_.restToPay))));

        if (nonNull(specification)) {
            Predicate predicate = specification.toPredicate(root, query, cb);
            query.where(predicate);
        }

        TypedQuery<Solde> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }

    @Override
    public List<TicketZCreditProjection> getTicketZDifferes(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TicketZCreditProjection> query = cb.createQuery(TicketZCreditProjection.class);
        Root<Sales> root = query.from(Sales.class);
        query
            .select(
                cb.construct(
                    TicketZCreditProjection.class,
                    root.get(Sales_.caissier).get(AppUser_.id),
                    root.get(Sales_.caissier).get(AppUser_.firstName),
                    root.get(Sales_.caissier).get(AppUser_.lastName),
                    cb.sumAsLong(root.get(Sales_.restToPay))
                )
            )
            .groupBy( root.get(Sales_.caissier).get(AppUser_.id),
                root.get(Sales_.caissier).get(AppUser_.firstName),
                root.get(Sales_.caissier).get(AppUser_.lastName));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<TicketZCreditProjection> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    @Override
    public VenteRecord fetchVenteRecord(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VenteRecord> cq = cb.createQuery(VenteRecord.class);
        Root<Sales> root = cq.from(Sales.class);
        SetJoin<Sales, SalePayment> payments = root.joinSet(Sales_.PAYMENTS, JoinType.LEFT);
        SetJoin<Sales, SalesLine> salesLineSetJoin = root.joinSet(Sales_.SALES_LINES);
        cq.select(buildVenteRecord(root, salesLineSetJoin, payments, cb, null));

        cq.where(specification.toPredicate(root, cq, cb));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private List<VenteRecord> fetch(Specification<Sales> specification, StatGroupBy statGroupBy) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VenteRecord> cq = cb.createQuery(VenteRecord.class);
        Root<Sales> root = cq.from(Sales.class);
        SetJoin<Sales, SalePayment> payments = root.joinSet(Sales_.PAYMENTS, JoinType.LEFT);
        SetJoin<Sales, SalesLine> salesLineSetJoin = root.joinSet(Sales_.SALES_LINES);
        Expression<?> groupingExpression = getGroupingExpression(root, statGroupBy);
        cq
            .select(buildVenteRecord(root, salesLineSetJoin, payments, cb, groupingExpression))
            .groupBy(groupingExpression, root.get(Sales_.statut))
            .orderBy(cb.desc(groupingExpression));
        cq.where(specification.toPredicate(root, cq, cb));
        return entityManager.createQuery(cq).getResultList();
    }

    private List<VenteRecord> fetch(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VenteRecord> cq = cb.createQuery(VenteRecord.class);
        Root<Sales> root = cq.from(Sales.class);
        SetJoin<Sales, SalePayment> payments = root.joinSet(Sales_.PAYMENTS, JoinType.LEFT);
        SetJoin<Sales, SalesLine> salesLineSetJoin = root.joinSet(Sales_.SALES_LINES);
        Expression<String> groupingExpression = root.get(Sales_.type);
        cq
            .select(buildVenteRecord(root, salesLineSetJoin, payments, cb, groupingExpression))
            .groupBy(groupingExpression, root.get(Sales_.statut))
            .orderBy(cb.desc(groupingExpression));
        cq.where(specification.toPredicate(root, cq, cb));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<VentePeriodeRecord> fetchVentePeriodeRecords(Specification<Sales> specification, StatGroupBy statGroupBy) {
        try {
            return fetch(specification, statGroupBy)
                .stream()
                .map(venteRecord -> new VentePeriodeRecord(buildMvtDate(venteRecord, statGroupBy), venteRecord.statut().name(), venteRecord)
                )
                .toList();
        } catch (Exception e) {
            LOG.error(null, e);
            return List.of();
        }
    }

    @Override
    public List<VenteByTypeRecord> fetchVenteByTypeRecords(Specification<Sales> specification) {
        try {
            return fetch(specification).stream().map(venteRecord -> new VenteByTypeRecord(venteRecord.type(), venteRecord)).toList();
        } catch (Exception e) {
            LOG.error(null, e);
            return List.of();
        }
    }

    private Selection<VenteRecord> buildVenteRecord(
        Root<Sales> root,
        SetJoin<Sales, SalesLine> salesLineSetJoin,
        SetJoin<Sales, SalePayment> payments,
        CriteriaBuilder cb,
        Expression<?> groupingExpression
    ) {
        Path<ThirdPartySales> thirdPartySalesPath = cb.treat(root, ThirdPartySales.class); //cb.sum(1,cb.quot(salesLineSetJoin.get(SalesLine_.taxValue),100.0d))

        return cb.construct(
            VenteRecord.class,
            cb.sum(root.get(Sales_.salesAmount)),
            cb.sum(root.get(Sales_.amountToBePaid)),
            cb.sum(root.get(Sales_.discountAmount)),
            cb.sum(cb.prod(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.costAmount))),

            cb.sum(root.get(Sales_.amountToBeTakenIntoAccount)),
            cb.diff(cb.sum(root.get(Sales_.salesAmount)), cb.sum(root.get(Sales_.discountAmount))),
            cb.ceiling(
                cb.sum(
                    cb.quot(
                        salesLineSetJoin.get(SalesLine_.salesAmount),
                        cb.sum(1, cb.quot(salesLineSetJoin.get(SalesLine_.taxValue), 100.0d))
                    )
                )
            ),//postgres
            cb.sum(thirdPartySalesPath.get(ThirdPartySales_.partAssure)),
            cb.sum(thirdPartySalesPath.get(ThirdPartySales_.partTiersPayant)),
            cb.sum(root.get(Sales_.restToPay)),
            cb.sum(root.get(Sales_.payrollAmount)),
            cb.sum(payments.get(SalePayment_.paidAmount)),
            cb.sum(payments.get(SalePayment_.reelAmount)),
            cb.sum(cb.prod(salesLineSetJoin.get(SalesLine_.regularUnitPrice), salesLineSetJoin.get(SalesLine_.quantityUg))),
            cb.count(root),
            //   cb.quot(cb.sum(root.get(Sales_.htAmount)), cb.count(root)),
            nonNull(groupingExpression)? root.get(Sales_.type): cb.literal(""),
            nonNull(groupingExpression)? root.get(Sales_.statut): cb.nullLiteral(SalesStatut.class),
            nonNull(groupingExpression) ? groupingExpression.cast(String.class) :  cb.literal("")
        );
    }

    @Override
    public List<BalanceCaisseDTO> fetchSalesForBalanceCaisse(MvtParam mvtParam) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BalanceCaisseDTO> cq = cb.createQuery(BalanceCaisseDTO.class);
        Root<Sales> root = cq.from(Sales.class);
        SetJoin<Sales, SalePayment> payments = root.joinSet(Sales_.PAYMENTS, JoinType.LEFT);
        SetJoin<Sales, SalesLine> salesLineSetJoin = root.joinSet(Sales_.SALES_LINES);
        Join<SalePayment, PaymentMode> paymentMode = payments.join(PaymentTransaction_.paymentMode, JoinType.INNER);
        Path<ThirdPartySales> thirdPartySalesPath = cb.treat(root, ThirdPartySales.class);
        Expression<Integer> quantityExpression = mvtParam.isExcludeFreeUnit() ? cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)) : salesLineSetJoin.get(SalesLine_.quantityRequested);

        Expression<Long> montantTtcExpression = mvtParam.isExcludeFreeUnit() ? cb.sumAsLong(cb.prod(cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)), salesLineSetJoin.get(SalesLine_.regularUnitPrice))) : cb.sumAsLong(cb.prod(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.regularUnitPrice)));
        Expression<Long> montantTtcAcahtExpression = mvtParam.isExcludeFreeUnit() ? cb.sumAsLong(cb.prod(cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)), salesLineSetJoin.get(SalesLine_.costAmount))) : cb.sumAsLong(cb.prod(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.costAmount)));
        Expression<?> discountExpression = mvtParam.isExcludeFreeUnit() ? cb.sum(cb.prod(quantityExpression, salesLineSetJoin.get(SalesLine_.tauxRemise))) : cb.sumAsLong(root.get(Sales_.discountAmount));
        Expression<Integer> montantHtQtyExpression = mvtParam.isExcludeFreeUnit() ? cb.prod(cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)), salesLineSetJoin.get(SalesLine_.regularUnitPrice)) : cb.prod(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.regularUnitPrice));


        cq.select(createBalanceCaisseDTOResult(cb, root, payments, paymentMode, salesLineSetJoin, thirdPartySalesPath, montantTtcExpression, montantTtcAcahtExpression, discountExpression, montantHtQtyExpression));

        cq.where(saleSpecification.builder(mvtParam).toPredicate(root, cq, cb));
        cq.groupBy(root.get(Sales_.type), payments.get(PaymentTransaction_.paymentMode).get(PaymentMode_.code));

        return entityManager.createQuery(cq).getResultList();
    }

    private CompoundSelection<BalanceCaisseDTO> createBalanceCaisseDTOResult(CriteriaBuilder cb, Root<Sales> root, SetJoin<Sales, SalePayment> payments, Join<SalePayment, PaymentMode> paymentMode, SetJoin<Sales, SalesLine> salesLineSetJoin, Path<ThirdPartySales> thirdPartySalesPath, Expression<Long> montantTtcExpression, Expression<Long> montantTtcAcahtExpression, Expression<?> discountExpression, Expression<Integer> montantHtQtyExpression) {
        return cb.construct(BalanceCaisseDTO.class,
            root.get(Sales_.type),
            cb.count(root.get(Sales_.id)),
            discountExpression,
            montantTtcExpression,
            cb.sumAsLong(payments.get(PaymentTransaction_.paidAmount)),
            cb.sumAsLong(payments.get(PaymentTransaction_.reelAmount)),
            cb.ceiling(
                cb.sum(
                    cb.quot(
                        montantHtQtyExpression,
                        cb.sum(1, cb.quot(salesLineSetJoin.get(SalesLine_.taxValue), 100.0d))
                    )
                )
            ),
            paymentMode.get(PaymentMode_.code),
            paymentMode.get(PaymentMode_.libelle),
            montantTtcAcahtExpression,
            cb.sum(root.get(Sales_.restToPay)),
            cb.sumAsLong(root.get(Sales_.amountToBeTakenIntoAccount)),
            cb.sumAsLong(thirdPartySalesPath.get(ThirdPartySales_.partTiersPayant)),
            cb.sumAsLong(thirdPartySalesPath.get(ThirdPartySales_.partAssure))
        );
    }


    private String buildMvtDate(VenteRecord venteRecord, StatGroupBy statGroupBy) {
        if (isNull(statGroupBy)) {
            return null;
        }
        var group = venteRecord.group();
        return switch (statGroupBy) {
            case DAY -> LocalDate.parse(group).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case MONTH -> DateUtil.getMonthFromMonth(Month.of(Integer.parseInt(group)));
            case YEAR -> Year.of(Integer.parseInt(group)).toString();
            case HOUR -> group;
        };
    }

    private Expression<?> getGroupingExpression(Root<Sales> root, StatGroupBy statGroupBy) {
        return switch (statGroupBy) {
            case DAY ->
                entityManager.getCriteriaBuilder().function("date", LocalDate.class, root.get(Sales_.updatedAt));
            case MONTH ->
                entityManager.getCriteriaBuilder().function("month", Integer.class, root.get(Sales_.updatedAt));
            case YEAR -> entityManager.getCriteriaBuilder().function("year", Integer.class, root.get(Sales_.updatedAt));
            case HOUR -> entityManager.getCriteriaBuilder().function("hour", Integer.class, root.get(Sales_.updatedAt));
            case null -> null;
        };
    }
}
