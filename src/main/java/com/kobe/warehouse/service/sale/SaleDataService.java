package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.CashSale_;
import com.kobe.warehouse.domain.ClientTiersPayant;
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
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReceiptPrinterService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.service.dto.UserDTO;
import com.kobe.warehouse.service.report.SaleInvoiceReportService;
import com.kobe.warehouse.service.stock.dto.StockDepotExportDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Transactional(readOnly = true)
public class SaleDataService {

    private static final String DEFAULT_HEURE_DEBUT = "00:00";
    private static final String DEFAULT_HEURE_FIN = "23:59";
    private final EntityManager em;
    private final SaleInvoiceReportService saleInvoiceService;
    private final SalesLineRepository salesLineRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final ReceiptPrinterService receiptPrinterService;
    private final SalesRepository salesRepository;
    private final StorageService storageService;
    private final SalePaymentRepository salePaymentRepository;

    public SaleDataService(
        EntityManager em,
        SaleInvoiceReportService saleInvoiceService,
        SalesLineRepository salesLineRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        ReceiptPrinterService receiptPrinterService,
        SalesRepository salesRepository, StorageService storageService,
        SalePaymentRepository salePaymentRepository
    ) {
        this.em = em;

        this.saleInvoiceService = saleInvoiceService;

        this.salesLineRepository = salesLineRepository;

        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.receiptPrinterService = receiptPrinterService;
        this.salesRepository = salesRepository;
        this.storageService = storageService;
        this.salePaymentRepository = salePaymentRepository;
    }

    private static SaleDTO mapRowToSaleDTO(Object[] row) {
        int i = 0;
        String dtype = (String) row[i++];
        Long id = ((Number) row[i++]).longValue();
        LocalDate saleDate = toLocalDate(row[i++]);
        String numberTransaction = (String) row[i++];
        Integer discountAmount = asInteger(row[i++]);
        Integer salesAmount = asInteger(row[i++]);
        Integer amountToBePaid = asInteger(row[i++]);
        Integer restToPay = asInteger(row[i++]);
        String statut = (String) row[i++];
        String paymentStatus = (String) row[i++];
        String natureVente = (String) row[i++];
        String typePrescription = (String) row[i++];
        boolean differe = Boolean.TRUE.equals(row[i++]);
        boolean canceled = Boolean.TRUE.equals(row[i++]);
        LocalDateTime createdAt = toLocalDateTime(row[i++]);
        LocalDateTime updatedAt = toLocalDateTime(row[i++]);
        String commentaire = (String) row[i++];
        Integer monnaie = asInteger(row[i++]);
        String tvaEmbeded = (String) row[i++];
        String numBon = (String) row[i++];
        Integer partAssure = asInteger(row[i++]);
        Integer partTiersPayant = asInteger(row[i++]);
        String userFullName = (String) row[i++];
        Integer sellerId = asInteger(row[i++]);
        String sellerFirstName = (String) row[i++];
        String sellerLastName = (String) row[i++];
        Integer cassierId = asInteger(row[i++]);
        String cassierFirstName = (String) row[i++];
        String cassierLastName = (String) row[i++];
        String caisseNum = (String) row[i++];
        String caisseEndNum = (String) row[i++];
        String firstName = (String) row[i++];
        String lastName = (String) row[i++];
        String phone = (String) row[i++];

        Integer customerId = asInteger(row[i]);

        return SaleDTOBuilder.of(dtype)
            .id(id, saleDate)
            .numberTransaction(numberTransaction)
            .amounts(discountAmount, salesAmount, amountToBePaid, restToPay, monnaie)
            .statut(statut, paymentStatus)
            .saleInfo(natureVente, typePrescription, commentaire)
            .flags(differe, canceled)
            .dates(createdAt, updatedAt)
            .tvaEmbeded(tvaEmbeded)
            .thirdPartyInfo(numBon, partAssure, partTiersPayant)
            .user(userFullName)
            .seller(sellerId, sellerFirstName, sellerLastName)
            .cassier(cassierId, cassierFirstName, cassierLastName)
            .caisses(caisseNum, caisseEndNum)
            .customer(customerId, firstName, lastName, phone)
            .build();
    }

    private static Integer asInteger(Object o) {
        if (o == null) {
            return null;
        }
        return ((Number) o).intValue();
    }

    private static LocalDate toLocalDate(Object o) {
        return switch (o) {
            case null -> null;
            case LocalDate ld -> ld;
            case java.sql.Date d -> d.toLocalDate();
            default -> (LocalDate) o;
        };
    }

    private static LocalDateTime toLocalDateTime(Object o) {
        return switch (o) {
            case null -> null;
            case LocalDateTime ldt -> ldt;
            case java.sql.Timestamp ts -> ts.toLocalDateTime();
            default -> (LocalDateTime) o;
        };
    }

    public List<SaleDTO> customerPurchases(Integer customerId, LocalDate fromDate,
                                           LocalDate toDate) {
        Specification<Sales> specification = Specification.where(
            salesRepository.filterByCustomerId(customerId));
        specification = specification.and(
            salesRepository.hasStatut(EnumSet.of(SalesStatut.CLOSED)));
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

    public SaleDTO fetchPurchaseBy(@NotNull Long id, @NotNull LocalDate saleDate) {
        return fetch(new SaleId(id, saleDate), false).orElseThrow(
            () -> new RuntimeException("Sale not found"));
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
        return storageService.getUser();
    }

    public List<SaleDTO> allPrevente(String query, String type, Integer userId,
                                     Set<SalesStatut> statuts,
                                     LocalDate fromDate, LocalDate toDate, boolean excludeDepot) {
        if (StringUtils.isEmpty(type) || type.equals(EntityConstant.TOUT)) {
            return allPreventes(query, userId, statuts, fromDate, toDate, excludeDepot);
        }
        if (type.equals(EntityConstant.VNO)) {
            return allPreventeVNO(query, userId, statuts, fromDate, toDate);
        } else {
            return allPreventeVO(query, userId, statuts, fromDate, toDate, excludeDepot);
        }
    }

    public List<SaleDTO> allPreventeVNO(String query, Integer userId, Set<SalesStatut> statuts,
                                        LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        Root<CashSale> cashSaleRoot = cb.treat(root, CashSale.class);
        cq.select(cashSaleRoot).distinct(true).orderBy(cb.desc(root.get(CashSale_.updatedAt)));
        List<Predicate> predicates = new ArrayList<>();
        predicatesPrevente(query, predicates, cb, root, userId, statuts, fromDate, toDate, true);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).toList();
    }

    public List<SaleDTO> allPreventes(String query, Integer userId, Set<SalesStatut> statuts,
                                      LocalDate fromDate, LocalDate toDate, boolean excludeDepot) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get(Sales_.updatedAt)));
        List<Predicate> predicates = new ArrayList<>();
        predicatesPrevente(query, predicates, cb, root, userId, statuts, fromDate, toDate, excludeDepot);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).toList();
    }

    public List<SaleDTO> allPreventeVO(String query, Integer userId, Set<SalesStatut> statuts,
                                       LocalDate fromDate, LocalDate toDate, boolean excludeDepot) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        Root<ThirdPartySales> thirdPartySalesRoot = cb.treat(root, ThirdPartySales.class);
        cq.select(thirdPartySalesRoot).distinct(true)
            .orderBy(cb.desc(root.get(ThirdPartySales_.updatedAt)));
        List<Predicate> predicates = new ArrayList<>();
        predicatesPrevente(query, predicates, cb, root, userId, statuts, fromDate, toDate, excludeDepot);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(this::buildSaleDTO).toList();
    }

    private void predicatesPrevente(String query, List<Predicate> predicates, CriteriaBuilder cb,
                                    Root<Sales> root, Integer userId, Set<SalesStatut> statuts, LocalDate fromDate,
                                    LocalDate toDate, boolean excludeDepot) {
        Join<Sales, AppUser> userJoin = root.join(Sales_.user);
        Join<AppUser, Magasin> magasinJoin = userJoin.join(AppUser_.magasin);
        predicates.add(
            cb.equal(magasinJoin, getUser().getMagasin()));
        if (StringUtils.isNotEmpty(query)) {
            query = query.toUpperCase() + "%";
            SetJoin<Sales, SalesLine> lineSetJoin = root.joinSet(Sales_.SALES_LINES);
            Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
            SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(
                Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
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
            Join<Sales, AppUser> sellerJoin = root.join(Sales_.seller);
            predicates.add(cb.equal(sellerJoin.get(AppUser_.id), userId));
        }

        predicates.add(root.get(Sales_.statut).in(statuts));
        predicates.add(cb.between(root.get(Sales_.saleDate), fromDate, toDate));
        if (excludeDepot) {
            predicates.add(cb.notEqual(root.type(), VenteDepot.class));
        }
    }

    public Page<DepotExtensionSaleDTO> fetchVenteDepot(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Integer userId,
        PaymentStatus paymentStatus,
        Integer depotId,
        Pageable pageable
    ) {
        Set<CategorieChiffreAffaire> categorieChiffreAffaires = Set.of(
            CategorieChiffreAffaire.CA_DEPOT);
        var totalCount = countVentesDepot(search, fromDate, toDate, userId, depotId, paymentStatus,
            categorieChiffreAffaires);
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
            root,
            saleJoinDepot,
            categorieChiffreAffaires,
            depotId
        );
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<VenteDepot> q = em.createQuery(cq);

        if (pageable != null) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }

        List<VenteDepot> results = q.getResultList();

        return new PageImpl<>(results.stream().map(this::buildDepotExtensionSaleDTO).toList(),
            pageable, totalCount);
    }

    public Page<SaleDTO> listVenteTerminees(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        String fromHour,
        String toHour,
        Boolean global,
        Integer userId,
        String type,
        PaymentStatus paymentStatus,
        Boolean isDiffere,
        Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        Pageable pageable
    ) {
        long userMagasinId = getUser().getMagasin().getId();
        if (CollectionUtils.isEmpty(categorieChiffreAffaires)) {
            categorieChiffreAffaires = Set.of(
                CategorieChiffreAffaire.CA,
                CategorieChiffreAffaire.CALLEBASE,
                CategorieChiffreAffaire.TO_IGNORE
            );
        }

        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        String caIn = String.join(", ", categorieChiffreAffaires.stream()
            .map(ca -> "'" + ca.name() + "'").toList());
        conditions.add("s.ca IN (" + caIn + ")");
        conditions.add("s.statut IN ('CLOSED', 'CANCELED')");
        conditions.add("m.id = :magasinId");
        params.put("magasinId", userMagasinId);

        if (StringUtils.isNotEmpty(type) && !type.equals(EntityConstant.TOUT)) {
            conditions.add(type.equals(EntityConstant.VO)
                ? "s.dtype = 'ThirdPartySales'" : "s.dtype = 'CashSale'");
        }

        if (fromDate != null && toDate != null) {
            conditions.add("s.sale_date BETWEEN :fromDate AND :toDate");
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }

        if (Boolean.TRUE.equals(global)) {
            if (StringUtils.isNotEmpty(search)) {
                conditions.add(
                    "EXISTS (SELECT 1 FROM sales_line sl" +
                        " JOIN produit p ON sl.produit_id = p.id" +
                        " LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id" +
                        " WHERE sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date" +
                        " AND (UPPER(p.libelle) LIKE :search" +
                        " OR p.code_ean_labo LIKE :search" +
                        " OR fp.code_cip LIKE :search" +
                        " OR fp.code_ean LIKE :search))"
                );
                params.put("search", search.toUpperCase() + "%");
            }
            if (isValidHour(fromHour, toHour)) {
                conditions.add("TO_CHAR(s.updated_at, 'HH24:MI') BETWEEN :fromHour AND :toHour");
                params.put("fromHour", fromHour + ":00");
                params.put("toHour", toHour + ":59");
            }
        } else {
            if (StringUtils.isNotEmpty(search)) {
                conditions.add("s.number_transaction LIKE :refSearch");
                params.put("refSearch", search.toUpperCase() + "%");
            }
        }

        if (userId != null) {
            conditions.add("(cas.id = :userId OR u.id = :userId OR sel.id = :userId)");
            params.put("userId", userId);
        }

        if (paymentStatus != null && !paymentStatus.equals(PaymentStatus.ALL)) {
            conditions.add("s.payment_status = :paymentStatus");
            params.put("paymentStatus", paymentStatus.name());
        }

        if (isDiffere != null) {
            conditions.add("s.differe = :isDiffere");
            params.put("isDiffere", isDiffere);
        }

        String whereClause = " WHERE " + String.join(" AND ", conditions);

        String baseJoins =
            " FROM sales s" +
                " JOIN app_user u ON s.user_id = u.id" +
                " JOIN magasin m ON u.magasin_id = m.id" +
                " JOIN app_user sel ON s.seller_id = sel.id" +
                " JOIN app_user cas ON s.caissier_id = cas.id";

        var countQuery = em.createNativeQuery(
            "SELECT COUNT(DISTINCT s.id)" + baseJoins + whereClause);
        params.forEach(countQuery::setParameter);
        long totalCount = ((Number) countQuery.getSingleResult()).longValue();

        if (totalCount == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
        }

        String selectClause =
            "SELECT s.dtype, s.id, s.sale_date, s.number_transaction," +
                " s.discount_amount, s.sales_amount, s.amount_to_be_paid, s.rest_to_pay," +
                " s.statut, s.payment_status, s.nature_vente, s.type_prescription," +
                " s.differe, s.canceled, s.created_at, s.updated_at," +
                " s.commentaire, s.monnaie, s.tvaembeded," +
                " s.num_bon, s.part_assure, s.part_tiers_payant," +
                " CONCAT(u.first_name, ' ', u.last_name)," +
                " sel.id, sel.first_name, sel.last_name," +
                " cas.id, cas.first_name, cas.last_name," +
                " p.poste_number, lp.poste_number,c.first_name,c.last_name,c.phone,c.id";

        String dataJoins = baseJoins +
            " LEFT JOIN poste p ON s.caisse_id = p.id" +
            " LEFT JOIN poste lp ON s.lastcaisse_id = lp.id" +
            " LEFT JOIN customer c ON s.customer_id = c.id";

        var dataQuery = em.createNativeQuery(
            selectClause + dataJoins + whereClause + " ORDER BY s.updated_at DESC"
        );
        params.forEach(dataQuery::setParameter);
        if (pageable != null) {
            dataQuery.setFirstResult((int) pageable.getOffset());
            dataQuery.setMaxResults(pageable.getPageSize());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        return new PageImpl<>(
            rows.stream().map(SaleDataService::mapRowToSaleDTO).toList(),
            pageable,
            totalCount
        );
    }

    private long countVentesDepot(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Integer userId,
        Integer depotId,
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
            root,
            saleJoinDepot,
            categorieChiffreAffaires,
            depotId
        );
        cq.select(cb.countDistinct(root));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        return q.getSingleResult();
    }

    private void buildDepotPredicatSaleLines(String query, CriteriaBuilder cb,
                                             List<Predicate> predicates, Root<VenteDepot> root) {
        if (StringUtils.isNotEmpty(query)) {
            if (StringUtils.isNotEmpty(query)) {
                query = query.toUpperCase() + "%";
                SetJoin<VenteDepot, SalesLine> lineSetJoin = root.joinSet(Sales_.SALES_LINES);
                Join<SalesLine, Produit> produitJoin = lineSetJoin.join(SalesLine_.produit);
                SetJoin<Produit, FournisseurProduit> fp = produitJoin.joinSet(
                    Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
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

    private boolean isValidHour(String fromHour, String toHour) {
        if (StringUtils.isEmpty(fromHour) || StringUtils.isEmpty(toHour)) {
            return false;
        }
        return !fromHour.equals(DEFAULT_HEURE_DEBUT) && !toHour.equals(DEFAULT_HEURE_FIN);
    }

    private void predicatesVentesDepot(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Integer userId,
        PaymentStatus paymentStatus,
        List<Predicate> predicates,
        CriteriaBuilder cb,
        Root<VenteDepot> root,
        Join<VenteDepot, Magasin> saleJoinDepot,
        Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        Integer depotId
    ) {
        if (depotId != null) {
            predicates.add(cb.equal(saleJoinDepot.get(Magasin_.id), depotId));
        }
        predicates.add(root.get(VenteDepot_.categorieChiffreAffaire).in(categorieChiffreAffaires));
        buildDepotPredicatSaleLines(search, cb, predicates, root);
        if (fromDate != null && toDate != null) {
            predicates.add(cb.between(root.get(VenteDepot_.saleDate), fromDate, toDate));
        }

        predicates.add(
            root.get(VenteDepot_.statut).in(Set.of(SalesStatut.CLOSED, SalesStatut.CANCELED)));
        predicates.add(
            cb.equal(root.get(VenteDepot_.user).get(AppUser_.magasin), getUser().getMagasin()));
        buildVenteDepotImpayePredicats(predicates, cb, root, paymentStatus);
    }

    private void buildVenteDepotImpayePredicats(
        List<Predicate> predicates,
        CriteriaBuilder cb,
        Root<VenteDepot> root,
        PaymentStatus paymentStatus
    ) {
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
                new ClientTiersPayantDTO(thirdPartySaleLine.getClientTiersPayant()).setNumBon(
                    thirdPartySaleLine.getNumBon())
            );
        });
        return Pair.of(thirdPartySaleLineDTOS, clientTiersPayantDTOS);
    }

    public List<SalesLine> getAllAvoirs() {
        return salesLineRepository.findAllByQuantityAvoirGreaterThan(0);
    }

    private ThirdPartySaleDTO buildFromEntity(ThirdPartySales thirdPartySales) {
        SaleId saleId = thirdPartySales.getId();
        List<ThirdPartySaleLine> tpsLines = thirdPartySaleLineRepository
            .findAllBySaleIdAndSaleSaleDate(saleId.getId(), saleId.getSaleDate());
        Pair<List<ThirdPartySaleLineDTO>, List<ClientTiersPayantDTO>> pair = buildTiersPayantDTOFromSale(
            tpsLines);
        String numBon = null;
        String num = null;
        for (ThirdPartySaleLine line : tpsLines) {
            ClientTiersPayant clientTiersPayant = line.getClientTiersPayant();
            if (clientTiersPayant.getPriorite() == PrioriteTiersPayant.R0) {
                numBon = line.getNumBon();
                num = clientTiersPayant.getNum();
                break;
            }
        }
        if (StringUtils.isEmpty(numBon) && !tpsLines.isEmpty()) {
            numBon = tpsLines.getFirst().getNumBon();
        }
        AssuredCustomer assuredCustomer = (AssuredCustomer) thirdPartySales.getCustomer();
        AssuredCustomerDTO customer = new AssuredCustomerDTO(assuredCustomer);
        if (StringUtils.isEmpty(num)) {
            Set<ClientTiersPayant> clientTiersPayants = assuredCustomer.getClientTiersPayants();
            if (!CollectionUtils.isEmpty(clientTiersPayants)) {
                Optional<ClientTiersPayant> clientTiersPayantOpt = clientTiersPayants.stream()
                    .filter(ctp -> ctp.getPriorite() == PrioriteTiersPayant.R0)
                    .findFirst();
                if (clientTiersPayantOpt.isPresent()) {
                    num = clientTiersPayantOpt.get().getNum();
                }
            }
        }

        customer.setNum(num);
        List<SaleLineDTO> salesLines = thirdPartySales.getSalesLines().stream()
            .map(SaleLineDTO::new)
            .sorted(Comparator.comparing(SaleLineDTO::getUpdatedAt, Comparator.reverseOrder()))
            .toList();
        List<PaymentDTO> payments = thirdPartySales.getPayments().stream()
            .map(PaymentDTO::new)
            .toList();
        return ThirdPartySaleDTO.from(thirdPartySales)
            .customer(customer)
            .customerId(customer.getId())
            .salesLines(salesLines)
            .payments(payments)
            .thirdPartySaleLines(pair.getLeft())
            .tiersPayants(pair.getRight())
            .numBon(numBon)
            .build();
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
            return receiptPrinterService.generateEscPosReceipt(
                new ThirdPartySaleDTO(thirdPartySales), isEdit);
        } else if (sales instanceof VenteDepot venteDepot) {
            return receiptPrinterService.generateEscPosReceipt(
                new DepotExtensionSaleDTO(venteDepot), isEdit);
        }
        return new byte[0];
    }

    public List<StockDepotExportDTO> exportVenteDepotStock(SaleId venteDepotId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StockDepotExportDTO> cq = cb.createQuery(StockDepotExportDTO.class);
        Root<SalesLine> root = cq.from(SalesLine.class);
        Join<SalesLine, Produit> produitJoin = root.join(SalesLine_.produit);
        Join<SalesLine, Sales> saleJoin = root.join(SalesLine_.sales);
        Join<Produit, FournisseurProduit> fournisseurProduitJoin = produitJoin.join(
            Produit_.fournisseurProduitPrincipal);
        cq
            .select(
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
            )
            .orderBy(cb.asc(produitJoin.get(Produit_.libelle)));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(saleJoin.get(Sales_.id), venteDepotId.getId()));
        predicates.add(cb.equal(saleJoin.get(Sales_.saleDate), venteDepotId.getSaleDate()));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<StockDepotExportDTO> q = em.createQuery(cq);
        return q.getResultList();
    }

    public long countPendingSales(Integer cashRegisterId) {
        Specification<Sales> specification = salesRepository.isActif().and(salesRepository.toDay())
            .and(salesRepository.notDepot());
        if (cashRegisterId != null) {
            specification = specification.and(
                salesRepository.hasCaissier(storageService.getUser()));
        }
        return salesRepository.count(specification);
    }

    private record SaleDTOBuilder(SaleDTO dto) {

        private SaleDTOBuilder(String dtype) {
            this(switch (dtype) {
                case "ThirdPartySales" -> new ThirdPartySaleDTO();
                case "VenteDepot" -> new DepotExtensionSaleDTO();
                default -> new CashSaleDTO();
            });
        }

        static SaleDTOBuilder of(String dtype) {
            return new SaleDTOBuilder(dtype);
        }

        SaleDTOBuilder id(Long id, LocalDate saleDate) {
            dto.setId(id);
            dto.setSaleId(new SaleId(id, saleDate));
            return this;
        }

        SaleDTOBuilder numberTransaction(String numberTransaction) {
            dto.setNumberTransaction(numberTransaction);
            return this;
        }

        SaleDTOBuilder amounts(Integer discountAmount, Integer salesAmount,
                               Integer amountToBePaid, Integer restToPay, Integer monnaie) {
            dto.setDiscountAmount(discountAmount);
            dto.setSalesAmount(salesAmount);
            int sa = salesAmount != null ? salesAmount : 0;
            int da = discountAmount != null ? discountAmount : 0;
            dto.setNetAmount(sa - da);
            dto.setAmountToBePaid(amountToBePaid);
            dto.setRestToPay(restToPay);
            dto.setMontantRendu(monnaie);
            return this;
        }

        SaleDTOBuilder statut(String statut, String paymentStatus) {
            dto.setStatut(SalesStatut.valueOf(statut));
            dto.setPaymentStatus(PaymentStatus.valueOf(paymentStatus));
            return this;
        }

        SaleDTOBuilder saleInfo(String natureVente, String typePrescription, String commentaire) {
            dto.setNatureVente(NatureVente.valueOf(natureVente));
            dto.setTypePrescription(TypePrescription.valueOf(typePrescription));
            dto.setCommentaire(commentaire);
            return this;
        }

        SaleDTOBuilder flags(boolean differe, boolean canceled) {
            dto.setDiffere(differe);
            dto.setCanceled(canceled);
            return this;
        }

        SaleDTOBuilder dates(LocalDateTime createdAt, LocalDateTime updatedAt) {
            dto.setCreatedAt(createdAt);
            dto.setUpdatedAt(updatedAt);
            return this;
        }

        SaleDTOBuilder tvaEmbeded(String tvaEmbeded) {
            dto.setTvaEmbededs(Util.transformTvaEmbeded(tvaEmbeded));
            return this;
        }

        SaleDTOBuilder thirdPartyInfo(String numBon, Integer partAssure, Integer partTiersPayant) {
            if (dto instanceof ThirdPartySaleDTO tp) {
                tp.setNumBon(numBon);
                tp.setPartAssure(partAssure);
                tp.setPartTiersPayant(partTiersPayant);
                tp.setCategorie("VO");
            }
            return this;
        }

        SaleDTOBuilder user(String userFullName) {
            dto.setUserFullName(userFullName);
            return this;
        }

        SaleDTOBuilder seller(Integer id, String firstName, String lastName) {
            if (id == null) {
                return this;
            }
            UserDTO seller = new UserDTO();
            seller.setId(id);
            seller.setFirstName(firstName);
            seller.setLastName(lastName);
            seller.setFullName(firstName + " " + lastName);
            seller.setAbbrName(firstName.charAt(0) + ". " + lastName);
            dto.setSeller(seller);
            dto.setSellerId(id);
            return this;
        }

        SaleDTOBuilder cassier(Integer id, String firstName, String lastName) {
            if (id == null) {
                return this;
            }
            UserDTO cassier = new UserDTO();
            cassier.setId(id);
            cassier.setFirstName(firstName);
            cassier.setLastName(lastName);
            cassier.setFullName(firstName + " " + lastName);
            cassier.setAbbrName(firstName.charAt(0) + ". " + lastName);
            dto.setCassier(cassier);
            dto.setCassierId(id);
            return this;
        }

        SaleDTOBuilder caisses(String caisseNum, String caisseEndNum) {
            dto.setCaisseNum(caisseNum);
            dto.setCaisseEndNum(caisseEndNum);
            return this;
        }


        SaleDTOBuilder customer(Integer customerId, String firstName, String lastName,
                                String phone) {
            if (nonNull(customerId)) {
                dto.setCustomerId(customerId);
                var customer = new CustomerDTO();
                customer.setId(customerId);
                customer.setFirstName(firstName);
                customer.setLastName(lastName);
                customer.setPhone(phone);
                customer.setFullName(firstName + " " + lastName);
                dto.setCustomer(customer);
            }

            return this;
        }

        SaleDTO build() {
            switch (dto) {
                case CashSaleDTO _ -> dto.setCategorie("VNO");
                case ThirdPartySaleDTO _ -> dto.setCategorie("VO");
                case DepotExtensionSaleDTO _ -> dto.setCategorie("DEPOT");
                default -> {
                }
            }
            return dto;
        }
    }
}
