package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.DifferePayment;
import com.kobe.warehouse.domain.DifferePaymentItem;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.DifferePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.receipt.service.DiffereReceiptService;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummary;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.NewDifferePaymentDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereResponse;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;
import com.kobe.warehouse.service.reglement.dto.BanqueInfoDTO;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class ReglementDiffereServiceImpl implements ReglementDiffereService {

    private final DifferePaymentRepository differePaymentRepository;
    private final SalesRepository salesRepository;
    private final CustomerRepository customerRepository;
    private final CashRegisterService cashRegisterService;
    private final ReglementDiffereReportService reglementDiffereReportService;
    private final BanqueRepository banqueRepository;
    private final DiffereReceiptService differeReceiptService;

    public ReglementDiffereServiceImpl(
        DifferePaymentRepository differePaymentRepository,
        SalesRepository salesRepository,
        CustomerRepository customerRepository,
        CashRegisterService cashRegisterService,
        ReglementDiffereReportService reglementDiffereReportService,
        BanqueRepository banqueRepository,
        DiffereReceiptService differeReceiptService
    ) {
        this.differePaymentRepository = differePaymentRepository;
        this.salesRepository = salesRepository;
        this.customerRepository = customerRepository;
        this.cashRegisterService = cashRegisterService;
        this.reglementDiffereReportService = reglementDiffereReportService;
        this.banqueRepository = banqueRepository;
        this.differeReceiptService = differeReceiptService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDiffere> getClientDiffere() {
        // add new thread
        return this.salesRepository.getClientDiffere(Pageable.unpaged());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiffereItem> getDiffereItems(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    ) {
        return this.salesRepository.getDiffereItems(buildSpecification(customerId, paymentStatuses), pageable);
    }

    private Specification<Sales> buildSpecification(Long customerId, Set<PaymentStatus> paymentStatuses) {
        Specification<Sales> specification = this.salesRepository.filterByCustomerId(customerId);
        specification = Objects.isNull(specification)
            ? this.salesRepository.filterByPaymentStatus(paymentStatuses)
            : specification.and(this.salesRepository.filterByPaymentStatus(paymentStatuses));
        return specification;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiffereDTO> getDiffere(Long customerId, Set<PaymentStatus> paymentStatuses, Pageable pageable) {
        return getAllDiffere(buildSpecification(customerId, paymentStatuses), pageable).map(differe -> {
            List<DiffereItem> differeItems =
                this.salesRepository.getDiffereItems(
                    this.salesRepository.filterByCustomerId(differe.customerId()),
                    Pageable.unpaged()
                ).getContent();
            return new DiffereDTO(
                differe.customerId(),
                differe.firstName(),
                differe.lastName(),
                differe.saleAmount(),
                differe.paidAmount(),
                differe.rest(),
                differeItems
            );
        });
    }

    private Page<DiffereDTO> getAllDiffere(Specification<Sales> specification, Pageable pageable) {
        return this.salesRepository.getDiffere(specification, pageable).map(differe -> {
            List<DiffereItem> differeItems =
                this.salesRepository.getDiffereItems(
                    this.salesRepository.filterByCustomerId(differe.customerId()),
                    Pageable.unpaged()
                ).getContent();
            return new DiffereDTO(
                differe.customerId(),
                differe.firstName(),
                differe.lastName(),
                differe.saleAmount(),
                differe.paidAmount(),
                differe.rest(),
                differeItems
            );
        });
    }

    @Override
    public Optional<DiffereDTO> getOne(Long id) {
        Specification<Sales> specification = this.salesRepository.filterByCustomerId(id);
        specification = specification.and(this.salesRepository.filterByPaymentStatus(Set.of(PaymentStatus.IMPAYE)));
        return Optional.ofNullable(getAllDiffere(specification, Pageable.unpaged()).getContent().getFirst());
    }

    @Override
    public ReglementDiffereResponse doReglement(NewDifferePaymentDTO differePayment) {
        Customer customer = this.customerRepository.getReferenceById(differePayment.customerId());
        List<Sales> sales = this.salesRepository.findAllById(differePayment.saleIds());
        DifferePayment differePaymentEntity = new DifferePayment();
        differePaymentEntity.setDiffereCustomer(customer);
        differePaymentEntity.setMontantVerse(differePayment.amount());
        differePaymentEntity.setExpectedAmount(differePayment.expectedAmount());
        differePaymentEntity.setCashRegister(cashRegisterService.getCashRegister());
        differePaymentEntity.setPaymentMode(new PaymentMode().code(differePayment.paimentMode().name()));
        differePaymentEntity.setTransactionDate(Objects.requireNonNullElse(differePayment.paymentDate(), LocalDate.now()));
        differePaymentEntity.setBanque(buildBanque(differePayment.banqueInfo()));
        AtomicInteger paidAmount = new AtomicInteger(differePayment.amount());
        sales.forEach(sale -> {
            if (paidAmount.get() <= 0) {
                return;
            }
            DifferePaymentItem differePaymentItem = new DifferePaymentItem();
            int amountToPay = sale.getRestToPay();
            if (paidAmount.get() >= amountToPay) {
                paidAmount.addAndGet(-amountToPay);
                sale.setPaymentStatus(PaymentStatus.PAYE);
                sale.setRestToPay(0);
                differePaymentItem.setPaidAmount(amountToPay);
            } else {
                int remainingAmount = paidAmount.get();
                sale.setPaymentStatus(PaymentStatus.IMPAYE);
                sale.setRestToPay(amountToPay - remainingAmount);
                differePaymentItem.setPaidAmount(remainingAmount);
                paidAmount.set(0);
            }
            sale.setPayrollAmount(Objects.requireNonNullElse(sale.getPayrollAmount(), 0) + amountToPay);
            differePaymentItem.setExpectedAmount(sale.getRestToPay());
            differePaymentItem.setSale(sale);
            differePaymentItem.setDifferePayment(differePaymentEntity);
            differePaymentEntity.setPaidAmount(
                Objects.requireNonNullElse(differePaymentEntity.getPaidAmount(), 0) + differePaymentItem.getPaidAmount()
            );
            differePaymentEntity.setReelAmount(differePaymentEntity.getPaidAmount());
            differePaymentEntity.getDifferePaymentItems().add(differePaymentItem);
        });
        return new ReglementDiffereResponse(this.differePaymentRepository.save(differePaymentEntity).getId());
    }

    @Override
    public ReglementDiffereReceiptDTO getReglementDiffereReceipt(Long id) {
        DifferePayment differePayment = this.differePaymentRepository.findById(id).orElseThrow();
        User user = differePayment.getCashRegister().getUser();
        Customer customer = differePayment.getDiffereCustomer();
        PaymentMode paymentMode = differePayment.getPaymentMode();
        BigDecimal solde = this.salesRepository.getDiffereSoldeByCustomerId(customer.getId());

        return new ReglementDiffereReceiptDTO(
            user.getFirstName(),
            user.getLastName(),
            customer.getFirstName(),
            customer.getLastName(),
            differePayment.getExpectedAmount(),
            differePayment.getMontantVerse(),
            differePayment.getPaidAmount(),
            paymentMode.getCode(),
            paymentMode.getLibelle(),
            Objects.nonNull(solde) ? solde.intValue() : 0
        );
    }

    @Override
    public void printReceipt(long idReglement) {
        this.differeReceiptService.printReceipt(null, getReglementDiffereReceipt(idReglement));
    }

    @Override
    public DifferePaymentSummaryDTO getDifferePaymentSummary(Long customerId, LocalDate startDate, LocalDate endDate) {
        DifferePaymentSummary differePaymentSummary =
            this.differePaymentRepository.getDiffereSummary(buildSpecification(customerId, startDate, endDate));
        return new DifferePaymentSummaryDTO(
            differePaymentSummary.paidAmount(),
            this.salesRepository.getSolde(salesRepository.filterByCustomerId(customerId)).solde()
        );
    }

    @Override
    public DiffereSummary getDiffereSummary(Long customerId, Set<PaymentStatus> paymentStatuses) {
        return this.salesRepository.getDiffereSummary(buildSpecification(customerId, paymentStatuses));
    }

    @Override
    public Resource printListToPdf(Long customerId, Set<PaymentStatus> paymentStatuses) {
        DiffereSummary differeSummary = this.getDiffereSummary(customerId, paymentStatuses);
        List<DiffereDTO> differe = this.getDiffere(customerId, paymentStatuses, Pageable.unpaged()).getContent();
        return this.reglementDiffereReportService.printListToPdf(differe, differeSummary);
    }

    @Override
    public Resource printReglementToPdf(Long customerId, LocalDate startDate, LocalDate endDate) {
        DifferePaymentSummaryDTO differePaymentSummary = this.getDifferePaymentSummary(customerId, startDate, endDate);
        List<ReglementDiffereWrapperDTO> list = getReglementsDifferes(customerId, startDate, endDate, Pageable.unpaged()).getContent();
        return this.reglementDiffereReportService.printReglementToPdf(list, differePaymentSummary, new ReportPeriode(startDate, endDate));
    }

    @Override
    public Page<ReglementDiffereWrapperDTO> getReglementsDifferes(
        Long customerId,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    ) {
        return this.differePaymentRepository.getDifferePayments(buildSpecification(customerId, startDate, endDate), pageable).map(
            differePayment -> {
                List<ReglementDiffereDTO> differePaymentItems =
                    this.differePaymentRepository.getDifferePaymentsByCustomerId(
                        this.differePaymentRepository.filterByCustomerId(differePayment.id())
                    );
                return new ReglementDiffereWrapperDTO(
                    differePayment.id(),
                    differePayment.firstName(),
                    differePayment.lastName(),
                    differePayment.paidAmount(),
                    this.salesRepository.getSolde(salesRepository.filterByCustomerId(differePayment.id())).solde(),
                    differePaymentItems
                );
            }
        );
    }

    private Banque buildBanque(BanqueInfoDTO banqueInfo) {
        if (Objects.isNull(banqueInfo)) {
            return null;
        }
        return banqueRepository.save(
            new Banque().setCode(banqueInfo.getCode()).setNom(banqueInfo.getNom()).setBeneficiaire(banqueInfo.getBeneficiaire())
        );
    }

    private Specification<DifferePayment> buildSpecification(Long customerId, LocalDate startDate, LocalDate endDate) {
        startDate = Objects.requireNonNullElse(startDate, LocalDate.now());
        endDate = Objects.requireNonNullElse(endDate, LocalDate.now());
        Specification<DifferePayment> specification = this.differePaymentRepository.filterByPeriode(startDate, endDate);
        if (Objects.nonNull(customerId)) {
            specification = specification.and(this.differePaymentRepository.filterByCustomerId(customerId));
        }

        return specification;
    }
}
