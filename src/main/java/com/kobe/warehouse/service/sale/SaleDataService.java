package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.CashSale_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.Ticket_;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.User_;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.service.report.SaleInvoiceReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import java.net.MalformedURLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SaleDataService {

    private final EntityManager em;
    private final UserRepository userRepository;
    private final SaleInvoiceReportService saleInvoiceService;

    private final SalesLineRepository salesLineRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public SaleDataService(
        EntityManager em,
        UserRepository userRepository,
        SaleInvoiceReportService saleInvoiceService,
        SalesLineRepository salesLineRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository
    ) {
        this.em = em;
        this.userRepository = userRepository;
        this.saleInvoiceService = saleInvoiceService;

        this.salesLineRepository = salesLineRepository;

        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
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
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).collect(Collectors.toList());
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
            predicates.add(
                cb.between(cb.function("DATE", Date.class, root.get("updatedAt")), Date.valueOf(fromDate), Date.valueOf(toDate))
            );
        }
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).collect(Collectors.toList());
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
        return q.getResultList().stream().map(SaleDTO::new).collect(Collectors.toList());
    }

    public SaleDTO findOne(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch(Sales_.SALES_LINES, JoinType.INNER);
        root.fetch(Sales_.PAYMENTS, JoinType.LEFT);
        root.fetch(Sales_.TICKETS, JoinType.LEFT);
        cq.select(root).distinct(true);
        cq.where(cb.equal(root.get(Sales_.id), id));
        TypedQuery<Sales> q = em.createQuery(cq);
        Sales sales = q.getSingleResult();
        return new SaleDTO(sales);
    }

    public Sales getOne(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch(Sales_.SALES_LINES, JoinType.INNER);
        root.fetch(Sales_.PAYMENTS, JoinType.LEFT);
        root.fetch(Sales_.TICKETS, JoinType.LEFT);
        cq.select(root).distinct(true);
        cq.where(cb.equal(root.get(Sales_.id), id));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getSingleResult();
    }

    public SaleDTO getOneSaleDTO(Long id) {
        Sales sales = getOne(id);
        return switch (sales) {
            case CashSale cashSale -> new CashSaleDTO(cashSale);
            case ThirdPartySales thirdPartySales -> new ThirdPartySaleDTO(thirdPartySales);
            default -> throw new RuntimeException("Not yet implemented");
        };
    }

    private void predicates(
        String query,
        LocalDate fromDate,
        LocalDate toDate,
        List<Predicate> predicates,
        CriteriaBuilder cb,
        Root<Sales> root
    ) {
        if (!StringUtils.isEmpty(query)) {
            query = query.toUpperCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get("customer").get("firstName")), query),
                    cb.like(cb.upper(root.get("customer").get("lastName")), query)
                )
            );
        }

        predicates.add(cb.notEqual(root.get("statut"), SalesStatut.PENDING));
        if (fromDate != null) {
            predicates.add(
                cb.between(cb.function("DATE", Date.class, root.get("updatedAt")), Date.valueOf(fromDate), Date.valueOf(toDate))
            );
        }
    }

    public SaleDTO fetchPurchaseBy(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.LEFT);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("id"), id));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        Sales sales = q.getSingleResult();
        if (sales instanceof ThirdPartySales thirdPartySales) {
            return buildFromEntity(thirdPartySales);
        } else {
            return new CashSaleDTO((CashSale) sales);
        }
    }

    public Optional<SaleDTO> fetchPurchaseForEditBy(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.LEFT);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("id"), id));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        Sales sales = q.getSingleResult();
        if (sales.getStatut() == SalesStatut.ACTIVE) {
            if (sales instanceof ThirdPartySales) {
                return Optional.of(buildFromEntity((ThirdPartySales) sales));
            } else {
                return Optional.of(new CashSaleDTO((CashSale) sales));
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Resource printInvoice(Long saleId) throws MalformedURLException {
        return this.saleInvoiceService.printInvoice(this.findOne(saleId));
    }

    private User getUser() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin);
        return user.orElseGet(null);
    }

    public List<SaleDTO> allPrevente(String query, String type, Long userId) {
        if (StringUtils.isEmpty(type) || type.equals(EntityConstant.TOUT)) {
            return allPreventes(query, userId);
        }
        if (type.equals(EntityConstant.VNO)) {
            return allPreventeVNO(query, userId);
        }
        if (type.equals(EntityConstant.VO)) {
            return allPreventeVO(query, userId);
        } else {
            return allPreventes(query, userId);
        }
    }

    public List<SaleDTO> allPreventeVNO(String query, Long userId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        Root<CashSale> cashSaleRoot = cb.treat(root, CashSale.class);
        cq.select(cashSaleRoot).distinct(true).orderBy(cb.desc(root.get(CashSale_.updatedAt)));
        //    cashSaleRoot.fetch(CashSale_.SALES_LINES);
        List<Predicate> predicates = new ArrayList<>();
        predicatesPrevente(query, predicates, cb, root, userId);
        //   predicatesPreventeVNO(query, predicates, cb, root, userId);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).toList();
    }

    public List<SaleDTO> allPreventes(String query, Long userId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
        //  root.fetch(Sales_.SALES_LINES);
        List<Predicate> predicates = new ArrayList<>();
        predicatesPrevente(query, predicates, cb, root, userId);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).toList();
    }

    public List<SaleDTO> allPreventeVO(String query, Long userId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        Root<ThirdPartySales> thirdPartySalesRoot = cb.treat(root, ThirdPartySales.class);
        cq.select(thirdPartySalesRoot).distinct(true).orderBy(cb.desc(root.get(ThirdPartySales_.updatedAt)));
        List<Predicate> predicates = new ArrayList<>();
        predicatesPrevente(query, predicates, cb, root, userId);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).toList();
    }

    private void predicatesPrevente(String query, List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root, Long userId) {
        LocalDate now = LocalDate.now();
        predicates.add(cb.equal(root.get(Sales_.user).get(User_.magasin), getUser().getMagasin()));
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            SetJoin<Sales, SalesLine> lineSetJoin = root.joinSet(Sales_.SALES_LINES);
            Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get(Sales_.numberTransaction)), query),
                    cb.like(cb.upper(produitJoin.get(Produit_.libelle)), query),
                    cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), query),
                    cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), query)
                )
            );
        }
        if (Objects.nonNull(userId)) {
            predicates.add(cb.equal(root.get(Sales_.seller).get(User_.id), userId));
        }

        predicates.add(cb.equal(root.get(Sales_.statut), SalesStatut.ACTIVE));
        predicates.add(cb.greaterThanOrEqualTo(root.get(Sales_.updatedAt), now.atStartOfDay()));
    }

    public Page<SaleDTO> listVenteTerminees(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        String fromHour,
        String toHour,
        Boolean global,
        Long userId,
        String type,
        PaymentStatus paymentStatus,
        Boolean isDiffere,
        Pageable pageable
    ) {
        var totalCount = countVentesTerminees(search, fromDate, toDate, fromHour, toHour, global, userId, paymentStatus, isDiffere);
        if (totalCount == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
        }
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
        predicatesVentesTerminees(
            search,
            fromDate,
            toDate,
            fromHour,
            toHour,
            global,
            userId,
            paymentStatus,
            isDiffere,
            predicates,
            cb,
            root
        );
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);

        if (pageable != null) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }

        List<Sales> results = q.getResultList();

        return new PageImpl<>(results.stream().map(this::buildSaleDTO).toList(), pageable, totalCount);
    }

    private long countVentesTerminees(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        String fromHour,
        String toHour,
        Boolean global,
        Long userId,
        PaymentStatus paymentStatus,
        Boolean isDiffere
    ) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Sales> root = cq.from(Sales.class);
        predicatesVentesTerminees(
            search,
            fromDate,
            toDate,
            fromHour,
            toHour,
            global,
            userId,
            paymentStatus,
            isDiffere,
            predicates,
            cb,
            root
        );
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
                predicates.add(
                    cb.or(
                        cb.like(cb.upper(produitJoin.get(Produit_.libelle)), query),
                        cb.like(cb.upper(produitJoin.get(Produit_.codeEan)), query),
                        cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), query)
                    )
                );
            }
        }
    }

    private void periodeDatePredicat(
        LocalDate fromDate,
        LocalDate toDate,
        CriteriaBuilder cb,
        List<Predicate> predicates,
        Root<Sales> root
    ) {
        if (fromDate != null && toDate != null) {
            predicates.add(
                cb.between(cb.function("DATE", Date.class, root.get(Sales_.updatedAt)), Date.valueOf(fromDate), Date.valueOf(toDate))
            );
        }
    }

    private void periodeTimePredicat(String fromHour, String toHour, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (StringUtils.isNotEmpty(fromHour) && StringUtils.isNotEmpty(toHour)) {
            predicates.add(
                cb.between(cb.function("TIME", String.class, root.get(Sales_.updatedAt)), fromHour.concat(":00"), toHour.concat(":59"))
            );
        }
    }

    private void predicatesVentesTerminees(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        String fromHour,
        String toHour,
        Boolean global,
        Long userId,
        PaymentStatus paymentStatus,
        Boolean isDiffere,
        List<Predicate> predicates,
        CriteriaBuilder cb,
        Root<Sales> root
    ) {
        if (global) {
            lineSetJoin(search, cb, predicates, root);
            periodeDatePredicat(fromDate, toDate, cb, predicates, root);
            periodeTimePredicat(fromHour, toHour, cb, predicates, root);
        } else { // recherche par reference
            predicatRechercheParReference(search, cb, predicates, root);
        }
        periodeUserPredicat(userId, cb, predicates, root);
        predicates.add(cb.isFalse(root.get(Sales_.canceled)));
        predicates.add(cb.equal(root.get(Sales_.statut), SalesStatut.CLOSED));
        predicates.add(cb.equal(root.get(Sales_.user).get(User_.magasin), getUser().getMagasin()));
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
            predicates.add(
                cb.or(
                    cb.equal(root.get(Sales_.user).get(User_.id), userId),
                    cb.equal(root.get(Sales_.cassier).get(User_.id), userId),
                    cb.equal(root.get(Sales_.user).get(User_.id), userId),
                    cb.equal(root.get(Sales_.seller).get(User_.id), userId)
                )
            );
        }
    }

    private void predicatRechercheParReference(String query, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (StringUtils.isNotEmpty(query)) {
            if (StringUtils.isNotEmpty(query)) {
                query = query.toUpperCase() + "%";
                SetJoin<Sales, Ticket> tickeSetJoin = root.joinSet(Sales_.TICKETS);
                predicates.add(cb.or(cb.like(root.get(Sales_.numberTransaction), query), cb.like(tickeSetJoin.get(Ticket_.code), query)));
            }
        }
    }

    private void impayePredicats(List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root, PaymentStatus paymentStatus) {
        if (paymentStatus != null && !paymentStatus.equals(PaymentStatus.ALL)) {
            predicates.add(cb.equal(root.get(Sales_.paymentStatus), paymentStatus));
        }
    }

    public Pair<List<ThirdPartySaleLineDTO>, List<ClientTiersPayantDTO>> buildTiersPayantDTOFromSale(
        List<ThirdPartySaleLine> thirdPartySaleLines
    ) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = new ArrayList<>();
        List<ThirdPartySaleLineDTO> thirdPartySaleLineDTOS = new ArrayList<>();
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            thirdPartySaleLineDTOS.add(new ThirdPartySaleLineDTO(thirdPartySaleLine));
            clientTiersPayantDTOS.add(
                new ClientTiersPayantDTO(thirdPartySaleLine.getClientTiersPayant()).setNumBon(thirdPartySaleLine.getNumBon())
            );
        });
        return Pair.of(thirdPartySaleLineDTOS, clientTiersPayantDTOS);
    }

    public List<SalesLine> getAllAvoirs() {
        return salesLineRepository.findAllByQuantityAvoirGreaterThan(0);
    }

    private ThirdPartySaleDTO buildFromEntity(ThirdPartySales thirdPartySales) {
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
        Pair<List<ThirdPartySaleLineDTO>, List<ClientTiersPayantDTO>> listListPair = buildTiersPayantDTOFromSale(
            thirdPartySaleLineRepository.findAllBySaleId(thirdPartySales.getId())
        );
        thirdPartySaleDTO.setTiersPayants(listListPair.getRight());
        thirdPartySaleDTO.setThirdPartySaleLines(listListPair.getLeft());
        return thirdPartySaleDTO;
    }

    private SaleDTO buildSaleDTO(Sales s) {
        if (s instanceof ThirdPartySales thirdPartySales) {
            return buildFromEntity(thirdPartySales);
        } else {
            return new CashSaleDTO((CashSale) s);
        }
    }
}
