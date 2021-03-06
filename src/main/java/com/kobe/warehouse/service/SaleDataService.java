package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.TicketDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SaleDataService {
    private final EntityManager em;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final SalesRepository salesRepository;
    private final SalesLineRepository salesLineRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;

    public SaleDataService(EntityManager em, UserRepository userRepository, ReportService reportService, SalesRepository salesRepository, SalesLineRepository salesLineRepository, PaymentRepository paymentRepository, TicketRepository ticketRepository) {
        this.em = em;
        this.userRepository = userRepository;
        this.reportService = reportService;
        this.salesRepository = salesRepository;
        this.salesLineRepository = salesLineRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
    }

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
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }


    public List<SaleDTO> customerPurchases(String query, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.INNER);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get("updatedAt")));
        List<Predicate> predicates = new ArrayList<>();
        predicates(query, fromDate, toDate, predicates, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }


    public SaleDTO findOne(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch(Sales_.SALES_LINES, JoinType.INNER);
        root.fetch(Sales_.PAYMENTS, JoinType.LEFT);
        root.fetch(Sales_.TICKETS, JoinType.LEFT);
        cq.select(root).distinct(true);
        cq.where(cb.equal(root.get(Sales_.id),id));
        TypedQuery<Sales> q = em.createQuery(cq);
        Sales sales=q.getSingleResult();
        return  new SaleDTO(sales);
    }


    private void predicates(String query, LocalDate fromDate, LocalDate toDate, List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root) {
        if (!StringUtils.isEmpty(query)) {
            query =  query.toUpperCase() + "%";
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
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
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

    public List<SaleDTO> allPrevente(String query, String type) {
        if (StringUtils.isEmpty(type) || type.equals(EntityConstant.TOUT)) return this.allPreventes(query);
        if (type.equals(EntityConstant.VNO)) return this.allPreventeVNO(query);
        if (type.equals(EntityConstant.VO)) return this.allPreventeVO(query);
        else return this.allPreventes(query);
    }

    public List<SaleDTO> allPreventeVNO(String query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        Root<CashSale> cashSaleRoot = cb.treat(root, CashSale.class);
        cq.select(cashSaleRoot).distinct(true).orderBy(cb.desc(root.get(CashSale_.updatedAt)));
        cashSaleRoot.fetch(CashSale_.SALES_LINES);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(cashSaleRoot.get(CashSale_.user).get(User_.magasin), this.getUser().getMagasin()));
        predicatesPreventeVNO(query, predicates, cb, cashSaleRoot);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    public List<SaleDTO> allPreventes(String query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
        root.fetch(Sales_.SALES_LINES);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get(Sales_.user).get(User_.magasin), this.getUser().getMagasin()));
        predicatesPrevente(query, predicates, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    public List<SaleDTO> allPreventeVO(String query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        Root<ThirdPartySales> thirdPartySalesRoot = cb.treat(root, ThirdPartySales.class);
        cq.select(thirdPartySalesRoot).distinct(true).orderBy(cb.desc(root.get(ThirdPartySales_.updatedAt)));
        thirdPartySalesRoot.fetch(Sales_.SALES_LINES);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(thirdPartySalesRoot.get(ThirdPartySales_.user).get(User_.magasin), this.getUser().getMagasin()));
        predicatesPreventeVO(query, predicates, cb, thirdPartySalesRoot);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    private void predicatesPreventeVNO(String query, List<Predicate> predicates, CriteriaBuilder cb, Root<CashSale> root) {
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            SetJoin<CashSale, SalesLine> lineSetJoin = root.joinSet(CashSale_.SALES_LINES);
            Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(cb.or(cb.like(cb.upper(produitJoin.get(Produit_.libelle)), query),
                cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), query),
                cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), query)));
        }
        predicates.add(cb.equal(root.get(CashSale_.statut), SalesStatut.ACTIVE));
        predicates.add(cb.between(cb.function("DATE", Date.class, root.get(CashSale_.updatedAt)), Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now())));

    }

    private void predicatesPrevente(String query, List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root) {
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            SetJoin<Sales, SalesLine> lineSetJoin = root.joinSet(Sales_.SALES_LINES);
            Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(cb.or(cb.like(cb.upper(produitJoin.get(Produit_.libelle)), query),
                cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), query),
                cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), query)));
        }
        predicates.add(cb.equal(root.get(Sales_.statut), SalesStatut.ACTIVE));
        predicates.add(cb.between(cb.function("DATE", Date.class, root.get(Sales_.updatedAt)), Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now())));

    }

    private void predicatesPreventeVO(String query, List<Predicate> predicates, CriteriaBuilder cb, Root<ThirdPartySales> root) {
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            SetJoin<ThirdPartySales, SalesLine> lineSetJoin = root.joinSet(ThirdPartySales_.SALES_LINES);
            Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(cb.or(cb.like(cb.upper(produitJoin.get(Produit_.libelle)), query),
                cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), query),
                cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), query)));
        }
        predicates.add(cb.equal(root.get(ThirdPartySales_.statut), SalesStatut.ACTIVE));
        predicates.add(cb.between(cb.function("DATE", Date.class, root.get(ThirdPartySales_.updatedAt)), Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now())));

    }


    public Page<SaleDTO> listVenteTerminees(String search,
                                            LocalDate fromDate,
                                            LocalDate toDate,
                                            String fromHour,
                                            String toHour,
                                            Boolean global,
                                            Long userId,
                                            String type,
                                            PaymentStatus paymentStatus, Boolean isDiffere,
                                            Pageable pageable) {

        var totalCount = countVentesTerminees(search,
            fromDate,
            toDate,
            fromHour,
            toHour,
            global,
            userId,
            paymentStatus, isDiffere);
        if (totalCount == 0) return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        if (StringUtils.isNotEmpty(type) && !type.equals(EntityConstant.TOUT)) {
            if (type.equals(EntityConstant.VO)) {
                Root<ThirdPartySales> thirdPartySalesRoot = cb.treat(root, ThirdPartySales.class);
                cq.select(thirdPartySalesRoot).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
            } else {
                Root<CashSale> cashSaleRoot = cb.treat(root, CashSale.class);
                cq.select(cashSaleRoot).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
            }
        } else {
            cq.select(root).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
        }
        List<Predicate> predicates = new ArrayList<>();
        predicatesVentesTerminees(search, fromDate, toDate, fromHour,
            toHour,
            global,
            userId,
            paymentStatus, isDiffere, predicates, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);

        if (pageable != null) {
            q.setFirstResult(pageable.getPageNumber());
            q.setMaxResults(pageable.getPageSize());
        }

        List<Sales> results = q.getResultList();

        return new PageImpl<>(results.stream().map(s -> new SaleDTO(s)
            .setPayments(getAllPaymentBySaleId(s.getId()))
            .setSalesLines(getAllItemsBySaleId(s.getId()))
            .setTickets(getAllTicketBySaleId(s.getId()))

        ).collect(Collectors.toList()), pageable, totalCount);
    }

    public List<SaleLineDTO> getAllItemsBySaleId(Long id) {
        return this.salesLineRepository.findAllBySalesId(id).stream().flatMap(List::stream)
            .map(SaleLineDTO::new).
            sorted(Comparator.comparing(SaleLineDTO::getCode))
            .collect(Collectors.toList());
    }

    public List<PaymentDTO> getAllPaymentBySaleId(Long id) {
        return this.paymentRepository.findBySalesId(id).stream().flatMap(List::stream).map(PaymentDTO::new).collect(Collectors.toList());
    }

    public List<TicketDTO> getAllTicketBySaleId(Long id) {
        return this.ticketRepository.findBySaleId(id).stream().flatMap(List::stream).map(TicketDTO::new).collect(Collectors.toList());
    }

    private long countVentesTerminees(String search,
                                      LocalDate fromDate,
                                      LocalDate toDate,
                                      String fromHour,
                                      String toHour,
                                      Boolean global,
                                      Long userId,
                                      PaymentStatus paymentStatus, Boolean isDiffere) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Sales> root = cq.from(Sales.class);
        predicatesVentesTerminees(search, fromDate, toDate, fromHour,
            toHour,
            global,
            userId,
            paymentStatus, isDiffere, predicates, cb, root);
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        return q.getSingleResult();

    }

    private void lineSetJoin(String query, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (StringUtils.isNotEmpty(query)) {
            if (StringUtils.isNotEmpty(query)) {
                query = query.toUpperCase() + "%";
                SetJoin<Sales, SalesLine> lineSetJoin = root.joinSet(Sales_.SALES_LINES);
                Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
                SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
                predicates.add(cb.or(cb.like(cb.upper(produitJoin.get(Produit_.libelle)), query),
                    cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), query),
                    cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), query)

                ));
            }
        }
    }

    private void periodeDatePredicat(LocalDate fromDate, LocalDate toDate, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (fromDate != null && toDate != null) {
            predicates.add(cb.between(cb.function("DATE", Date.class, root.get(Sales_.updatedAt)), Date.valueOf(fromDate), Date.valueOf(toDate)));
        }
    }

    private void periodeTimePredicat(String fromHour, String toHour, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (StringUtils.isNotEmpty(fromHour) && StringUtils.isNotEmpty(toHour)) {
            predicates.add(cb.between(cb.function("TIME", Time.class, root.get(Sales_.updatedAt)), Time.valueOf(LocalTime.parse(fromHour)), Time.valueOf(LocalTime.parse(toHour.concat(":59")))));
        }


    }

    private void predicatesVentesTerminees(String search, LocalDate fromDate, LocalDate toDate, String fromHour,
                                           String toHour,
                                           Boolean global,
                                           Long userId,
                                           PaymentStatus paymentStatus, Boolean isDiffere, List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root) {
        if (global) {
            lineSetJoin(search, cb, predicates, root);
           periodeDatePredicat(fromDate, toDate, cb, predicates, root);
            periodeTimePredicat(fromHour, toHour, cb, predicates, root);
        } else { //recherche par reference
            predicatRechercheParReference(search, cb, predicates, root);
        }
        periodeUserPredicat(userId, cb, predicates, root);
        predicates.add(cb.isFalse(root.get(Sales_.canceled)));
        predicates.add(cb.equal(root.get(Sales_.statut), SalesStatut.CLOSED));
        predicates.add(cb.equal(root.get(Sales_.user).get(User_.magasin), this.getUser().getMagasin()));
        impayePredicats(predicates, cb, root, paymentStatus);
        if (isDiffere != null) {
            if (isDiffere) {
                predicates.add(cb.isTrue(root.get(Sales_.differe)));
            } else {
                predicates.add(cb.isFalse(root.get(Sales_.differe)));
            }
        }
    }

    private void periodeUserPredicat(Long userId, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (userId != null) {
            predicates.add(cb.or(cb.equal(root.get(Sales_.user).get(User_.id), userId), cb.equal(root.get(Sales_.cassier).get(User_.id), userId),
                cb.equal(root.get(Sales_.user).get(User_.id), userId),
                cb.equal(root.get(Sales_.seller).get(User_.id), userId)
            ));
        }
    }

    private void predicatRechercheParReference(String query, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (StringUtils.isNotEmpty(query)) {
            if (StringUtils.isNotEmpty(query)) {
                query = query.toUpperCase() + "%";
                SetJoin<Sales, Ticket> tickeSetJoin = root.joinSet(Sales_.TICKETS);
                predicates.add(cb.or(cb.like(root.get(Sales_.numberTransaction), query),
                    cb.like(tickeSetJoin.get(Ticket_.code), query)
                ));
            }
        }
    }

    private void impayePredicats(List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root, PaymentStatus paymentStatus) {
        if (paymentStatus != null && !paymentStatus.equals(PaymentStatus.ALL)) {
            predicates.add(cb.equal(root.get(Sales_.paymentStatus), paymentStatus));
        }

    }
}
