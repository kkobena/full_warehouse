package com.kobe.warehouse.repository;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalePayment_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.domain.User_;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.enumeration.StatGroupBy;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.reglement.differe.dto.Differe;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.Solde;
import com.kobe.warehouse.service.tiketz.dto.TicketZCreditProjection;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@Transactional(readOnly = true)
public class CustomSalesRepositoryImpl implements CustomSalesRepository {

    private final EntityManager entityManager;

    public CustomSalesRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
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
                    root.get(Sales_.cashRegister).get(CashRegister_.user).get(User_.firstName),
                    root.get(Sales_.cashRegister).get(CashRegister_.user).get(User_.lastName),
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

        Predicate predicate = specification.toPredicate(root, query, cb);
        if (nonNull(predicate)) {
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
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        if (nonNull(predicate)) {
            countQuery.where(predicate);
        }
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private Long count(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Sales> root = countQuery.from(Sales.class);
        countQuery.select(cb.count(root));
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        if (nonNull(predicate)) {
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

        Predicate predicate = specification.toPredicate(root, query, cb);
        if (nonNull(predicate)) {
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

        Predicate predicate = specification.toPredicate(root, query, cb);
        if (nonNull(predicate)) {
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
                    root.get(Sales_.caissier).get(User_.id),
                    root.get(Sales_.caissier).get(User_.firstName),
                    root.get(Sales_.caissier).get(User_.lastName),
                    cb.sumAsLong(root.get(Sales_.restToPay))
                )
            )
            .groupBy(root.get(Sales_.caissier).get(User_.id));

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
        SetJoin<Sales, SalePayment> payments = root.joinSet(Sales_.PAYMENTS);
        cq.select(buildVenteRecord(root, payments, cb));
        cq.where(specification.toPredicate(root, cq, cb));
        return entityManager.createQuery(cq).getSingleResult();
    }

    @Override
    public List<VentePeriodeRecord> fetchVentePeriodeRecords(Specification<Sales> specification, StatGroupBy statGroupBy) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Sales> root = cq.from(Sales.class);
        Expression<?> mvtDate = getGroupingExpression(root, statGroupBy);
        List<Selection<?>> selections = new ArrayList<>(buildSelection(root, cb));
        selections.add(mvtDate.alias("mvtDate"));
        selections.add(root.get("statut").alias("statut"));
        cq.multiselect(selections);
        cq.where(specification.toPredicate(root, cq, cb));
        cq.groupBy(mvtDate, root.get("statut"));
        cq.orderBy(cb.asc(mvtDate));
        List<Tuple> tuples = entityManager.createQuery(cq).getResultList();
        return tuples.stream().map(tuple -> buildVentePeriodeRecord(tuple, statGroupBy)).toList();
    }

    @Override
    public List<VenteByTypeRecord> fetchVenteByTypeRecords(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Sales> root = cq.from(Sales.class);
        List<Selection<?>> selections = new ArrayList<>(buildSelection(root, cb));
        selections.add(root.get("dtype").alias("type_vente"));
        cq.multiselect(selections);
        cq.where(specification.toPredicate(root, cq, cb));
        cq.groupBy(root.get("dtype"));
        List<Tuple> tuples = entityManager.createQuery(cq).getResultList();
        return tuples.stream().map(this::buildVenteByTypeRecord).toList();
    }

    @Override
    public List<VenteModePaimentRecord> fetchVenteModePaimentRecords(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Sales> root = cq.from(Sales.class);
        Join<Object, Object> payment = root.join("paymentTransactions");
        Join<Object, Object> paymentMode = payment.join("paymentMode");
        cq.multiselect(
            payment.get("paymentMode").get("code").alias("payment_mode_code"),
            paymentMode.get("libelle").alias("libelle"),
            cb.sum(payment.get("reelAmount")).alias("real_net_amount"),
            cb.sum(payment.get("paidAmount")).alias("paid_amount")
        );
        cq.where(specification.toPredicate(root, cq, cb));
        cq.groupBy(payment.get("paymentMode").get("code"), paymentMode.get("libelle"));
        List<Tuple> tuples = entityManager.createQuery(cq).getResultList();
        return tuples.stream().map(this::buildModePaiment).toList();
    }

    private CompoundSelection<VenteRecord> buildVenteRecord(Root<Sales> root, SetJoin<Sales, SalePayment> payments, CriteriaBuilder cb) {
        Path<ThirdPartySales> thirdPartySalesPath = cb.treat(root, ThirdPartySales.class);
        return cb.construct(
            VenteRecord.class,
            cb.sum(root.get(Sales_.salesAmount)),
            cb.sum(root.get(Sales_.amountToBePaid)),
            cb.sum(root.get(Sales_.discountAmount)),
            cb.sum(root.get(Sales_.costAmount)),
            cb.diff(cb.sum(root.get(Sales_.htAmount)), cb.sum(root.get(Sales_.costAmount))),
            cb.sum(root.get(Sales_.amountToBeTakenIntoAccount)),
            cb.sum(root.get(Sales_.htAmount)),
            cb.sum(thirdPartySalesPath.get(ThirdPartySales_.partAssure)),
            cb.sum(thirdPartySalesPath.get(ThirdPartySales_.partTiersPayant)),
            cb.sum(root.get(Sales_.taxAmount)),
            cb.sum(root.get(Sales_.restToPay)),
            cb.sum(root.get(Sales_.htAmountUg)),
            cb.sum(root.get(Sales_.discountAmountHorsUg)),
            cb.sum(root.get(Sales_.discountAmountUg)),
            cb.sum(root.get(Sales_.netUgAmount)),
            cb.sum(root.get(Sales_.margeUg)),
            cb.sum(root.get(Sales_.montantttcUg)),
            cb.sum(root.get(Sales_.payrollAmount)),
            cb.sum(root.get(Sales_.montantTvaUg)),
            cb.sum(root.get(Sales_.montantnetUg)),
            cb.sum(payments.get(SalePayment_.paidAmount)),
            cb.sum(payments.get(SalePayment_.reelAmount)),
            cb.quot(cb.sum(root.get(Sales_.htAmount)), cb.count(root)),
            root.get(Sales_.type),
            root.get(Sales_.statut)
        );
    }

    private List<Selection<?>> buildSelection(Root<Sales> root, CriteriaBuilder cb) {
        return List.of(
            cb.sum(root.get("salesAmount")).alias("sales_amount"),
            cb.quot(cb.sum(root.get("htAmount")), cb.count(root)).alias("panierMoyen"),
            cb.sum(root.get("amountToBePaid")).alias("amount_to_be_paid"),
            cb.sum(root.get("discountAmount")).alias("discount_amount"),
            cb.sum(root.get("costAmount")).alias("cost_amount"),
            cb.diff(cb.sum(root.get("htAmount")), cb.sum(root.get("costAmount"))).alias("marge"),
            cb.sum(root.get("amountToBeTakenIntoAccount")).alias("amount_to_be_taken_into_account"),
            cb.sum(root.get("netAmount")).alias("net_amount"),
            cb.sum(root.get("htAmount")).alias("ht_amount"),
            cb.sum(root.get("partAssure")).alias("part_assure"),
            cb.sum(root.get("partTiersPayant")).alias("part_tiers_payant"),
            cb.sum(root.get("taxAmount")).alias("tax_amount"),
            cb.sum(root.get("restToPay")).alias("rest_to_pay"),
            cb.sum(root.get("htAmountUg")).alias("ht_amount_ug"),
            cb.sum(root.get("discountAmountHorsUg")).alias("discount_amount_hors_ug"),
            cb.sum(root.get("discountAmountUg")).alias("discount_amount_ug"),
            cb.sum(root.get("netUgAmount")).alias("net_ug_amount"),
            cb.sum(root.get("margeUg")).alias("marge_ug"),
            cb.sum(root.get("montantTtcUg")).alias("montant_ttc_ug"),
            cb.sum(root.get("payrollAmount")).alias("payroll_amount"),
            cb.sum(root.get("montantTvaUg")).alias("montant_tva_ug"),
            cb.sum(root.get("montantNetUg")).alias("montant_net_ug"),
            cb.sum(root.get("paidAmount")).alias("paid_amount"),
            cb.sum(root.get("realNetAmount")).alias("real_net_amount"),
            cb.count(root).alias("sale_count")
        );
    }

    private VenteRecord buildVenteRecord(Tuple tuple) {
        if (Objects.isNull(tuple.get("sales_amount", BigDecimal.class))) {
            return null;
        }
        return new VenteRecord(
            tuple.get("sales_amount", BigDecimal.class),
            tuple.get("amount_to_be_paid", BigDecimal.class),
            tuple.get("discount_amount", BigDecimal.class),
            tuple.get("cost_amount", BigDecimal.class),
            tuple.get("marge", BigDecimal.class),
            tuple.get("amount_to_be_taken_into_account", BigDecimal.class),
            tuple.get("net_amount", BigDecimal.class),
            tuple.get("ht_amount", BigDecimal.class),
            tuple.get("part_assure", BigDecimal.class),
            tuple.get("part_tiers_payant", BigDecimal.class),
            tuple.get("tax_amount", BigDecimal.class),
            tuple.get("rest_to_pay", BigDecimal.class),
            tuple.get("ht_amount_ug", BigDecimal.class),
            tuple.get("discount_amount_hors_ug", BigDecimal.class),
            tuple.get("discount_amount_ug", BigDecimal.class),
            tuple.get("net_ug_amount", BigDecimal.class),
            tuple.get("marge_ug", BigDecimal.class),
            tuple.get("montant_ttc_ug", BigDecimal.class),
            tuple.get("payroll_amount", BigDecimal.class),
            tuple.get("montant_tva_ug", BigDecimal.class),
            tuple.get("montant_net_ug", BigDecimal.class),
            tuple.get("paid_amount", BigDecimal.class),
            tuple.get("real_net_amount", BigDecimal.class),
            tuple.get("sale_count", Long.class),
            tuple.get("panierMoyen", Double.class) != null ? tuple.get("panierMoyen", Double.class) : 0.0,
            "",
            null
        );
    }

    private VenteByTypeRecord buildVenteByTypeRecord(Tuple tuple) {
        TypeVenteDTO typeVenteDTO = fromTypeVente(tuple.get("type_vente", String.class));
        if (Objects.isNull(typeVenteDTO)) {
            return null;
        }

        return new VenteByTypeRecord(typeVenteDTO.getValue(), buildVenteRecord(tuple));
    }

    private VentePeriodeRecord buildVentePeriodeRecord(Tuple tuple, StatGroupBy statGroupBy) {
        String statut = tuple.get("statut", String.class);

        if (Objects.isNull(statut)) {
            return null;
        }

        return new VentePeriodeRecord(buildMvtDate(tuple, statGroupBy), statut, buildVenteRecord(tuple));
    }

    private VenteModePaimentRecord buildModePaiment(Tuple tuple) {
        String paymentModeCode = tuple.get("payment_mode_code", String.class);

        if (Objects.isNull(paymentModeCode)) {
            return null;
        }

        return new VenteModePaimentRecord(
            paymentModeCode,
            tuple.get("libelle", String.class),
            tuple.get("real_net_amount", BigDecimal.class),
            tuple.get("paid_amount", BigDecimal.class)
        );
    }

    private TypeVenteDTO fromTypeVente(String typeVente) {
        if (StringUtils.hasLength(typeVente)) {
            if (typeVente.equalsIgnoreCase(TypeVente.CashSale.name())) {
                return TypeVenteDTO.CashSale;
            } else if (typeVente.equalsIgnoreCase(TypeVente.ThirdPartySales.name())) {
                return TypeVenteDTO.ThirdPartySales;
            }
        }
        return null;
    }

    private String buildMvtDate(Tuple tuple, StatGroupBy statGroupBy) {
        return switch (statGroupBy) {
            case DAY -> LocalDate.parse(tuple.get("mvtDate", String.class)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case MONTH -> DateUtil.getMonthFromMonth(Month.of(tuple.get("mvtDate", Integer.class)));
            case YEAR -> Year.of(tuple.get("mvtDate", Integer.class)).toString();
            case HOUR -> tuple.get("mvtDate", Integer.class).toString();
        };
    }

    private Expression<?> getGroupingExpression(Root<Sales> root, StatGroupBy statGroupBy) {
        return switch (statGroupBy) {
            case DAY -> entityManager.getCriteriaBuilder().function("date", LocalDate.class, root.get(Sales_.updatedAt));
            case MONTH -> entityManager.getCriteriaBuilder().function("month", Integer.class, root.get(Sales_.updatedAt));
            case YEAR -> entityManager.getCriteriaBuilder().function("year", Integer.class, root.get(Sales_.updatedAt));
            case HOUR -> entityManager.getCriteriaBuilder().function("hour", Integer.class, root.get(Sales_.updatedAt));
        };
    }
}
