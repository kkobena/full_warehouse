package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.CashSale_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Magasin_;
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
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.VenteDepot_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.ReceiptPrinterService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.service.report.SaleInvoiceReportService;
import com.kobe.warehouse.service.stock.dto.StockDepotExportDTO;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;

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
    private final SalesRepository salesRepository;

    public SaleDataService(
        EntityManager em,
        UserRepository userRepository,
        SaleInvoiceReportService saleInvoiceService,
        SalesLineRepository salesLineRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        ReceiptPrinterService receiptPrinterService,
        SalesRepository salesRepository
    ) {
        this.em = em;
        this.userRepository = userRepository;
        this.saleInvoiceService = saleInvoiceService;

        this.salesLineRepository = salesLineRepository;

        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.receiptPrinterService = receiptPrinterService;
        this.salesRepository = salesRepository;
    }

    public List<SaleDTO> customerPurchases(Long customerId, LocalDate fromDate, LocalDate toDate) {
        Specification<Sales> specification = Specification.where(salesRepository.filterByCustomerId(customerId));
        specification = specification.and(salesRepository.hasStatut(EnumSet.of(SalesStatut.CLOSED)));
        if (fromDate != null) {
            specification = specification.and(salesRepository.between(fromDate, toDate));
        }
        return salesRepository.findAll(specification).stream().map(this::buildSaleDTO).toList();
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

        return switch (sales) {
            case ThirdPartySales thirdPartySales -> Optional.of(buildFromEntity(thirdPartySales));
            case CashSale cashSale -> Optional.of(new CashSaleDTO(cashSale));
            case VenteDepot venteDepot -> Optional.of(new DepotExtensionSaleDTO(venteDepot));
            default -> Optional.empty();
        };
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

    @Transactional
    public Page<DepotExtensionSaleDTO> fetchVenteDepot(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Long userId,
        PaymentStatus paymentStatus, Long depotId,
        Pageable pageable
    ) {
        Set<CategorieChiffreAffaire> categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.CA_DEPOT);
        var totalCount = countVentesDepot(search, fromDate, toDate, userId, depotId, paymentStatus, categorieChiffreAffaires);
        if (totalCount == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<VenteDepot> cq = cb.createQuery(VenteDepot.class);
        Root<VenteDepot> root = cq.from(VenteDepot.class);
        Join<VenteDepot, Magasin> saleJoinDepot = root.join(VenteDepot_.depot);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
        List<Predicate> predicates = new ArrayList<>();
        predicatesVentesDepot(
            search,
            fromDate,
            toDate,
            userId,
            paymentStatus,
            predicates,
            cb,
            root, saleJoinDepot, categorieChiffreAffaires, depotId
        );
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<VenteDepot> q = em.createQuery(cq);

        if (pageable != null) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }

        List<VenteDepot> results = q.getResultList();

        return new PageImpl<>(results.stream().map(this::buildDepotExtensionSaleDTO).toList(), pageable, totalCount);
    }

    @Transactional
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
        Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        Pageable pageable
    ) {
        long userMagasinId = getUser().getMagasin().getId();
        if (CollectionUtils.isEmpty(categorieChiffreAffaires)) {
            categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.CA, CategorieChiffreAffaire.CALLEBASE, CategorieChiffreAffaire.TO_IGNORE);
        }
        var totalCount = countVentesTerminees(search, fromDate, toDate, fromHour, toHour, global, userId, paymentStatus, isDiffere, categorieChiffreAffaires, userMagasinId);
        if (totalCount == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);

        Root<Sales> root = cq.from(Sales.class);
        Join<Sales, AppUser> userJoin = root.join(Sales_.user);
        Join<AppUser, Magasin> magasinJoin = userJoin.join(AppUser_.magasin);
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
            root, userJoin, magasinJoin, userMagasinId, categorieChiffreAffaires
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


    private long countVentesDepot(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Long userId,
        Long depotId,
        PaymentStatus paymentStatus,
        Set<CategorieChiffreAffaire> categorieChiffreAffaires
    ) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<VenteDepot> root = cq.from(VenteDepot.class);
        Join<VenteDepot, Magasin> saleJoinDepot = root.join(VenteDepot_.depot);
        predicatesVentesDepot(
            search,
            fromDate,
            toDate,
            userId,
            paymentStatus,
            predicates,
            cb,
            root, saleJoinDepot, categorieChiffreAffaires, depotId
        );
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        return q.getSingleResult();
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
        Boolean isDiffere, Set<CategorieChiffreAffaire> categorieChiffreAffaires, long userMagasinId
    ) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Sales> root = cq.from(Sales.class);
        Join<Sales, AppUser> userJoin = root.join(Sales_.user);
        Join<AppUser, Magasin> magasinJoin = userJoin.join(AppUser_.magasin);
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
            root, userJoin, magasinJoin, userMagasinId, categorieChiffreAffaires
        );
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        return q.getSingleResult();
    }


    private void buildDepotPredicatSaleLines(String query, CriteriaBuilder cb, List<Predicate> predicates, Root<VenteDepot> root) {
        if (StringUtils.isNotEmpty(query)) {
            if (StringUtils.isNotEmpty(query)) {
                query = query.toUpperCase() + "%";
                SetJoin<VenteDepot, SalesLine> lineSetJoin = root.joinSet(Sales_.SALES_LINES);
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
        Root<Sales> root, Join<Sales, AppUser> userJoin, Join<AppUser, Magasin> magasinJoin, Long userMagasinId,
        Set<CategorieChiffreAffaire> categorieChiffreAffaires
    ) {
        predicates.add(root.get(Sales_.categorieChiffreAffaire).in(categorieChiffreAffaires));
        if (global) {
            lineSetJoin(search, cb, predicates, root);
            periodeDatePredicat(fromDate, toDate, cb, predicates, root);
            periodeTimePredicat(fromHour, toHour, cb, predicates, root);
        } else { // recherche par reference
            periodeDatePredicat(fromDate, toDate, cb, predicates, root); // a cause du partitionnement par date
            predicatRechercheParReference(search, cb, predicates, root);
        }
        periodeUserPredicat(userId, cb, predicates, root, userJoin);
        // predicates.add(cb.isFalse(root.get(Sales_.canceled)));
        predicates.add(root.get(Sales_.statut).in(Set.of(SalesStatut.CLOSED, SalesStatut.CANCELED)));
        predicates.add(cb.equal(magasinJoin.get(Magasin_.id), userMagasinId));
        impayePredicats(predicates, cb, root, paymentStatus);
        if (isDiffere != null) {
            if (isDiffere) {
                predicates.add(cb.isTrue(root.get(Sales_.differe)));
            } else {
                predicates.add(cb.isFalse(root.get(Sales_.differe)));
            }
        }
    }


    private void predicatesVentesDepot(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Long userId,
        PaymentStatus paymentStatus,
        List<Predicate> predicates,
        CriteriaBuilder cb,
        Root<VenteDepot> root, Join<VenteDepot, Magasin> saleJoinDepot,
        Set<CategorieChiffreAffaire> categorieChiffreAffaires, Long depotId
    ) {
        if (depotId != null) {
            predicates.add(cb.equal(saleJoinDepot.get(Magasin_.id), depotId));
        }
        predicates.add(root.get(VenteDepot_.categorieChiffreAffaire).in(categorieChiffreAffaires));
        buildDepotPredicatSaleLines(search, cb, predicates, root);
        if (fromDate != null && toDate != null) {
            predicates.add(cb.between(root.get(VenteDepot_.saleDate), fromDate, toDate));
        }

        predicates.add(root.get(VenteDepot_.statut).in(Set.of(SalesStatut.CLOSED, SalesStatut.CANCELED)));
        predicates.add(cb.equal(root.get(VenteDepot_.user).get(AppUser_.magasin), getUser().getMagasin()));
        buildVenteDepotImpayePredicats(predicates, cb, root, paymentStatus);

    }


    private void periodeUserPredicat(Long userId, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root, Join<Sales, AppUser> userJoin) {
        if (userId != null) {
            Join<Sales, AppUser> caissierJoin = root.join(Sales_.caissier);
            Join<Sales, AppUser> selleJoin = root.join(Sales_.seller);
            predicates.add(
                cb.or(
                    cb.equal(caissierJoin.get(AppUser_.id), userId),
                    cb.equal(userJoin.get(AppUser_.id), userId),
                    cb.equal(selleJoin.get(AppUser_.id), userId)
                )
            );
        }
    }

    private void predicatRechercheParReference(String query, CriteriaBuilder cb, List<Predicate> predicates, Root<Sales> root) {
        if (StringUtils.isNotEmpty(query) && StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            predicates.add(cb.like(root.get(Sales_.numberTransaction), query));
        }

    }

    private void impayePredicats(List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root, PaymentStatus paymentStatus) {
        if (paymentStatus != null && !paymentStatus.equals(PaymentStatus.ALL)) {
            predicates.add(cb.equal(root.get(Sales_.paymentStatus), paymentStatus));
        }
    }

    private void buildVenteDepotImpayePredicats(List<Predicate> predicates, CriteriaBuilder cb, Root<VenteDepot> root, PaymentStatus paymentStatus) {
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

    private DepotExtensionSaleDTO buildDepotExtensionSaleDTO(VenteDepot venteDepot) {
        return new DepotExtensionSaleDTO(venteDepot);
    }

    private SaleDTO buildSaleDTO(Sales s) {
        if (s instanceof ThirdPartySales thirdPartySales) {
            return buildFromEntity(thirdPartySales);
        } else if (s instanceof CashSale cashSale) {
            return new CashSaleDTO(cashSale);
        } else if (s instanceof VenteDepot venteDepot) {
            return new DepotExtensionSaleDTO(venteDepot);
        }
        return new SaleDTO(s);
    }

    public void printReceipt(SaleId saleId, boolean isEdit) {
        Sales sales = fetchById(saleId, false);
        if (sales instanceof CashSale g) {
            receiptPrinterService.printCashSale(new CashSaleDTO(g), isEdit);
        } else if (sales instanceof ThirdPartySales thirdPartySales) {
            receiptPrinterService.printVoSale(new ThirdPartySaleDTO(thirdPartySales), isEdit);
        } else if (sales instanceof VenteDepot venteDepot) {
            receiptPrinterService.printVenteDepot(new DepotExtensionSaleDTO(venteDepot), isEdit);
        }
    }

    public byte[] generateEscPosReceipt(SaleId saleId, boolean isEdit) throws IOException {
        Sales sales = fetchById(saleId, false);
        if (sales instanceof CashSale g) {
            return receiptPrinterService.generateEscPosReceipt(new CashSaleDTO(g), isEdit);
        } else if (sales instanceof ThirdPartySales thirdPartySales) {
            return receiptPrinterService.generateEscPosReceipt(new ThirdPartySaleDTO(thirdPartySales), isEdit);
        } else if (sales instanceof VenteDepot venteDepot) {
            return receiptPrinterService.generateEscPosReceipt(new DepotExtensionSaleDTO(venteDepot), isEdit);
        }
        return new byte[0];
    }

    public List<StockDepotExportDTO> exportVenteDepotStock(SaleId venteDepotId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StockDepotExportDTO> cq = cb.createQuery(StockDepotExportDTO.class);
        Root<SalesLine> root = cq.from(SalesLine.class);
        Join<SalesLine, Produit> produitJoin = root.join(SalesLine_.produit);
        Join<SalesLine, Sales> saleJoin = root.join(SalesLine_.sales);
        Join<Produit, FournisseurProduit> fournisseurProduitJoin = produitJoin.join(Produit_.fournisseurProduitPrincipal);
        //Long produitId, String code, String produitLibelle, String codeEan, Integer quantitySold, Integer quantityRequested, Integer regularUnitPrice, Integer taxValue, Integer costAmount
        cq.select(
            cb.construct(
                StockDepotExportDTO.class,
                produitJoin.get(Produit_.id),
                fournisseurProduitJoin.get(FournisseurProduit_.codeCip),
                produitJoin.get(Produit_.codeEanLaboratoire),
                produitJoin.get(Produit_.libelle),
                root.get(SalesLine_.quantitySold),
                root.get(SalesLine_.quantityRequested),
                root.get(SalesLine_.regularUnitPrice),
                root.get(SalesLine_.taxValue),
                root.get(SalesLine_.costAmount)
            )
        ).orderBy(cb.asc(produitJoin.get(Produit_.libelle)));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(saleJoin.get(Sales_.id), venteDepotId.getId()));
        predicates.add(cb.equal(saleJoin.get(Sales_.saleDate), venteDepotId.getSaleDate()));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<StockDepotExportDTO> q = em.createQuery(cq);
        return q.getResultList();
    }
}
