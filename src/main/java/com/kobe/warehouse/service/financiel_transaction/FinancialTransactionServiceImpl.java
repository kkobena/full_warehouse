package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.PaymentTransaction_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.enumeration.Order;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseSum;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.service.utils.ServiceUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Transactional
@Service
public class FinancialTransactionServiceImpl implements FinancialTransactionService {

    private static final String FULL_BASE_COUNT_QUERY =
        """
        SELECT COUNT(*) as total FROM (SELECT s.id,s.updated_at as createdAt,s.number_transaction as numberTransaction,'vente' as typeMvt,s.dtype as typeTransaction,s.ca as ca,p.payment_mode_code as paymentModeCode,s.user_id as userId
         FROM  sales s join payment p ON s.id = p.sales_id JOIN payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id LEFT JOIN customer c ON s.customer_id = c.id   union
         SELECT  pt.id,pt.created_at as createdAt,'' as numberTransaction,'transaction' as typeMvt,pt.type_transaction as typeTransaction,pt.categorie_ca as ca,pt.payment_mode_code as paymentModeCode,pt.user_id as userId
         FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id
        JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code) as mvt
        """;
    private static final String SQL_FULL_BASE_QUERY =
        """
        SELECT * FROM (SELECT 'vente' as typeMvt, u.id AS userId,
                              s.id , p.paid_amount as amount,p.payment_mode_code as paymentModeCode,
                              md.libelle as paymentModeLibelle,s.dtype AS typeTransaction,concat(u.first_name,'-',u.last_name) as userFullName
                               ,concat(c.first_name,' ',c.last_name) as customerFullName,s.updated_at as createdAt,s.number_transaction
                                  as numberTransaction,s.num_bon as infoAssureur,s.statut as statut,s.ca as ca,
                              s.ht_amount as htAmount,s.discount_amount as discountAmount,s.net_amount as netAmount,
                              s.part_assure as partAssure,s.part_tiers_payant as part_tiers_payant,NOW() as transactionDate
                       FROM  sales s join payment p ON s.id = p.sales_id JOIN
         payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id
            LEFT JOIN customer c ON s.customer_id = c.id   union
          SELECT 'transaction' as typeMvt, pt.user_id AS userId, pt.id ,pt.amount AS amount,pt.payment_mode_code as paymentModeCode,
                              pmp.libelle as paymentModeLibelle,pt.type_transaction as typeTransaction,
                              concat(up.first_name,'-',up.last_name)
                                            as userFullName,concat(pc.first_name,' ',pc.last_name) as customerFullName,
                              pt.created_at as createdAt, '' as numberTransaction,
                              tp.name as infoAssureur,'NA' as statut,pt.categorie_ca as ca,0 as htAmount,0 as discountAmount,0 as netAmount,0 as partAssure,0 as part_tiers_payant,
                              pt.transaction_date as transactionDate
        FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp
          ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id
        JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code) as mvt
        """;

    private static final String USER_ID = " AND mvt.userId  = %d ";
    private static final String DATE = " WHERE mvt.createdAt BETWEEN '%s' AND '%s' ";
    private static final String SEARCH = " AND LOWER(mvt.numberTransaction) LIKE '%s' ";
    private static final String TYPE = " AND mvt.typeTransaction IN (%s) ";
    private static final String PAYMENT_MODE = " AND  mvt.paymentModeCode IN (%s) ";
    private static final String CATEGORIE_CHIFFRE_AFFAIRE = " AND mvt.ca IN (%s) ";
    private static final String SQL_FULL_BASE_QUERY_ORDER_BY = " ORDER BY mvt.createdAt %s";

    private static final String SALES_SUM_SQL_QUERY =
        """
        SELECT 'vente' as typeMvt, SUM(p.paid_amount) as amount,p.payment_mode_code as paymentModeCode,md.libelle as paymentModeLibelle,s.dtype AS typeTransaction FROM  sales s join payment p ON s.id = p.sales_id
        JOIN payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id LEFT JOIN customer c ON s.customer_id = c.id

        """;

    private static final String TRANSACTION_SUM_SQL_QUERY =
        """
        SELECT 'transaction' as typeMvt, SUM(pt.amount) AS amount,pt.payment_mode_code as paymentModeCode,pmp.libelle as paymentModeLibelle,pt.type_transaction as typeTransaction
        FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp  ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code
        """;
    private static final Logger log = LoggerFactory.getLogger(FinancialTransactionServiceImpl.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserService userService;
    private final CustomerRepository customerRepository;
    private final TiersPayantRepository tiersPayantRepository;
    private final FournisseurRepository fournisseurRepository;
    private final WarehouseCalendarService warehouseCalendarService;
    private final CashRegisterService cashRegisterService;
    private final EntityManager em;
    private final MvtCaisseReportReportService mvtCaisseReportService;

    public FinancialTransactionServiceImpl(
        PaymentTransactionRepository paymentTransactionRepository,
        UserService userService,
        CustomerRepository customerRepository,
        TiersPayantRepository tiersPayantRepository,
        FournisseurRepository fournisseurRepository,
        WarehouseCalendarService warehouseCalendarService,
        CashRegisterService cashRegisterService,
        EntityManager em,
        MvtCaisseReportReportService mvtCaisseReportService
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userService = userService;
        this.customerRepository = customerRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.warehouseCalendarService = warehouseCalendarService;
        this.cashRegisterService = cashRegisterService;
        this.em = em;
        this.mvtCaisseReportService = mvtCaisseReportService;
    }

    @Override
    public void create(FinancialTransactionDTO financialTransactionDTO) {
        PaymentTransaction paymentTransaction = fromDTO(financialTransactionDTO);
        paymentTransactionRepository.save(paymentTransaction);
    }

    @Override
    public Page<FinancialTransactionDTO> findAll(FinancielTransactionFilterDTO financielTransactionFilter, Pageable pageable) {
        long total = getCount(financielTransactionFilter);
        if (total == 0) {
            return Page.empty();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PaymentTransaction> cq = cb.createQuery(PaymentTransaction.class);
        Root<PaymentTransaction> root = cq.from(PaymentTransaction.class);
        cq.select(root).distinct(true).orderBy(cb.asc(root.get(PaymentTransaction_.createdAt)));
        List<Predicate> predicates = predicate(cb, root, financielTransactionFilter);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<PaymentTransaction> q = em.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<FinancialTransactionDTO> paymentTransactions = q.getResultList().stream().map(this::toDTO).toList();
        return new PageImpl<>(paymentTransactions, pageable, total);
    }

    @Override
    public Page<MvtCaisseDTO> findAll(TransactionFilterDTO transactionFilter, Pageable pageable) {
        long total = fetchSalesAndTransactionCount(transactionFilter);

        if (total == 0) {
            return Page.empty();
        }
        List<Tuple> tuples = fetchSalesAndTransaction(transactionFilter, pageable);
        List<MvtCaisseDTO> mvtCaisseDTOS = new ArrayList<>();
        tuples.forEach(tuple -> mvtCaisseDTOS.add(buildFromTuple(tuple)));
        return new PageImpl<>(mvtCaisseDTOS, pageable, total);
    }

    @Override
    public List<MvtCaisseDTO> findAll(TransactionFilterDTO transactionFilter) {
        List<Tuple> tuples = fetchSalesAndTransaction(transactionFilter, Pageable.unpaged());
        List<MvtCaisseDTO> mvtCaisseDTOS = new ArrayList<>();
        tuples.forEach(tuple -> mvtCaisseDTOS.add(buildFromTuple(tuple)));
        return mvtCaisseDTOS;
    }

    @Override
    public MvtCaisseWrapper getMvtCaisseSum(TransactionFilterDTO transactionFilter) {
        return buildMvtCaisseWrapper(transactionFilter);
    }

    @Override
    public Optional<FinancialTransactionDTO> findById(Long id) {
        return this.paymentTransactionRepository.findById(id).map(this::toDTO);
    }

    @Override
    public Resource exportToPdf(TransactionFilterDTO transactionFilter) throws IOException {
        List<MvtCaisseDTO> mvtCaisses = findAll(transactionFilter);
        MvtCaisseWrapper mvtCaisseWrapper = buildMvtCaisseWrapper(transactionFilter);
        Pair pair = buildPeriode(transactionFilter);
        return this.mvtCaisseReportService.exportToPdf(
                new ArrayList<>(mvtCaisses),
                mvtCaisseWrapper,
                new ReportPeriode(((LocalDateTime) pair.key()).toLocalDate(), ((LocalDateTime) pair.value()).toLocalDate())
            );
    }

    private PaymentTransaction fromDTO(FinancialTransactionDTO financialTransaction) {
        var user = userService.getUser();
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setCreatedAt(LocalDateTime.now());
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        paymentTransaction.setCashRegister(cashRegister);
        paymentTransaction.setCalendar(this.warehouseCalendarService.initCalendar());
        paymentTransaction.setAmount(financialTransaction.getAmount());
        paymentTransaction.setPaymentMode(financialTransaction.getPaymentMode());
        if (Objects.nonNull(financialTransaction.getTransactionDate())) {
            paymentTransaction.setTransactionDate(financialTransaction.getTransactionDate());
        }
        paymentTransaction.setTypeFinancialTransaction(financialTransaction.getTypeTransaction());
        paymentTransaction.setOrganismeId(financialTransaction.getOrganismeId());
        paymentTransaction.setUser(userService.getUser());
        switch (financialTransaction.getTypeTransaction()) {
            case REGLEMENT_DIFFERE, REGLEMENT_TIERS_PAYANT, ENTREE_CAISSE:
                paymentTransaction.setCredit(true);
                break;
            case SORTIE_CAISSE, REGLMENT_FOURNISSEUR:
                paymentTransaction.setCredit(false);
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + financialTransaction.getTypeTransaction());
        }
        return paymentTransaction;
    }

    private FinancialTransactionDTO toDTO(PaymentTransaction paymentTransaction) {
        FinancialTransactionDTO financialTransactionDTO = new FinancialTransactionDTO();
        financialTransactionDTO.setAmount(paymentTransaction.getAmount());
        financialTransactionDTO.setPaymentMode(paymentTransaction.getPaymentMode());
        financialTransactionDTO.setTransactionDate(paymentTransaction.getTransactionDate());
        financialTransactionDTO.setTypeFinancialTransaction(paymentTransaction.getTypeFinancialTransaction());
        financialTransactionDTO.setOrganismeId(paymentTransaction.getOrganismeId());
        financialTransactionDTO.setCreatedAt(paymentTransaction.getCreatedAt());
        financialTransactionDTO.setCredit(paymentTransaction.isCredit());
        switch (paymentTransaction.getTypeFinancialTransaction()) {
            case REGLEMENT_DIFFERE:
                Customer customer = this.customerRepository.getReferenceById(paymentTransaction.getOrganismeId());
                financialTransactionDTO.setOrganismeName(customer.getFirstName().concat(" ").concat(customer.getLastName()));
                break;
            case REGLEMENT_TIERS_PAYANT:
                financialTransactionDTO.setOrganismeName(
                    tiersPayantRepository.getReferenceById(paymentTransaction.getOrganismeId()).getName()
                );
                break;
            case REGLMENT_FOURNISSEUR:
                financialTransactionDTO.setOrganismeName(
                    fournisseurRepository.getReferenceById(paymentTransaction.getOrganismeId()).getLibelle()
                );
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + paymentTransaction.getTypeFinancialTransaction());
        }
        var user = paymentTransaction.getUser();
        financialTransactionDTO.setUserFullName(user.getFirstName().concat(" ").concat(user.getLastName()));
        return financialTransactionDTO;
    }

    private long getCount(FinancielTransactionFilterDTO transactionFilter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<PaymentTransaction> root = cq.from(PaymentTransaction.class);
        cq.select(cb.countDistinct(root));
        List<Predicate> predicates = predicate(cb, root, transactionFilter);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    private List<Predicate> predicate(CriteriaBuilder cb, Root<PaymentTransaction> root, FinancielTransactionFilterDTO transactionFilter) {
        List<Predicate> predicates = new ArrayList<>();

        if (!ObjectUtils.isEmpty(transactionFilter.typeFinancialTransaction())) {
            predicates.add(cb.equal(root.get(PaymentTransaction_.typeFinancialTransaction), transactionFilter.typeFinancialTransaction()));
        }
        if (!ObjectUtils.isEmpty(transactionFilter.organismeId())) {
            predicates.add(cb.equal(root.get(PaymentTransaction_.organismeId), transactionFilter.organismeId()));
        }
        if (Objects.nonNull(transactionFilter.userId())) {
            predicates.add(cb.equal(root.get(PaymentTransaction_.user).get("id"), transactionFilter.userId()));
        }
        if (Objects.nonNull(transactionFilter.fromDate()) && Objects.nonNull(transactionFilter.toDate())) {
            predicates.add(
                cb.between(
                    root.get(PaymentTransaction_.createdAt),
                    LocalDateTime.of(transactionFilter.fromDate(), LocalTime.MIN),
                    LocalDateTime.of(transactionFilter.toDate(), LocalTime.MAX)
                )
            );
        }

        if (transactionFilter.categorieChiffreAffaire() != null) {
            predicates.add(cb.equal(root.get(PaymentTransaction_.categorieChiffreAffaire), transactionFilter.categorieChiffreAffaire()));
        }
        if (transactionFilter.paymentMode() != null) {
            predicates.add(cb.equal(root.get(PaymentTransaction_.paymentMode).get("code"), transactionFilter.paymentMode()));
        }

        return predicates;
    }

    private Pair buildPeriode(TransactionFilterDTO financielTransactionFilter) {
        LocalDateTime fromDate = Objects.nonNull(financielTransactionFilter.fromDate())
            ? LocalDateTime.of(financielTransactionFilter.fromDate(), LocalTime.MIN)
            : LocalDate.now().atStartOfDay();
        LocalDateTime toDate = Objects.nonNull(financielTransactionFilter.toDate())
            ? LocalDateTime.of(financielTransactionFilter.toDate(), LocalTime.MAX)
            : LocalDateTime.now();
        return new Pair(fromDate, toDate);
    }

    private String getWhereClause(TransactionFilterDTO financielTransactionFilter) {
        StringBuilder where = new StringBuilder();
        Pair periode = buildPeriode(financielTransactionFilter);
        LocalDateTime fromDate = (LocalDateTime) periode.key();
        LocalDateTime toDate = (LocalDateTime) periode.value();

        where.append(String.format(DATE, fromDate, toDate));
        if (financielTransactionFilter.userId() != null) {
            where.append(String.format(USER_ID, financielTransactionFilter.userId()));
        }
        if (StringUtils.hasText(financielTransactionFilter.search())) {
            String search = "%" + financielTransactionFilter.search().toLowerCase() + "%";
            where.append(String.format(SEARCH, search));
        }

        if (
            financielTransactionFilter.typeFinancialTransactions() != null &&
            !financielTransactionFilter.typeFinancialTransactions().isEmpty()
        ) {
            var type = convertAllTypeToString(financielTransactionFilter);
            var typesVente = getTypeVentes(financielTransactionFilter);

            if (!typesVente.isEmpty()) {
                if (!type.isEmpty()) {
                    where.append(String.format(TYPE, type.concat(",").concat(typesVente)));
                } else {
                    where.append(String.format(TYPE, typesVente));
                }
            } else {
                if (!type.isEmpty()) {
                    where.append(String.format(TYPE, type));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(financielTransactionFilter.paymentModes())) {
            String paymentModes = financielTransactionFilter
                .paymentModes()
                .stream()
                .map(e -> "'" + e + "'")
                .collect(Collectors.joining(","));
            where.append(String.format(PAYMENT_MODE, paymentModes));
        }
        if (CollectionUtils.isNotEmpty(financielTransactionFilter.categorieChiffreAffaires())) {
            var categorieChiffreAffairesOrdinal = financielTransactionFilter
                .categorieChiffreAffaires()
                .stream()
                .map(e -> String.valueOf(e.ordinal()))
                .collect(Collectors.joining(","));
            var categorieChiffreAffaires = financielTransactionFilter
                .categorieChiffreAffaires()
                .stream()
                .map(e -> "'" + e.name() + "'")
                .collect(Collectors.joining(","));
            var ca = String.format(CATEGORIE_CHIFFRE_AFFAIRE, categorieChiffreAffairesOrdinal.concat(",").concat(categorieChiffreAffaires));
            where.append(ca);
        }

        return where.toString();
    }

    private Set<Integer> getTypeTransaction(Set<TypeFinancialTransaction> typeFinancialTransactions) {
        return typeFinancialTransactions
            .stream()
            .filter(
                type ->
                    type == TypeFinancialTransaction.REGLEMENT_DIFFERE ||
                    type == TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT ||
                    type == TypeFinancialTransaction.ENTREE_CAISSE ||
                    type == TypeFinancialTransaction.SORTIE_CAISSE ||
                    type == TypeFinancialTransaction.REGLMENT_FOURNISSEUR
            )
            .map(TypeFinancialTransaction::ordinal)
            .collect(Collectors.toSet());
    }

    private Set<String> getTypeVente(Set<TypeFinancialTransaction> typeFinancialTransactions) {
        boolean asSaleType =
            typeFinancialTransactions.contains(TypeFinancialTransaction.CASH_SALE) ||
            typeFinancialTransactions.contains(TypeFinancialTransaction.CREDIT_SALE) ||
            typeFinancialTransactions.contains(TypeFinancialTransaction.VENTES_DEPOTS);
        if (asSaleType) {
            Set<String> financialTransactions = typeFinancialTransactions
                .stream()
                .filter(
                    type ->
                        type == TypeFinancialTransaction.CASH_SALE ||
                        type == TypeFinancialTransaction.CREDIT_SALE ||
                        type == TypeFinancialTransaction.VENTES_DEPOTS
                )
                .map(TypeFinancialTransaction::name)
                .collect(Collectors.toSet());

            return Stream.of(TypeVente.values())
                .filter(e -> financialTransactions.contains(e.name()))
                .map(e -> "'" + e.getValue() + "'")
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private String getOrderByClause(TransactionFilterDTO financielTransactionFilter) {
        if (Objects.isNull(financielTransactionFilter.order())) {
            Order order = Order.DESC;
            return String.format(SQL_FULL_BASE_QUERY_ORDER_BY, order.name());
        }
        return String.format(SQL_FULL_BASE_QUERY_ORDER_BY, financielTransactionFilter.order().name());
    }

    private String getFullBaseQuery(TransactionFilterDTO financielTransactionFilter) {
        return SQL_FULL_BASE_QUERY + getWhereClause(financielTransactionFilter) + getOrderByClause(financielTransactionFilter);
    }

    private String getSalesSumSqlQuery(TransactionFilterDTO financielTransactionFilter) {
        return SALES_SUM_SQL_QUERY + getSaleSumWhereClause(financielTransactionFilter) + " GROUP by paymentModeCode,typeTransaction";
    }

    private String getTransactionSumSqlQuery(TransactionFilterDTO financielTransactionFilter) {
        return (
            TRANSACTION_SUM_SQL_QUERY +
            getTransactionSumWhereClause(financielTransactionFilter) +
            " GROUP by paymentModeCode,typeTransaction"
        );
    }

    private String getFullBaseCountQuery(TransactionFilterDTO financielTransactionFilter) {
        return FULL_BASE_COUNT_QUERY + getWhereClause(financielTransactionFilter);
    }

    private List<Tuple> fetchSalesAndTransaction(TransactionFilterDTO financielTransactionFilter, Pageable pageable) {
        if (pageable.isPaged()) {
            return em
                .createNativeQuery(getFullBaseQuery(financielTransactionFilter), Tuple.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        }
        return em.createNativeQuery(getFullBaseQuery(financielTransactionFilter), Tuple.class).getResultList();
    }

    private long fetchSalesAndTransactionCount(TransactionFilterDTO financielTransactionFilter) {
        return ((Number) em.createNativeQuery(getFullBaseCountQuery(financielTransactionFilter)).getSingleResult()).longValue();
    }

    private List<Tuple> fetchSalesAndTransactionTotaux(TransactionFilterDTO financielTransactionFilter) {
        return em.createNativeQuery(getSalesSumSqlQuery(financielTransactionFilter), Tuple.class).getResultList();
    }

    private List<MvtCaisseSum> buildSalesAndTransactionTotaux(TransactionFilterDTO financielTransactionFilter) {
        return fetchSalesAndTransactionTotaux(financielTransactionFilter)
            .stream()
            .map(this::buildSumFromTuple)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private List<Tuple> fetchTransactionSum(TransactionFilterDTO financielTransactionFilter) {
        return em.createNativeQuery(getTransactionSumSqlQuery(financielTransactionFilter), Tuple.class).getResultList();
    }

    private List<MvtCaisseSum> buildTransactionSum(TransactionFilterDTO financielTransactionFilter) {
        return fetchTransactionSum(financielTransactionFilter)
            .stream()
            .map(this::buildSumFromTuple)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Optional<MvtCaisseSum> buildSumFromTuple(Tuple tuple) {
        if (Objects.isNull(tuple.get("amount"))) {
            return Optional.empty();
        }
        MvtCaisseSum mvtCaisseSum = new MvtCaisseSum();
        if (tuple.get("amount") instanceof BigDecimal) {
            mvtCaisseSum.setAmount(tuple.get("amount", BigDecimal.class));
        } else {
            mvtCaisseSum.setAmount(BigDecimal.valueOf(tuple.get("amount", Double.class)));
        }
        mvtCaisseSum.setPaymentModeLibelle(tuple.get("paymentModeLibelle", String.class));
        mvtCaisseSum.setPaymentModeCode(tuple.get("paymentModeCode", String.class));
        mvtCaisseSum.setType(tuple.get("typeMvt", String.class));

        if (mvtCaisseSum.getType().equals("vente")) {
            TypeVente typeVente = TypeVente.fromValue(tuple.get("typeTransaction", String.class));
            mvtCaisseSum.setTypeTransaction(TypeFinancialTransaction.valueOf(typeVente.name()));
        }
        if (mvtCaisseSum.getType().equals("transaction")) {
            mvtCaisseSum.setTypeTransaction(TypeFinancialTransaction.values()[tuple.get("typeTransaction", Byte.class)]);
        }
        mvtCaisseSum.setTransactionTypeAffichage(mvtCaisseSum.getTypeTransaction().getTransactionTypeAffichage());
        return Optional.of(mvtCaisseSum);
    }

    private MvtCaisseDTO buildFromTuple(Tuple tuple) {
        MvtCaisseDTO mvtCaisseDTO = new MvtCaisseDTO();
        mvtCaisseDTO.setId(tuple.get("id", Long.class));
        if (tuple.get("typeMvt", String.class).equals("vente")) {
            TypeVente typeVente = TypeVente.fromValue(tuple.get("typeTransaction", String.class));
            mvtCaisseDTO.setType(TypeFinancialTransaction.valueOf(typeVente.name()));
            mvtCaisseDTO.setCategorieChiffreAffaire(CategorieChiffreAffaire.valueOf(tuple.get("ca", String.class)));
            mvtCaisseDTO.setStatut(SalesStatut.valueOf(tuple.get("statut", String.class)));
            mvtCaisseDTO.setDiscount(tuple.get("discountAmount", Integer.class));
            mvtCaisseDTO.setHtAmount(tuple.get("htAmount", Integer.class));
            mvtCaisseDTO.setNetAmount(tuple.get("netAmount", Integer.class));
            mvtCaisseDTO.setPartAssure(tuple.get("partAssure", Integer.class));
            mvtCaisseDTO.setPartAssureur(tuple.get("part_tiers_payant", Integer.class));
        }
        mvtCaisseDTO.setMontant(tuple.get("amount", Integer.class));
        mvtCaisseDTO.setOrganisme(tuple.get("customerFullName", String.class));
        String[] usersNames = tuple.get("userFullName", String.class).split("-");
        mvtCaisseDTO.setUserFullName(usersNames[0].charAt(0) + "".toUpperCase() + ". " + usersNames[1]);
        try {
            mvtCaisseDTO.setReference(tuple.get("numberTransaction", String.class));
        } catch (Exception e) {
            log.error("Error while setting reference", e);
        }

        mvtCaisseDTO.setPaymentMode(tuple.get("paymentModeCode", String.class));
        mvtCaisseDTO.setPaymentModeLibelle(tuple.get("paymentModeLibelle", String.class));
        mvtCaisseDTO.setDate(DateUtil.format(tuple.get("createdAt", Timestamp.class)));
        mvtCaisseDTO.setNumBon(tuple.get("infoAssureur", String.class));
        if (tuple.get("typeMvt", String.class).equals("transaction")) {
            mvtCaisseDTO.setType(TypeFinancialTransaction.values()[Integer.parseInt(tuple.get("typeTransaction", String.class))]);
            mvtCaisseDTO.setCategorieChiffreAffaire(CategorieChiffreAffaire.values()[Integer.parseInt(tuple.get("ca", String.class))]);
            mvtCaisseDTO.setTransactionDate(DateUtil.format(tuple.get("transactionDate", Timestamp.class)));
        }
        return mvtCaisseDTO;
    }

    private String getSaleSumWhereClause(TransactionFilterDTO financielTransactionFilter) {
        StringBuilder where = new StringBuilder();
        Pair periode = buildPeriode(financielTransactionFilter);
        LocalDateTime fromDate = (LocalDateTime) periode.key();
        LocalDateTime toDate = (LocalDateTime) periode.value();

        where.append(String.format(" WHERE s.updated_at BETWEEN '%s' AND '%s' ", fromDate, toDate));
        if (financielTransactionFilter.userId() != null) {
            where.append(String.format(" AND s.user_id=%d ", financielTransactionFilter.userId()));
        }
        if (StringUtils.hasText(financielTransactionFilter.search())) {
            String search = "%" + financielTransactionFilter.search().toLowerCase() + "%";
            where.append(String.format(" AND s.number_transaction LIKE '%s' ", search));
        }

        if (
            financielTransactionFilter.typeFinancialTransactions() != null &&
            !financielTransactionFilter.typeFinancialTransactions().isEmpty()
        ) {
            var typesVente = getTypeVentes(financielTransactionFilter);
            if (!typesVente.isEmpty()) {
                where.append(String.format(" AND s.dtype IN (%s) ", typesVente));
            }
        }

        if (CollectionUtils.isNotEmpty(financielTransactionFilter.paymentModes())) {
            String paymentModes = financielTransactionFilter
                .paymentModes()
                .stream()
                .map(e -> "'" + e + "'")
                .collect(Collectors.joining(","));
            where.append(String.format(" AND md.code IN (%s) ", paymentModes));
        }
        if (CollectionUtils.isNotEmpty(financielTransactionFilter.categorieChiffreAffaires())) {
            var categorieChiffreAffaires = financielTransactionFilter
                .categorieChiffreAffaires()
                .stream()
                .map(e -> "'" + e.name() + "'")
                .collect(Collectors.joining(","));
            var ca = String.format(" AND s.ca IN (%s) ", categorieChiffreAffaires);
            where.append(ca);
        }

        return where.toString();
    }

    private String convertAllTypeToString(TransactionFilterDTO financielTransactionFilter) {
        return getTypeTransaction(financielTransactionFilter.typeFinancialTransactions())
            .stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    private String getTypeVentes(TransactionFilterDTO financielTransactionFilter) {
        return String.join(",", getTypeVente(financielTransactionFilter.typeFinancialTransactions()));
    }

    private String getTransactionSumWhereClause(TransactionFilterDTO financielTransactionFilter) {
        StringBuilder where = new StringBuilder();
        Pair periode = buildPeriode(financielTransactionFilter);
        LocalDateTime fromDate = (LocalDateTime) periode.key();
        LocalDateTime toDate = (LocalDateTime) periode.value();

        where.append(String.format(" WHERE pt.created_at BETWEEN '%s' AND '%s' ", fromDate, toDate));
        if (financielTransactionFilter.userId() != null) {
            where.append(String.format(" AND pt.user_id=%d ", financielTransactionFilter.userId()));
        }

        if (
            financielTransactionFilter.typeFinancialTransactions() != null &&
            !financielTransactionFilter.typeFinancialTransactions().isEmpty()
        ) {
            var type = convertAllTypeToString(financielTransactionFilter);

            if (!type.isEmpty()) {
                where.append(String.format(" AND pt.type_transaction IN (%s) ", type));
            }
        }

        if (CollectionUtils.isNotEmpty(financielTransactionFilter.paymentModes())) {
            String paymentModes = financielTransactionFilter
                .paymentModes()
                .stream()
                .map(e -> "'" + e + "'")
                .collect(Collectors.joining(","));
            where.append(String.format(" AND pmp.code IN (%s) ", paymentModes));
        }
        if (CollectionUtils.isNotEmpty(financielTransactionFilter.categorieChiffreAffaires())) {
            var categorieChiffreAffairesOrdinal = financielTransactionFilter
                .categorieChiffreAffaires()
                .stream()
                .map(e -> String.valueOf(e.ordinal()))
                .collect(Collectors.joining(","));
            var ca = String.format(" AND s.ca IN (%s) ", categorieChiffreAffairesOrdinal);
            where.append(ca);
        }

        return where.toString();
    }

    private MvtCaisseWrapper buildMvtCaisseWrapper(TransactionFilterDTO transactionFilter) {
        MvtCaisseWrapper mvtCaisseWrapper = new MvtCaisseWrapper();
        List<MvtCaisseSum> mvtCaisseSums = buildSalesAndTransactionTotaux(transactionFilter);
        Set<TypeFinancialTransaction> typeFinancialTransactions = transactionFilter.typeFinancialTransactions();
        if (Objects.isNull(typeFinancialTransactions) || typeFinancialTransactions.isEmpty()) {
            mvtCaisseSums.addAll(buildTransactionSum(transactionFilter));
        } else {
            if (!getTypeTransaction(typeFinancialTransactions).isEmpty()) {
                mvtCaisseSums.addAll(buildTransactionSum(transactionFilter));
            }
        }

        BigDecimal totalPaymentAmount = new BigDecimal(0);
        BigDecimal totalMobileAmount = new BigDecimal(0);
        BigDecimal creditedAmount = new BigDecimal(0);
        BigDecimal debitedAmount = new BigDecimal(0);
        List<com.kobe.warehouse.service.dto.records.Tuple> typeTransactionAmounts = new ArrayList<>();
        List<com.kobe.warehouse.service.dto.records.Tuple> modesPaiementAmounts = new ArrayList<>();
        List<MvtCaisseSum> groupingByMode = new ArrayList<>();
        for (Entry<TransactionTypeAffichage, List<MvtCaisseSum>> transactionTypeAffichageListEntry : mvtCaisseSums
            .stream()
            .collect(Collectors.groupingBy(MvtCaisseSum::getTransactionTypeAffichage))
            .entrySet()) {
            TransactionTypeAffichage key = transactionTypeAffichageListEntry.getKey();
            List<MvtCaisseSum> value = transactionTypeAffichageListEntry.getValue();
            BigDecimal typeAmount = new BigDecimal(0);
            groupingByMode.addAll(value);
            for (MvtCaisseSum mvtCaisseSum : value) {
                typeAmount = typeAmount.add(mvtCaisseSum.getAmount());

                if (ServiceUtil.isPaymentMode(mvtCaisseSum.getPaymentModeCode())) {
                    totalMobileAmount = totalMobileAmount.add(mvtCaisseSum.getAmount());
                }
            }
            switch (key) {
                case VNO, VO:
                    totalPaymentAmount = totalPaymentAmount.add(typeAmount);
                    break;
                case ENTREE_CAISSE, REGLEMENT_DIFFERE, REGLEMENT_TIERS_PAYANT:
                    creditedAmount = creditedAmount.add(typeAmount);
                    break;
                case SORTIE_CAISSE, REGLEMENT_FOURNISSEUR:
                    debitedAmount = debitedAmount.add(typeAmount);
                    break;
                default:
                    break;
            }
            typeTransactionAmounts.add(new com.kobe.warehouse.service.dto.records.Tuple(key.name(), key.getValue(), typeAmount));
        }
        groupingByMode
            .stream()
            .collect(Collectors.groupingBy(MvtCaisseSum::getPaymentModeCode))
            .forEach((k, v) -> {
                BigDecimal amount = v.stream().map(MvtCaisseSum::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                modesPaiementAmounts.add(new com.kobe.warehouse.service.dto.records.Tuple(k, v.getFirst().getPaymentModeLibelle(), amount));
            });
        mvtCaisseWrapper.setCreditedAmount(creditedAmount);
        mvtCaisseWrapper.setDebitedAmount(debitedAmount);
        mvtCaisseWrapper.setTotalMobileAmount(totalMobileAmount);
        mvtCaisseWrapper.setTotalPaymentAmount(totalPaymentAmount);
        mvtCaisseWrapper.setTypeTransactionAmounts(typeTransactionAmounts);
        mvtCaisseWrapper.setModesPaiementAmounts(modesPaiementAmounts);
        return mvtCaisseWrapper;
    }
}
