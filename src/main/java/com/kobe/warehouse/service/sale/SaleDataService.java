package com.kobe.warehouse.service.sale;

import static java.util.Objects.isNull;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.CashSale_;
import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.ReceiptPrinterService;
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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.validation.constraints.NotNull;
import java.net.MalformedURLException;
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

    private static final String DEFAULT_HEURE_DEBUT = "00:00";
    private static final String DEFAULT_HEURE_FIN = "23:59";
    private final EntityManager em;
    private final UserRepository userRepository;
    private final SaleInvoiceReportService saleInvoiceService;
    private final SalesLineRepository salesLineRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final ReceiptPrinterService receiptPrinterService;

    public SaleDataService(
        EntityManager em,
        UserRepository userRepository,
        SaleInvoiceReportService saleInvoiceService,
        SalesLineRepository salesLineRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        ReceiptPrinterService receiptPrinterService
    ) {
        this.em = em;
        this.userRepository = userRepository;
        this.saleInvoiceService = saleInvoiceService;

        this.salesLineRepository = salesLineRepository;

        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.receiptPrinterService = receiptPrinterService;
    }

    public List<SaleDTO> customerPurchases(Long customerId, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch(Sales_.SALES_LINES, JoinType.INNER);
        root.fetch(Sales_.PAYMENTS, JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get(Sales_.customer).get(Customer_.id), customerId));
        predicates.add(root.get(Sales_.statut).in(SalesStatut.CLOSED, SalesStatut.CANCELED));
        if (fromDate != null) {
            predicates.add(cb.between(root.get(Sales_.saleDate), fromDate, toDate));
        } else {
            LocalDate now = LocalDate.now();
            LocalDate from = LocalDate.now().minusYears(1);
            predicates.add(cb.between(root.get(Sales_.saleDate), from, now));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).collect(Collectors.toList());
    }

    public SaleDTO findOne(SaleId id) {
        return new SaleDTO(fetchById(id, false));
    }

    public Sales getOne(SaleId id) {
        return fetchById(id, false);
    }

    public SaleDTO getOneSaleDTO(SaleId id) {
        Sales sales = getOne(id);
        return switch (sales) {
            case CashSale cashSale -> new CashSaleDTO(cashSale);
            case ThirdPartySales thirdPartySales -> new ThirdPartySaleDTO(thirdPartySales);
            default -> throw new RuntimeException("Not yet implemented");
        };
    }

    public SaleDTO fetchPurchaseBy(@NotNull Long id, @NotNull LocalDate saleDate) {
        return fetch(new SaleId(id, saleDate), false).orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    public Optional<SaleDTO> fetchPurchaseForEditBy(@NotNull Long id, @NotNull LocalDate saleDate) {
        return fetch(new SaleId(id, saleDate), true);
    }

    private Sales fetchById(SaleId id, boolean toEdit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch(Sales_.SALES_LINES, JoinType.LEFT);
        root.fetch(Sales_.PAYMENTS, JoinType.LEFT);
        cq.select(root).distinct(true);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get(Sales_.id), id.getId()));
        predicates.add(cb.equal(root.get(Sales_.saleDate), id.getSaleDate()));
        if (toEdit) {
            predicates.add(cb.equal(root.get(Sales_.statut), SalesStatut.ACTIVE));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getSingleResult();
    }

    private Optional<SaleDTO> fetch(SaleId id, boolean toEdit) {
        Sales sales = fetchById(id, toEdit);
        if (isNull(sales)) {
            return Optional.empty();
        }

        if (sales instanceof ThirdPartySales thirdPartySales) {
            return Optional.of(buildFromEntity(thirdPartySales));
        } else {
            return Optional.of(new CashSaleDTO((CashSale) sales));
        }
    }

    @Transactional(readOnly = true)
    public Resource printInvoice(SaleId saleId) throws MalformedURLException {
        return this.saleInvoiceService.printInvoice(new SaleDTO(this.fetchById(saleId, false)));
    }

    private AppUser getUser() {
        Optional<AppUser> user = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin);
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
        List<Predicate> predicates = new ArrayList<>();
        predicatesPrevente(query, predicates, cb, root, userId);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).toList();
    }

    public List<SaleDTO> allPreventes(String query, Long userId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
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
        predicates.add(cb.equal(root.get(Sales_.user).get(AppUser_.magasin), getUser().getMagasin()));
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            SetJoin<Sales, SalesLine> lineSetJoin = root.joinSet(Sales_.SALES_LINES);
            Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(
                cb.or(
                    cb.like(root.get(Sales_.numberTransaction), query),
                    cb.like(fp.get(FournisseurProduit_.codeCip), query),
                    cb.like(fp.get(FournisseurProduit_.codeEan), query),
                    cb.like(cb.upper(produitJoin.get(Produit_.libelle)), query),
                    cb.like(produitJoin.get(Produit_.codeEanLaboratoire), query)
                )
            );
        }
        if (Objects.nonNull(userId)) {
            predicates.add(cb.equal(root.get(Sales_.seller).get(AppUser_.id), userId));
        }

        predicates.add(cb.equal(root.get(Sales_.statut), SalesStatut.ACTIVE));
        predicates.add(cb.equal(root.get(Sales_.saleDate), now));
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
                        cb.like(produitJoin.get(Produit_.codeEanLaboratoire), query),
                        cb.like(fp.get(FournisseurProduit_.codeCip), query),
                        cb.like(fp.get(FournisseurProduit_.codeEan), query)
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
            predicates.add(cb.between(root.get(Sales_.saleDate), fromDate, toDate));
        }
    }

    private void periodeTimePredicat(String fromHour, String toHour, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (isValidHour(fromHour, toHour)) {
            Expression<String> timeExpr = cb.function("TO_CHAR", String.class, root.get(Sales_.updatedAt), cb.literal("HH24:MI"));
            predicates.add(cb.between(timeExpr, fromHour.concat(":00"), toHour.concat(":59")));
        }
    }

    private boolean isValidHour(String fromHour, String toHour) {
        if (StringUtils.isEmpty(fromHour) || StringUtils.isEmpty(toHour)) {
            return false;
        }
        return !fromHour.equals(DEFAULT_HEURE_DEBUT) && !toHour.equals(DEFAULT_HEURE_FIN);
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
            periodeDatePredicat(fromDate, toDate, cb, predicates, root); // a cause du partitionnement par date
            predicatRechercheParReference(search, cb, predicates, root);
        }
        periodeUserPredicat(userId, cb, predicates, root);
        // predicates.add(cb.isFalse(root.get(Sales_.canceled)));
        predicates.add(root.get(Sales_.statut).in(SalesStatut.CLOSED, SalesStatut.CANCELED));
        predicates.add(cb.equal(root.get(Sales_.user).get(AppUser_.magasin), getUser().getMagasin()));
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
                    cb.equal(root.get(Sales_.caissier).get(AppUser_.id), userId),
                    cb.equal(root.get(Sales_.user).get(AppUser_.id), userId),
                    cb.equal(root.get(Sales_.seller).get(AppUser_.id), userId)
                )
            );
        }
    }

    private void predicatRechercheParReference(String query, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (StringUtils.isNotEmpty(query)) {
            if (StringUtils.isNotEmpty(query)) {
                query = query.toUpperCase() + "%";
                predicates.add(cb.like(root.get(Sales_.numberTransaction), query));
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
        SaleId saleId = thirdPartySales.getId();
        Pair<List<ThirdPartySaleLineDTO>, List<ClientTiersPayantDTO>> listListPair = buildTiersPayantDTOFromSale(
            thirdPartySaleLineRepository.findAllBySaleIdAndSaleSaleDate(saleId.getId(), saleId.getSaleDate())
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

    public void printReceipt(SaleId saleId, boolean isEdit) {
        Sales sales = fetchById(saleId, isEdit);
        if (sales instanceof CashSale g) {
            receiptPrinterService.printCashSale(new CashSaleDTO(g), isEdit);
        } else if (sales instanceof ThirdPartySales thirdPartySales) {
            receiptPrinterService.printVoSale(new ThirdPartySaleDTO(thirdPartySales), isEdit);
        }
    }
}
