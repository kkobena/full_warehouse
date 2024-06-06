package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.PaymentTransaction_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
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
import com.kobe.warehouse.service.dto.enumeration.Order;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseSum;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
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
SELECT COUNT(*) as total FROM (SELECT s.id,s.updated_at as createdAt,s.number_transaction as numberTransaction,'vente' as typeMvt,s.dtype as typeTransaction,s.ca as ca,p.payment_mode_code as paymentModeCode
 FROM  sales s join payment p ON s.id = p.sales_id JOIN payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id LEFT JOIN customer c ON s.customer_id = c.id   union
 SELECT  pt.id,pt.created_at as createdAt,pt.ticket_code as numberTransaction,'transaction' as typeMvt,pt.type_transaction as typeTransaction,pt.categorie_ca as ca,pt.payment_mode_code as paymentModeCode
 FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id
JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code) as mvt
  """;
  private static final String SQL_FULL_BASE_QUERY =
      """
SELECT * FROM (SELECT 'vente' as typeMvt, u.id AS userId,
                      s.id , p.paid_amount as amount,p.payment_mode_code as paymentModeCode,
                      md.libelle as paymentModeLibelle,s.dtype AS typeTransaction,concat(u.first_name,' ',u.last_name) as userFullName
                       ,concat(c.first_name,' ',c.last_name) as customerFullName,s.updated_at as createdAt,s.number_transaction
                          as numberTransaction,s.num_bon as infoAssureur,s.statut as statut,s.ca as ca,
                      s.ht_amount as htAmount,s.discount_amount as discountAmount,s.net_amount as netAmount,
                      s.part_assure as partAssure,s.part_tiers_payant as part_tiers_payant,NOW() as transactionDate
               FROM  sales s join payment p ON s.id = p.sales_id JOIN
 payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id
    LEFT JOIN customer c ON s.customer_id = c.id   union
  SELECT 'transaction' as typeMvt, pt.user_id AS userId, pt.id ,pt.amount AS amount,pt.payment_mode_code as paymentModeCode,
                      pmp.libelle as paymentModeLibelle,pt.type_transaction as typeTransaction,
                      concat(up.first_name,' ',up.last_name)
                                    as userFullName,concat(pc.first_name,' ',pc.last_name) as customerFullName,
                      pt.created_at as createdAt, pt.ticket_code as numberTransaction,
                      tp.name as infoAssureur,'NA' as statut,pt.categorie_ca as ca,0 as htAmount,0 as discountAmount,0 as netAmount,0 as partAssure,0 as part_tiers_payant,
                      pt.transaction_date as transactionDate
FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp
  ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id
JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code) as mvt
""";
  private static final String SQL_ONLY_SALES =
      """
SELECT wc.work_day,
 s.id AS saleId, p.paid_amount as paidAmount,p.payment_mode_code as paymentModeCode,
 md.libelle as paymentModeLibelle,s.dtype AS typeVente,concat(u.first_name,' ',u.last_name) as userFullName
,concat(c.first_name,' ',c.last_name) as customerFullName,s.updated_at as saleDate,s.number_transaction
 as saleNumberTransaction,s.num_bon as saleNumBon,s.statut as saleStatut,s.ca as saleCa
FROM warehouse_calendar wc  JOIN sales s ON wc.work_day = s.calendar_work_day
 join payment p ON s.id = p.sales_id JOIN
     payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id
  LEFT JOIN customer c ON s.customer_id = c.id

""";

  private static final String SQL_ONLY_TRANSACTION_BASE =
      """
SELECT pt.categorie_ca AS transactionCategorieCa, pt.ticket_code as paymentTransactionTicketCode, pt.user_id AS transactionUserId, pt.calendar_work_day AS work_day, pt.amount AS paymentTransactionAmount,pt.created_at as paymentTransactionAmountCreated,pt.type_transaction as typeTransaction, concat(up.first_name,' ',up.last_name)
as transactionUserFullName,concat(pc.first_name,' ',pc.last_name) as transactionCustomerFullName,tp.name as tiersPayantName, pmp.libelle as transactionPaymentModeLibelle,pmp.code as transactionPaymentModeCode,
pt.id as transactionId FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id
JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code;
""";

  private static final String SQL_ONLY_SALES_COUNT =
      """
SELECT count(*) AS total
FROM warehouse_calendar wc  JOIN sales s ON wc.work_day = s.calendar_work_day
  join payment p ON s.id = p.sales_id JOIN
  payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id
  LEFT JOIN customer c ON s.customer_id = c.id
""";
  private static final String SQL_ONLY_TRANSACTION_COUNT =
      """
SELECT count(*) FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code
""";

  private static final String USER_ID = " AND mvt.userId  = %d ";
  private static final String DATE = " WHERE mvt.createdAt BETWEEN '%s' AND '%s' ";
  private static final String SEARCH = " AND LOWER(mvt.numberTransaction) LIKE '%s' ";
  private static final String TYPE_EVENT = " AND mvt.typeMvt ='%s' ";
  private static final String TYPE = " AND mvt.typeTransaction IN (%s) ";
  private static final String PAYMENT_MODE = " AND  mvt.paymentModeCode IN (%s) ";
  private static final String CATEGORIE_CHIFFRE_AFFAIRE = " AND mvt.ca IN (%s) ";
  private static final String SQL_FULL_BASE_QUERY_ORDER_BY = " ORDER BY mvt.createdAt %s";

  private static final String SQL_FULL_BASE_QUERY_TOTAUX =
      """
SELECT sum(mvt.amount) AS amount,mvt.typeMvt,mvt.typeTransaction,mvt.paymentModeCode,mvt.paymentModeLibelle FROM (SELECT 'vente' as typeMvt,
                       p.paid_amount as amount,p.payment_mode_code as paymentModeCode,
                      md.libelle as paymentModeLibelle,s.dtype AS typeTransaction,s.updated_at as createdAt
               FROM  sales s join payment p ON s.id = p.sales_id JOIN
                     payment_mode md ON p.payment_mode_code = md.code  JOIN user u ON s.user_id = u.id
                             LEFT JOIN customer c ON s.customer_id = c.id   union
               SELECT 'transaction' as typeMvt, pt.amount AS amount,pt.payment_mode_code as paymentModeCode,
                      pmp.libelle as paymentModeLibelle,pt.type_transaction as typeTransaction,pt.created_at as createdAt
               FROM  payment_transaction pt left join  customer pc on pt.organisme_id = pc.id LEFT JOIN tiers_payant tp
  ON  pt.organisme_id = tp.id JOIN user up ON pt.user_id = up.id
 JOIN payment_mode pmp ON pt.payment_mode_code = pmp.code) as mvt group by mvt.typeMvt,mvt.typeTransaction,mvt.paymentModeCode,mvt.paymentModeLibelle

""";

  private final PaymentTransactionRepository paymentTransactionRepository;
  private final UserService userService;
  private final CustomerRepository customerRepository;
  private final TiersPayantRepository tiersPayantRepository;
  private final FournisseurRepository fournisseurRepository;
  private final WarehouseCalendarService warehouseCalendarService;
  private final CashRegisterService cashRegisterService;
  private final EntityManager em;

  public FinancialTransactionServiceImpl(
      PaymentTransactionRepository paymentTransactionRepository,
      UserService userService,
      CustomerRepository customerRepository,
      TiersPayantRepository tiersPayantRepository,
      FournisseurRepository fournisseurRepository,
      WarehouseCalendarService warehouseCalendarService,
      CashRegisterService cashRegisterService,
      EntityManager em) {
    this.paymentTransactionRepository = paymentTransactionRepository;
    this.userService = userService;
    this.customerRepository = customerRepository;
    this.tiersPayantRepository = tiersPayantRepository;
    this.fournisseurRepository = fournisseurRepository;
    this.warehouseCalendarService = warehouseCalendarService;
    this.cashRegisterService = cashRegisterService;
    this.em = em;
  }

  @Override
  public void create(FinancialTransactionDTO financialTransactionDTO) {
    PaymentTransaction paymentTransaction = fromDTO(financialTransactionDTO);
    paymentTransactionRepository.save(paymentTransaction);
  }

  @Override
  public Page<FinancialTransactionDTO> findAll(
      FinancielTransactionFilterDTO financielTransactionFilter, Pageable pageable) {
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
    List<FinancialTransactionDTO> paymentTransactions =
        q.getResultList().stream().map(this::toDTO).toList();
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
  public List<MvtCaisseSum> getMvtCaisseSum(TransactionFilterDTO transactionFilter) {
    List<MvtCaisseSum> mvtCaisseSums = new ArrayList<>();
    List<MvtCaisseSum> mvtCaisseSumsFinal = new ArrayList<>();
    fetchSalesAndTransactionTotaux(transactionFilter)
        .forEach(tuple -> mvtCaisseSums.addAll(buildSumFromTuple(tuple)));
    mvtCaisseSums.stream()
        .collect(
            Collectors.groupingBy(
                MvtCaisseSum::getPaymentModeCode,
                Collectors.groupingBy(MvtCaisseSum::getTypeTransaction)))
        .forEach(
            (paymentModeCode, typeTransactionMap) -> {
              typeTransactionMap.forEach(
                  (typeTransaction, mvtCaisseSums1) -> {
                    BigDecimal amount =
                        mvtCaisseSums1.stream()
                            .map(MvtCaisseSum::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    mvtCaisseSumsFinal.add(
                        new MvtCaisseSum()
                            .setAmount(amount)
                            .setPaymentModeCode(paymentModeCode)
                            .setPaymentModeLibelle(mvtCaisseSums1.get(0).getPaymentModeLibelle())
                            .setTypeTransaction(typeTransaction));
                  });
            });
    return mvtCaisseSumsFinal;
  }

  @Override
  public Optional<FinancialTransactionDTO> findById(Long id) {
    return this.paymentTransactionRepository.findById(id).map(this::toDTO);
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
      case REGLEMENT_DIFFERE:
      case REGLEMENT_TIERS_PAYANT:
      case ENTREE_CAISSE:
        paymentTransaction.setCredit(true);
        break;
      case SORTIE_CAISSE:
      case REGLMENT_FOURNISSEUR:
        paymentTransaction.setCredit(false);
        break;
    }
    return paymentTransaction;
  }

  private FinancialTransactionDTO toDTO(PaymentTransaction paymentTransaction) {
    FinancialTransactionDTO financialTransactionDTO = new FinancialTransactionDTO();
    financialTransactionDTO.setAmount(paymentTransaction.getAmount());
    financialTransactionDTO.setPaymentMode(paymentTransaction.getPaymentMode());
    financialTransactionDTO.setTransactionDate(paymentTransaction.getTransactionDate());
    financialTransactionDTO.setTypeFinancialTransaction(
        paymentTransaction.getTypeFinancialTransaction());
    financialTransactionDTO.setOrganismeId(paymentTransaction.getOrganismeId());
    financialTransactionDTO.setCreatedAt(paymentTransaction.getCreatedAt());
    financialTransactionDTO.setCredit(paymentTransaction.isCredit());
    switch (paymentTransaction.getTypeFinancialTransaction()) {
      case REGLEMENT_DIFFERE:
        Customer customer =
            this.customerRepository.getReferenceById(paymentTransaction.getOrganismeId());
        financialTransactionDTO.setOrganismeName(
            customer.getFirstName().concat(" ").concat(customer.getLastName()));
        break;
      case REGLEMENT_TIERS_PAYANT:
        financialTransactionDTO.setOrganismeName(
            tiersPayantRepository.getReferenceById(paymentTransaction.getOrganismeId()).getName());
        break;
      case REGLMENT_FOURNISSEUR:
        financialTransactionDTO.setOrganismeName(
            fournisseurRepository
                .getReferenceById(paymentTransaction.getOrganismeId())
                .getLibelle());
        break;
    }
    var user = paymentTransaction.getUser();
    financialTransactionDTO.setUserFullName(
        user.getFirstName().concat(" ").concat(user.getLastName()));
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

  private List<Predicate> predicate(
      CriteriaBuilder cb,
      Root<PaymentTransaction> root,
      FinancielTransactionFilterDTO transactionFilter) {
    List<Predicate> predicates = new ArrayList<>();

    if (!ObjectUtils.isEmpty(transactionFilter.typeFinancialTransaction())) {
      predicates.add(
          cb.equal(
              root.get(PaymentTransaction_.typeFinancialTransaction),
              transactionFilter.typeFinancialTransaction()));
    }
    if (!ObjectUtils.isEmpty(transactionFilter.organismeId())) {
      predicates.add(
          cb.equal(root.get(PaymentTransaction_.organismeId), transactionFilter.organismeId()));
    }
    if (Objects.nonNull(transactionFilter.userId())) {
      predicates.add(
          cb.equal(root.get(PaymentTransaction_.user).get("id"), transactionFilter.userId()));
    }
    if (Objects.nonNull(transactionFilter.fromDate())
        && Objects.nonNull(transactionFilter.toDate())) {
      predicates.add(
          cb.between(
              root.get(PaymentTransaction_.createdAt),
              LocalDateTime.of(transactionFilter.fromDate(), LocalTime.MIN),
              LocalDateTime.of(transactionFilter.toDate(), LocalTime.MAX)));
    }
    if (StringUtils.hasText(transactionFilter.search())) {
      var search = "%" + transactionFilter.search().toLowerCase() + "%";
      predicates.add(
          cb.or(
              cb.like(cb.lower(root.get(PaymentTransaction_.ticketCode)), search),
              cb.like(cb.lower(root.get(PaymentTransaction_.ticketCode)), search)));
    }
    if (transactionFilter.categorieChiffreAffaire() != null) {
      predicates.add(
          cb.equal(
              root.get(PaymentTransaction_.categorieChiffreAffaire),
              transactionFilter.categorieChiffreAffaire()));
    }
    if (transactionFilter.paymentMode() != null) {
      predicates.add(
          cb.equal(
              root.get(PaymentTransaction_.paymentMode).get("code"),
              transactionFilter.paymentMode()));
    }

    return predicates;
  }

  private String getWhereClause(TransactionFilterDTO financielTransactionFilter) {
    StringBuilder where = new StringBuilder();

    LocalDateTime fromDate =
        Objects.nonNull(financielTransactionFilter.fromDate())
            ? LocalDateTime.of(financielTransactionFilter.fromDate(), LocalTime.MIN)
            : LocalDate.now().atStartOfDay();
    LocalDateTime toDate =
        Objects.nonNull(financielTransactionFilter.toDate())
            ? LocalDateTime.of(financielTransactionFilter.toDate(), LocalTime.MAX)
            : LocalDateTime.now();

    where.append(String.format(DATE, fromDate, toDate));
    if (financielTransactionFilter.userId() != null) {
      where.append(String.format(USER_ID, financielTransactionFilter.userId()));
    }
    if (StringUtils.hasText(financielTransactionFilter.search())) {
      String search = "%" + financielTransactionFilter.search().toLowerCase() + "%";
      where.append(String.format(SEARCH, search));
    }
    /* if (StringUtils.hasText(financielTransactionFilter.typeFinancialTransactions())) {
      where.append(String.format(TYPE, financielTransactionFilter.typeFinancialTransactions()));
    }*/
    if (financielTransactionFilter.typeFinancialTransactions() != null
        && !financielTransactionFilter.typeFinancialTransactions().isEmpty()) {
      // StringBuilder typeBuilder = new StringBuilder();
      var type =
          getTypeTransaction(financielTransactionFilter.typeFinancialTransactions()).stream()
              .map(String::valueOf)
              .collect(Collectors.joining(","));
      var typesVente =
          getTypeVente(financielTransactionFilter.typeFinancialTransactions()).stream()
              .collect(Collectors.joining(","));

      if (typesVente.length() > 0) {

        if (type.length() > 0) {
          where.append(String.format(TYPE, type.concat(",").concat(typesVente)));
        } else {
          where.append(String.format(TYPE, typesVente));
        }

      } else {
        if (type.length() > 0) {
          where.append(String.format(TYPE, type));
        }
      }
    }

    if (CollectionUtils.isNotEmpty(financielTransactionFilter.paymentModes())) {
      String paymentModes =
          financielTransactionFilter.paymentModes().stream()
              .map(e -> "'" + e + "'")
              .collect(Collectors.joining(","));
      where.append(String.format(PAYMENT_MODE, paymentModes));
    }
    if (CollectionUtils.isNotEmpty(financielTransactionFilter.categorieChiffreAffaires())) {

      var categorieChiffreAffairesOrdinal =
          financielTransactionFilter.categorieChiffreAffaires().stream()
              .map(e -> String.valueOf(e.ordinal()))
              .collect(Collectors.joining(","));
      var categorieChiffreAffaires =
          financielTransactionFilter.categorieChiffreAffaires().stream()
              .map(e -> "'" + e.name() + "'")
              .collect(Collectors.joining(","));
      var ca =
          String.format(
              CATEGORIE_CHIFFRE_AFFAIRE,
              categorieChiffreAffairesOrdinal.concat(",").concat(categorieChiffreAffaires));
      where.append(ca);
    }

    return where.toString();
  }

  private Set<Integer> getTypeTransaction(Set<TypeFinancialTransaction> typeFinancialTransactions) {
    return typeFinancialTransactions.stream()
        .filter(
            type ->
                type == TypeFinancialTransaction.REGLEMENT_DIFFERE
                    || type == TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT
                    || type == TypeFinancialTransaction.ENTREE_CAISSE
                    || type == TypeFinancialTransaction.SORTIE_CAISSE
                    || type == TypeFinancialTransaction.REGLMENT_FOURNISSEUR)
        .map(TypeFinancialTransaction::ordinal)
        .collect(Collectors.toSet());
  }

  private Set<String> getTypeVente(Set<TypeFinancialTransaction> typeFinancialTransactions) {
    boolean asSaleType =
        typeFinancialTransactions.contains(TypeFinancialTransaction.CASH_SALE)
            || typeFinancialTransactions.contains(TypeFinancialTransaction.CREDIT_SALE)
            || typeFinancialTransactions.contains(TypeFinancialTransaction.VENTES_DEPOTS);
    if (asSaleType) {
      Set<String> financialTransactions =
          typeFinancialTransactions.stream()
              .filter(
                  type ->
                      type == TypeFinancialTransaction.CASH_SALE
                          || type == TypeFinancialTransaction.CREDIT_SALE
                          || type == TypeFinancialTransaction.VENTES_DEPOTS)
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
    return SQL_FULL_BASE_QUERY
        + getWhereClause(financielTransactionFilter)
        + getOrderByClause(financielTransactionFilter);
  }

  private String getFullBaseTotauxQuery(TransactionFilterDTO financielTransactionFilter) {
    return SQL_FULL_BASE_QUERY_TOTAUX + getWhereClause(financielTransactionFilter);
  }

  private String getFullBaseCountQuery(TransactionFilterDTO financielTransactionFilter) {
    return FULL_BASE_COUNT_QUERY + getWhereClause(financielTransactionFilter);
  }

  private String getOnlyTransactionOrderByClause(TransactionFilterDTO financielTransactionFilter) {
    if (Objects.isNull(financielTransactionFilter.order())) {
      Order order = Order.DESC;
      return String.format(" ORDER BY pt.created_at %s", order.name());
    }
    return String.format(" ORDER BY pt.created_at %s", financielTransactionFilter.order().name());
  }

  private List<Tuple> fetchSalesAndTransaction(
      TransactionFilterDTO financielTransactionFilter, Pageable pageable) {
    if (pageable.isPaged()) {
      return em.createNativeQuery(getFullBaseQuery(financielTransactionFilter), Tuple.class)
          .setFirstResult((int) pageable.getOffset())
          .setMaxResults(pageable.getPageSize())
          .getResultList();
    }
    return em.createNativeQuery(getFullBaseQuery(financielTransactionFilter), Tuple.class)
        .getResultList();
  }

  private long fetchSalesAndTransactionCount(TransactionFilterDTO financielTransactionFilter) {
    return ((Number)
            em.createNativeQuery(getFullBaseCountQuery(financielTransactionFilter))
                .getSingleResult())
        .longValue();
  }

  private List<Tuple> fetchSalesAndTransactionTotaux(
      TransactionFilterDTO financielTransactionFilter) {
    return em.createNativeQuery(getFullBaseTotauxQuery(financielTransactionFilter), Tuple.class)
        .getResultList();
  }

  private List<MvtCaisseSum> buildSumFromTuple(Tuple tuple) {
    if (Objects.isNull(tuple.get("paidAmount", BigDecimal.class))
        && Objects.isNull(tuple.get("paymentTransactionAmount", BigDecimal.class))) {
      return List.of();
    }
    List<MvtCaisseSum> mvtCaisseSums = new ArrayList<>();
    if (Objects.nonNull(tuple.get("paidAmount", BigDecimal.class))) {
      TypeVente typeVente = TypeVente.fromValue(tuple.get("typeVente", String.class));
      MvtCaisseSum mvtCaisseSum = new MvtCaisseSum();
      mvtCaisseSum.setAmount(tuple.get("paidAmount", BigDecimal.class));
      mvtCaisseSum.setPaymentModeCode(tuple.get("salePaymentModeCode", String.class));
      mvtCaisseSum.setPaymentModeLibelle(tuple.get("salePaymentModeLibelle", String.class));
      mvtCaisseSum.setTypeTransaction(TypeFinancialTransaction.valueOf(typeVente.name()));
      mvtCaisseSums.add(mvtCaisseSum);
    }
    if (Objects.nonNull(tuple.get("paymentTransactionAmount", BigDecimal.class))) {
      MvtCaisseSum mvtCaisseSum = new MvtCaisseSum();
      mvtCaisseSum.setAmount(tuple.get("paymentTransactionAmount", BigDecimal.class));
      mvtCaisseSum.setPaymentModeCode(tuple.get("transactionPaymentModeCode", String.class));
      mvtCaisseSum.setPaymentModeLibelle(tuple.get("transactionPaymentModeLibelle", String.class));
      mvtCaisseSum.setTypeTransaction(
          TypeFinancialTransaction.values()[tuple.get("typeTransaction", Integer.class)]);
      mvtCaisseSums.add(mvtCaisseSum);
    }
    return mvtCaisseSums;
  }

  private MvtCaisseDTO buildFromTuple(Tuple tuple) {
    MvtCaisseDTO mvtCaisseDTO = new MvtCaisseDTO();
    mvtCaisseDTO.setId(tuple.get("id", Long.class));
    if (tuple.get("typeMvt", String.class).equals("vente")) {
      TypeVente typeVente = TypeVente.fromValue(tuple.get("typeTransaction", String.class));
      mvtCaisseDTO.setType(TypeFinancialTransaction.valueOf(typeVente.name()));
      mvtCaisseDTO.setCategorieChiffreAffaire(
          CategorieChiffreAffaire.valueOf(tuple.get("ca", String.class)));
      mvtCaisseDTO.setStatut(SalesStatut.valueOf(tuple.get("statut", String.class)));
      mvtCaisseDTO.setDiscount(tuple.get("discountAmount", Integer.class));
      mvtCaisseDTO.setHtAmount(tuple.get("htAmount", Integer.class));
      mvtCaisseDTO.setNetAmount(tuple.get("netAmount", Integer.class));
      mvtCaisseDTO.setPartAssure(tuple.get("partAssure", Integer.class));
      mvtCaisseDTO.setPartAssureur(tuple.get("part_tiers_payant", Integer.class));
    }
    mvtCaisseDTO.setMontant(tuple.get("amount", Integer.class));
    mvtCaisseDTO.setOrganisme(tuple.get("customerFullName", String.class));
    mvtCaisseDTO.setUserFullName(tuple.get("userFullName", String.class));
    mvtCaisseDTO.setReference(tuple.get("numberTransaction", String.class));
    mvtCaisseDTO.setPaymentMode(tuple.get("paymentModeCode", String.class));
    mvtCaisseDTO.setPaymentModeLibelle(tuple.get("paymentModeLibelle", String.class));
    mvtCaisseDTO.setDate(DateUtil.format(tuple.get("createdAt", Timestamp.class)));
    mvtCaisseDTO.setNumBon(tuple.get("infoAssureur", String.class));
    if (tuple.get("typeMvt", String.class).equals("transaction")) {
      mvtCaisseDTO.setType(
          TypeFinancialTransaction.values()[
              Integer.parseInt(tuple.get("typeTransaction", String.class))]);
      mvtCaisseDTO.setCategorieChiffreAffaire(
          CategorieChiffreAffaire.values()[Integer.parseInt(tuple.get("ca", String.class))]);
      mvtCaisseDTO.setTransactionDate(
          DateUtil.format(tuple.get("transactionDate", Timestamp.class)));
    }
    return mvtCaisseDTO;
  }
}
