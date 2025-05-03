package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.DifferePayment;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.DifferePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.reglement.differe.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementDiffereServiceImpl implements ReglementDiffereService {
    private final DifferePaymentRepository differePaymentRepository;
    private final SalesRepository salesRepository;
    private final CustomerRepository customerRepository;
    private final CashRegisterService cashRegisterService;
    private final ReglementDiffereReceiptService reglementDiffereReceiptService;
    private final ReglementDiffereReportService reglementDiffereReportService;

    public ReglementDiffereServiceImpl(DifferePaymentRepository differePaymentRepository, SalesRepository salesRepository, CustomerRepository customerRepository, CashRegisterService cashRegisterService, ReglementDiffereReceiptService reglementDiffereReceiptService, ReglementDiffereReportService reglementDiffereReportService) {
        this.differePaymentRepository = differePaymentRepository;
        this.salesRepository = salesRepository;
        this.customerRepository = customerRepository;
        this.cashRegisterService = cashRegisterService;
        this.reglementDiffereReceiptService = reglementDiffereReceiptService;
        this.reglementDiffereReportService = reglementDiffereReportService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDiffere> getClientDiffere() {
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
        return this.salesRepository.getDiffereItems(buildSpecification(customerId, search, startDate, endDate, paymentStatuses), pageable);
    }

    private Specification<Sales> buildSpecification(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses
    ) {
        startDate = Objects.requireNonNullElse(startDate, LocalDate.now());
        endDate = Objects.requireNonNullElse(endDate, LocalDate.now());
        Specification<Sales> specification = Specification.where(this.salesRepository.filterByPeriode(startDate, endDate));
        specification = specification.and(this.salesRepository.filterByCustomerId(customerId));
        specification = specification.and(this.salesRepository.filterNumberTransaction(search));
        specification = specification.and(this.salesRepository.filterByPaymentStatus(paymentStatuses));
        return specification;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiffereDTO> getDiffere(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    ) {
        return getAllDiffere(buildSpecification(customerId, search, startDate, endDate, paymentStatuses), pageable).map(differe -> {
            List<DiffereItem> differeItems =
                this.salesRepository.getDiffereItems(
                    Specification.where(this.salesRepository.filterByCustomerId(differe.customerId())),
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
                    Specification.where(this.salesRepository.filterByCustomerId(differe.customerId())),
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
        Specification<Sales> specification = Specification.where(this.salesRepository.filterByCustomerId(id));
        specification = specification.and(this.salesRepository.filterByPaymentStatus(Set.of(PaymentStatus.IMPAYE)));
        return Optional.ofNullable(getAllDiffere(specification, Pageable.unpaged()).getContent().getFirst());
    }

    @Override
    public Long doReglement(NewDifferePaymentDTO differePayment) {
        Customer customer = this.customerRepository.getReferenceById(differePayment.customerId());
        List<Sales> sales = this.salesRepository.findAllById(differePayment.saleIds());
        DifferePayment differePaymentEntity = new DifferePayment();
        differePaymentEntity.setDiffereCustomer(customer);
        differePaymentEntity.setMontantVerse(differePayment.amount());
        differePaymentEntity.setExpectedAmount(differePayment.expectedAmount());
        differePaymentEntity.setCashRegister(cashRegisterService.getCashRegister());
        differePaymentEntity.setPaymentMode(new PaymentMode().code(differePayment.paimentMode().name()));
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
                sale.setRestToPay(amountToPay);
                differePaymentItem.setPaidAmount(amountToPay);
            } else {
                int remainingAmount = paidAmount.get();
                sale.setPaymentStatus(PaymentStatus.IMPAYE);
                sale.setRestToPay(amountToPay - remainingAmount);
                differePaymentItem.setPaidAmount(remainingAmount);
                paidAmount.set(0);
            }
            differePaymentItem.setExpectedAmount(sale.getRestToPay());
            differePaymentItem.setSale(sale);
            differePaymentItem.setDifferePayment(differePaymentEntity);
            differePaymentEntity.setPaidAmount(differePaymentEntity.getPaidAmount() + differePaymentItem.getPaidAmount());
            differePaymentEntity.setReelAmount(differePaymentEntity.getPaidAmount());
            differePaymentEntity.getDifferePaymentItems().add(differePaymentItem);

        });
        return this.differePaymentRepository.save(differePaymentEntity).getId();
    }

    @Override
    public void printReceipt(long idReglement) {
        DifferePayment differePayment = this.differePaymentRepository.findById(idReglement).orElseThrow();
        User user = differePayment.getCashRegister().getUser();
        Customer customer = differePayment.getDiffereCustomer();
        PaymentMode paymentMode = differePayment.getPaymentMode();
        BigDecimal solde = this.salesRepository.getDiffereSoldeByCustomerId(customer.getId());

        this.reglementDiffereReceiptService.printRecipt(new ReglementDiffereReceiptDTO(
            user.getFirstName(), user.getLastName(), customer.getFirstName(), customer.getLastName(),
            differePayment.getExpectedAmount(), differePayment.getMontantVerse(), differePayment.getPaidAmount(), paymentMode.getCode(),
            paymentMode.getLibelle(), Objects.nonNull(solde) ? solde.intValue() : 0
        ));

    }

    @Override
    public DifferePaymentSummary getDifferePaymentSummary(Long customerId, LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Override
    public DiffereSummary getDiffereSummary(Long customerId, String search, LocalDate startDate, LocalDate endDate, Set<PaymentStatus> paymentStatuses) {
        return this.salesRepository.getDiffereSummary(buildSpecification(customerId, search, startDate, endDate, paymentStatuses));
    }

    @Override
    public Resource printListToPdf(Long customerId,
                                   String search,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   Set<PaymentStatus> paymentStatuses) {
        DiffereSummary differeSummary = this.getDiffereSummary(customerId, search, startDate, endDate, paymentStatuses);
        List<DiffereDTO> differe = this.getDiffere(customerId, search, startDate, endDate, paymentStatuses, Pageable.unpaged()).getContent();
        return this.reglementDiffereReportService.printListToPdf(differe, differeSummary, new ReportPeriode(startDate, endDate));
    }

    @Override
    public Resource printReglementToPdf(Long customerId,
                                        LocalDate startDate,
                                        LocalDate endDate) {
        return null;
    }

    @Override
    public Page<ReglementDiffereWrapperDTO> getReglementsDifferes(Long customerId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return null;
    }
}
