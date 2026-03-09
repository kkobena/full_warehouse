package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.utils.ServiceUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final SalePaymentRepository paymentRepository;
    private final PaymentModeRepository paymentModeRepository;
    private final TransactionIdGeneratorService transactionIdGeneratorService;

    public PaymentServiceImpl(
        SalePaymentRepository paymentRepository,
        PaymentModeRepository paymentModeRepository,
        TransactionIdGeneratorService transactionIdGeneratorService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentModeRepository = paymentModeRepository;
        this.transactionIdGeneratorService = transactionIdGeneratorService;
    }

    @Override
    public void clonePayment(SalePayment payment, Sales copy) {
        SalePayment paymentCopy = (SalePayment) payment.clone();
        paymentCopy.setId(this.transactionIdGeneratorService.nextId());
        paymentCopy.setCreatedAt(copy.getCreatedAt());
        paymentCopy.setTransactionDate(LocalDate.now());
        paymentCopy.setCashRegister(copy.getCashRegister());
        paymentCopy.setSale(copy);
        paymentCopy.setMontantVerse(paymentCopy.getMontantVerse() * (-1));
        paymentCopy.setReelAmount(paymentCopy.getReelAmount() * (-1));
        paymentCopy.setPaidAmount(paymentCopy.getPaidAmount() * (-1));
        paymentCopy.setExpectedAmount(paymentCopy.getExpectedAmount() * (-1));
        paymentRepository.save(paymentCopy);
        paymentRepository.save(payment);
    }

    @Override
    public Set<SalePayment> clonePayments(Set<SalePayment> salePayments, Sales copy) {

        Set<SalePayment> copyPayments = new HashSet<>();
        for (SalePayment salePayment : salePayments) {
            SalePayment paymentCopy = (SalePayment) salePayment.clone();
            paymentCopy.setId(this.transactionIdGeneratorService.nextId());
            paymentCopy.setTransactionDate(LocalDate.now());
            paymentCopy.setCreatedAt(copy.getCreatedAt());
            paymentCopy.setCashRegister(copy.getCashRegister());
            paymentCopy.setSale(copy);
            copyPayments.add(paymentCopy);

        }

        return copyPayments;

    }

    @Override
    public List<SalePayment> findAllBySales(SaleId id) {
        return this.paymentRepository.findAllBySaleIdAndSaleSaleDate(id.getId(), id.getSaleDate());
    }

    @Override
    public List<SalePayment> findAllBySale(Sales sales) {
        return paymentRepository.findAllBySale(sales);
    }

    @Override
    public void saveAll(Set<SalePayment> payments) {
        if (!CollectionUtils.isEmpty(payments)) {
            paymentRepository.saveAll(payments);
        }
    }


    @Override
    public void buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO) {
        removeOldPayment(sales);
        if (CollectionUtils.isEmpty(saleDTO.getPayments()) || saleDTO.getPayrollAmount() == null || saleDTO.getPayrollAmount() <= 0) {
            sales.setPayments(new HashSet<>());
            return;
        }
        saleDTO.getPayments().stream().filter(paymentDTO -> Objects.nonNull(paymentDTO.getPaidAmount()) && paymentDTO.getPaidAmount() > 0).forEach(paymentDTO -> paymentRepository.save(buildPaymentFromFromPaymentDTO(sales, paymentDTO)));
    }

    private void removeOldPayment(Sales sales) {
        Set<SalePayment> payments = sales.getPayments();
        if (!CollectionUtils.isEmpty(payments)) {
            payments.forEach(payment -> {
                payment.setSale(null);
                this.paymentRepository.delete(payment);
            });
            sales.setPayments(null);
        }
    }

    @Override
    public void delete(SalePayment payment) {
        paymentRepository.delete(payment);
    }

    private SalePayment buildPaymentFromFromPaymentDTO(Sales sales, PaymentDTO paymentDTO) {
        SalePayment payment = new SalePayment();
        payment.setId(this.transactionIdGeneratorService.nextId());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setSale(sales);
        payment.setCashRegister(sales.getCashRegister());

        // Normalisation des montants pour éviter les NPE
        int netAmount = Objects.requireNonNullElse(paymentDTO.getNetAmount(), 0);
        int paidAmount = Objects.requireNonNullElse(paymentDTO.getPaidAmount(), 0);

        if (paymentDTO.getPaymentMode() != null) {
            PaymentMode paymentMode = paymentModeRepository.getReferenceById(paymentDTO.getPaymentMode().getCode());
            ModePaimentCode modePaimentCode = ModePaimentCode.valueOf(paymentMode.getCode());

            if (modePaimentCode == ModePaimentCode.CASH) {
                applyCashPaymentAmounts(payment, sales, paymentDTO, netAmount);
            } else {
                payment.setReelAmount(netAmount);
                payment.setPaidAmount(paidAmount);
            }

            payment.setPaymentMode(paymentMode);
        } else {
            payment.setReelAmount(netAmount);
            payment.setPaidAmount(paidAmount);
        }

        payment.setExpectedAmount(netAmount);

        if (sales instanceof ThirdPartySales thirdPartySales) {
            payment.setPartTiersPayant(thirdPartySales.getPartTiersPayant());
            payment.setPartAssure(thirdPartySales.getPartAssure());
            payment.setTypeFinancialTransaction(TypeFinancialTransaction.CREDIT_SALE);
        }
        if (sales instanceof CashSale) {
            payment.setTypeFinancialTransaction(TypeFinancialTransaction.CASH_SALE);
            payment.setPartTiersPayant(0);
            payment.setPartAssure(0);
        }
        return payment;
    }

    private void applyCashPaymentAmounts(SalePayment payment, Sales sales, PaymentDTO paymentDTO, int netAmount) {
        int montantVerse = Objects.requireNonNullElse(paymentDTO.getMontantVerse(), 0);
        payment.setMontantVerse(montantVerse);

        int montantAttendu = Objects.requireNonNullElse(sales.getAmountToBePaid(), 0);

        if (montantVerse <= montantAttendu) {
            payment.setReelAmount(montantVerse);
            payment.setPaidAmount(ServiceUtil.arrondirAuMultipleDe5(montantVerse));
        } else {
            // Le client couvre la totalité : reelAmount = montant net attendu, paidAmount = montant arrondi au multiple de 5
            payment.setReelAmount(netAmount);
            payment.setPaidAmount(ServiceUtil.resoudreMontantPaye(montantVerse, montantAttendu));
        }
    }
}
