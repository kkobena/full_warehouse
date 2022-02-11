package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaleDataService {
    private final EntityManager em;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final SalesRepository salesRepository;

    public SaleDataService(EntityManager em, UserRepository userRepository, ReportService reportService, SalesRepository salesRepository) {
        this.em = em;
        this.userRepository = userRepository;
        this.reportService = reportService;
        this.salesRepository = salesRepository;
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> customerPurchases(Long customerId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.INNER);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get("updatedAt")));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("customer").get("id"), customerId));
        predicates.add(cb.notEqual(root.get("statut"), SalesStatut.PENDING));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> customerPurchases(Long customerId, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.INNER);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get("updatedAt")));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("customer").get("id"), customerId));
        predicates.add(cb.notEqual(root.get("statut"), SalesStatut.PENDING));
        if (fromDate != null) {
            predicates.add(cb.between(cb.function("DATE", Date.class, root.get("updatedAt")), Date.valueOf(fromDate), Date.valueOf(toDate)));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> customerPurchases(String query, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.INNER);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get("updatedAt")));
        List<Predicate> predicates = new ArrayList<>();
        predicates(query, fromDate, toDate, predicates, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    private void predicates(String query, LocalDate fromDate, LocalDate toDate, List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root) {
        if (!StringUtils.isEmpty(query)) {
            query = "%" + query.toUpperCase() + "%";
            predicates.add(cb.or(cb.like(cb.upper(root.get("customer").get("firstName")), query), cb.like(cb.upper(root.get("customer").get("lastName")), query)));
        }

        predicates.add(cb.notEqual(root.get("statut"), SalesStatut.PENDING));
        if (fromDate != null) {
            predicates.add(cb.between(cb.function("DATE", Date.class, root.get("updatedAt")), Date.valueOf(fromDate), Date.valueOf(toDate)));
        }
    }

    private long count(String query, LocalDate fromDate, LocalDate toDate) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Sales> root = cq.from(Sales.class);
        predicates(query, fromDate, toDate, predicates, cb, root);
        cq.select(cb.count(root));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Long> q = em.createQuery(cq);
        return q.getSingleResult();
    }

    @Transactional(readOnly = true)
    public SaleDTO purchaseBy(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.LEFT);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("id"), id));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return new SaleDTO(q.getSingleResult());
    }


    @Transactional(readOnly = true)
    public String printInvoice(Long saleId) throws IOException {
        Optional<Sales> ptSale = salesRepository.findOneWithEagerSalesLines(saleId);
        Sales sales = ptSale.get();
        Map<String, Object> parameters = reportService.buildMagasinInfo();
        // reportService.buildCustomerInfo(parameters, sales.getCustomer());
        reportService.buildSaleInfo(parameters, sales);
        return reportService.buildReportToPDF(parameters, "warehouse_facture", sales.getSalesLines().stream().map(SaleLineDTO::new).collect(Collectors.toList()));

    }

    private User getUser() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(null);
    }


}
