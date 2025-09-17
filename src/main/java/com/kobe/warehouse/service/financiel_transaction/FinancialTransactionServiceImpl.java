package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.DefaultPayment;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.PaymentTransaction_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.repository.DefaultTransactionRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.MouvementCaisseGroupByMode;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseSumProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.SaleInfo;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.service.utils.ServiceUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
public class FinancialTransactionServiceImpl implements FinancialTransactionService {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserService userService;
    private final SalesRepository salesRepository;
    private final CashRegisterService cashRegisterService;
    private final EntityManager em;
    private final MvtCaisseReportReportService mvtCaisseReportService;
    private final DefaultTransactionRepository defaultTransactionRepository;
    private final TransactionIdGeneratorService transactionIdGeneratorService;

    public FinancialTransactionServiceImpl(
        PaymentTransactionRepository paymentTransactionRepository,
        UserService userService,
        SalesRepository salesRepository,
        CashRegisterService cashRegisterService,
        EntityManager em,
        MvtCaisseReportReportService mvtCaisseReportService,
        DefaultTransactionRepository defaultTransactionRepository, TransactionIdGeneratorService transactionIdGeneratorService
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userService = userService;
        this.salesRepository = salesRepository;
        this.cashRegisterService = cashRegisterService;
        this.em = em;
        this.mvtCaisseReportService = mvtCaisseReportService;
        this.defaultTransactionRepository = defaultTransactionRepository;
        this.transactionIdGeneratorService = transactionIdGeneratorService;
    }

    @Override
    public void create(FinancialTransactionDTO financialTransactionDTO) {
        defaultTransactionRepository.save(fromDTO(financialTransactionDTO));
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
        return fetchAll(transactionFilter, pageable);
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
        List<MvtCaisseDTO> mvtCaisses = fetchAll(transactionFilter, Pageable.unpaged()).getContent();
        MvtCaisseWrapper mvtCaisseWrapper = buildMvtCaisseWrapper(transactionFilter);
        Pair pair = buildPeriode(transactionFilter);
        return this.mvtCaisseReportService.exportToPdf(
            new ArrayList<>(mvtCaisses),
            mvtCaisseWrapper,
            new ReportPeriode(((LocalDateTime) pair.key()).toLocalDate(), ((LocalDateTime) pair.value()).toLocalDate())
        );
    }

    @Override
    public List<MouvementCaisse> findMouvementsCaisse(LocalDate fromDate, LocalDate toDate) {
        return this.paymentTransactionRepository.findMouvementsCaisse(fromDate, toDate);
    }

    @Override
    public List<MouvementCaisseGroupByMode> findMouvementsCaisseGroupBYModeReglement(LocalDate fromDate, LocalDate toDate) {
        return this.paymentTransactionRepository.findMouvementsCaisseGroupBYModeReglement(fromDate, toDate);
    }

    private DefaultPayment fromDTO(FinancialTransactionDTO financialTransaction) {
        var user = userService.getUser();
        DefaultPayment paymentTransaction = new DefaultPayment();
        paymentTransaction.setId(transactionIdGeneratorService.nextId());

        paymentTransaction.setCreatedAt(LocalDateTime.now());
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        paymentTransaction.setCashRegister(cashRegister);
        paymentTransaction.setExpectedAmount(financialTransaction.getAmount());
        paymentTransaction.setPaidAmount(financialTransaction.getAmount());
        paymentTransaction.setReelAmount(financialTransaction.getAmount());
        paymentTransaction.setPaymentMode(financialTransaction.getPaymentMode());
        if (Objects.nonNull(financialTransaction.getTransactionDate())) {
            paymentTransaction.setTransactionDate(financialTransaction.getTransactionDate());
        }
        paymentTransaction.setTypeFinancialTransaction(financialTransaction.getTypeTransaction());
        switch (financialTransaction.getTypeTransaction()) {
            case SORTIE_CAISSE, REGLMENT_FOURNISSEUR:
                paymentTransaction.setCredit(true);
                break;
            default:
                break;
        }
        return paymentTransaction;
    }

    private FinancialTransactionDTO toDTO(PaymentTransaction paymentTransaction) {
        FinancialTransactionDTO financialTransactionDTO = new FinancialTransactionDTO();
        //   financialTransactionDTO.setAmount(paymentTransaction.getAmount());
        financialTransactionDTO.setPaymentMode(paymentTransaction.getPaymentMode());
        financialTransactionDTO.setTransactionDate(paymentTransaction.getTransactionDate());
        financialTransactionDTO.setTypeFinancialTransaction(paymentTransaction.getTypeFinancialTransaction());
        // financialTransactionDTO.setOrganismeId(paymentTransaction.getObjectId());
        financialTransactionDTO.setCreatedAt(paymentTransaction.getCreatedAt());
        financialTransactionDTO.setCredit(paymentTransaction.isCredit());
        /* switch (paymentTransaction.getTypeFinancialTransaction()) {
            case REGLEMENT_DIFFERE:
                Customer customer = this.customerRepository.getReferenceById(paymentTransaction.getObjectId());
                financialTransactionDTO.setOrganismeName(customer.getFirstName().concat(" ").concat(customer.getLastName()));
                break;
            case REGLEMENT_TIERS_PAYANT:
                financialTransactionDTO.setOrganismeName(
                    tiersPayantRepository.getReferenceById(paymentTransaction.getObjectId()).getName()
                );
                break;
            case REGLMENT_FOURNISSEUR:
                financialTransactionDTO.setOrganismeName(
                    fournisseurRepository.getReferenceById(paymentTransaction.getObjectId()).getLibelle()
                );
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + paymentTransaction.getTypeFinancialTransaction());
        }*/
        var user = paymentTransaction.getCashRegister().getUser();
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

        if (Objects.nonNull(transactionFilter.userId())) {
            predicates.add(
                cb.equal(root.get(PaymentTransaction_.cashRegister).get(CashRegister_.user).get(AppUser_.id), transactionFilter.userId())
            );
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

    private Page<MvtCaisseDTO> fetchAll(TransactionFilterDTO transactionFilter, Pageable pageable) {
        return paymentTransactionRepository.fetchAll(buildSpecification(transactionFilter), pageable).map(this::buildFrom);
    }

    private Specification<PaymentTransaction> buildSpecification(TransactionFilterDTO transactionFilter) {
        var from = Objects.requireNonNullElse(transactionFilter.fromDate(), LocalDate.now());
        var to = Objects.requireNonNullElse(transactionFilter.toDate(), LocalDate.now());
        Specification<PaymentTransaction> specification =
            paymentTransactionRepository.filterByPeriode(from, to);
        if (transactionFilter.userId() != null) {
            specification = specification.and(paymentTransactionRepository.filterByUserId(transactionFilter.userId()));
        }

        if (!org.springframework.util.CollectionUtils.isEmpty(transactionFilter.paymentModes())) {
            specification = specification.and(paymentTransactionRepository.filterByPaymentMode(transactionFilter.paymentModes()));
        }
        if (!org.springframework.util.CollectionUtils.isEmpty(transactionFilter.typeFinancialTransactions())) {
            specification = specification.and(
                paymentTransactionRepository.filterByTypeFinancialTransaction(EnumSet.copyOf(transactionFilter.typeFinancialTransactions()))
            );
        }

        if (org.springframework.util.CollectionUtils.isEmpty(transactionFilter.categorieChiffreAffaires())) {
            specification = specification.and(
                paymentTransactionRepository.filterByCategorieChiffreAffaire(EnumSet.of(CategorieChiffreAffaire.CA))
            );
        } else {
            specification = specification.and(
                paymentTransactionRepository.filterByCategorieChiffreAffaire(EnumSet.copyOf(transactionFilter.categorieChiffreAffaires()))
            );
        }
        return specification;
    }

    //TODO voir comment implementer avec une fonction postgre jsonb
    private MvtCaisseDTO buildFrom(MvtCaisseProjection mvtCaisseProjection) {
        MvtCaisseDTO mvtCaisseDTO = new MvtCaisseDTO();
        mvtCaisseDTO.setId(mvtCaisseProjection.id());
        mvtCaisseDTO.setMontant(mvtCaisseProjection.montant());
        mvtCaisseDTO.setPaymentMode(mvtCaisseProjection.paymentMode());
        mvtCaisseDTO.setPaymentModeLibelle(mvtCaisseProjection.paymentModeLibelle());
        mvtCaisseDTO.setType(mvtCaisseProjection.typeFinancialTransaction());
        mvtCaisseDTO.setDate(DateUtil.format(mvtCaisseProjection.createdAt()));
        mvtCaisseDTO.setUserFullName(mvtCaisseProjection.firstName().charAt(0) + "".toUpperCase() + ". " + mvtCaisseProjection.lastName());
        switch (mvtCaisseProjection.paymentType()) {
            case SalePayment -> {
                SaleInfo saleInfo = salesRepository.findSaleInfoById(mvtCaisseProjection.saleId(), mvtCaisseProjection.saleDate());
                mvtCaisseDTO.setReference(saleInfo.getReference());
                if (saleInfo.getCustomerFirstName() != null) {
                    mvtCaisseDTO.setOrganisme(saleInfo.getCustomerLastName() + " " + saleInfo.getCustomerFirstName());
                }
            }
            case InvoicePayment -> {
            } //TODO dans invoice
            case DifferePayment -> {
            } //TODO dans differe
            case AccountTransaction -> {
            } //TODO gestion des cautions
        }
        return mvtCaisseDTO;
    }

    private MvtCaisseWrapper buildMvtCaisseWrapper(TransactionFilterDTO transactionFilter) {
        List<MvtCaisseSumProjection> mvtCaisseSumProjections = paymentTransactionRepository.fetchAllSum(
            buildSpecification(transactionFilter)
        );
        MvtCaisseWrapper mvtCaisseWrapper = new MvtCaisseWrapper();

        BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        BigDecimal totalMobileAmount = BigDecimal.ZERO;
        BigDecimal creditedAmount = BigDecimal.ZERO;
        BigDecimal debitedAmount = BigDecimal.ZERO;
        List<com.kobe.warehouse.service.dto.records.Tuple> typeTransactionAmounts = new ArrayList<>();
        List<com.kobe.warehouse.service.dto.records.Tuple> modesPaiementAmounts = new ArrayList<>();

        for (Entry<TransactionTypeAffichage, List<MvtCaisseSumProjection>> transactionTypeAffichageListEntry : mvtCaisseSumProjections
            .stream()
            .collect(Collectors.groupingBy(mvt -> mvt.typeTransaction().getTransactionTypeAffichage()))
            .entrySet()) {
            TransactionTypeAffichage key = transactionTypeAffichageListEntry.getKey();
            List<MvtCaisseSumProjection> value = transactionTypeAffichageListEntry.getValue();
            BigDecimal typeAmount = new BigDecimal(0);
            for (MvtCaisseSumProjection mvtCaisseSum : value) {
                typeAmount = typeAmount.add(BigDecimal.valueOf(mvtCaisseSum.amount()));

                if (ServiceUtil.isPaymentMode(mvtCaisseSum.codeModeReglement())) {
                    totalMobileAmount = totalMobileAmount.add(BigDecimal.valueOf(mvtCaisseSum.amount()));
                }
            }
            switch (key) {
                case VNO, VO:
                    totalPaymentAmount = totalPaymentAmount.add(typeAmount);
                    break;
                case ENTREE_CAISSE, REGLEMENT_DIFFERE, REGLEMENT_TIERS_PAYANT, DEPOT_CAUTION:
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
        mvtCaisseSumProjections
            .stream()
            .collect(Collectors.groupingBy(MvtCaisseSumProjection::codeModeReglement))
            .forEach((k, v) -> {
                modesPaiementAmounts.add(
                    new com.kobe.warehouse.service.dto.records.Tuple(
                        k,
                        v.getFirst().libelleModeReglement(),
                        v.stream().mapToLong(MvtCaisseSumProjection::amount).sum()
                    )
                );
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
