package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.PaymentRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentModeRepository paymentModeRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentModeRepository paymentModeRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentModeRepository = paymentModeRepository;
    }

    @Override
    public void clonePayment(SalePayment payment, Sales copy) {
        SalePayment paymentCopy = (SalePayment) payment.clone();
        paymentCopy.setId(null);
        paymentCopy.setCreatedAt(copy.getCreatedAt());
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
    public List<SalePayment> findAllBySalesId(Long id) {
        return paymentRepository.findAllBySaleId(id);
    }

    @Override
    public Set<SalePayment> buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO, User user) {
        Set<SalePayment> payments = new HashSet<>();
        saleDTO
            .getPayments()
            .forEach(paymentDTO -> {
                SalePayment payment = buildPaymentFromFromPaymentDTO(sales, paymentDTO);
                paymentRepository.save(payment);
                payments.add(payment);
            });
        return payments;
    }

    @Override
    public void buildPaymentFromFromPaymentDTO(Sales sales, SaleDTO saleDTO) {
        removeOldPayment(sales);
        saleDTO.getPayments().forEach(paymentDTO -> paymentRepository.save(buildPaymentFromFromPaymentDTO(sales, paymentDTO)));
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
        payment.setCreatedAt(LocalDateTime.now());
        payment.setSale(sales);
        payment.setCashRegister(sales.getCashRegister());
        if (paymentDTO.getPaymentMode() != null) {
            PaymentMode paymentMode = paymentModeRepository.getReferenceById(paymentDTO.getPaymentMode().getCode());
            ModePaimentCode modePaimentCode = ModePaimentCode.valueOf(paymentMode.getCode());
            if (modePaimentCode == ModePaimentCode.CASH) {
                payment.setMontantVerse(paymentDTO.getMontantVerse());
            }
            payment.setPaymentMode(paymentMode);
        }

        payment.setReelAmount(paymentDTO.getNetAmount());
        payment.setPaidAmount(paymentDTO.getPaidAmount());
        payment.setExpectedAmount(paymentDTO.getNetAmount());
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
}
