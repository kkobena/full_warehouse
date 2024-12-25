package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional(readOnly = true)
public class ReglementDataServiceImpl implements ReglementDataService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public ReglementDataServiceImpl(
        FacturationRepository facturationRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository
    ) {
        this.facturationRepository = facturationRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
    }

    @Override
    @Transactional
    public void deleteReglement(Long idReglement) {
        InvoicePayment invoicePayment = invoicePaymentRepository.findById(idReglement).orElseThrow();
        List<InvoicePayment> invoicePayments = invoicePayment.getInvoicePayments();
        if (CollectionUtils.isEmpty(invoicePayments)) {
            deleteInvoicePayment(invoicePayment, null);
        } else {
            FactureTiersPayant groupeFacture = invoicePayment.getFactureTiersPayant();
            int totalAmount = 0;
            for (InvoicePayment payment : invoicePayments) {
                totalAmount += deleteInvoicePayment(payment, groupeFacture);
            }
            updateFactureTiersPayantStatus(groupeFacture, totalAmount);
            facturationRepository.save(groupeFacture);
            paymentTransactionRepository.deleteByOrganismeId(invoicePayment.getId());
            invoicePaymentRepository.delete(invoicePayment);
        }
    }

    private int deleteInvoicePayment(InvoicePayment invoicePayment, FactureTiersPayant groupeFacture) {
        FactureTiersPayant factureTiersPayant = invoicePayment.getFactureTiersPayant();
        int totalAmount = 0;
        int paidAmount = 0;
        for (InvoicePaymentItem invoicePaymentItem : invoicePayment.getInvoicePaymentItems()) {
            ThirdPartySaleLine thirdPartySaleLine = invoicePaymentItem.getThirdPartySaleLine();
            thirdPartySaleLine.setMontantRegle(thirdPartySaleLine.getMontantRegle() - invoicePaymentItem.getPaidAmount());
            factureTiersPayant.setMontantRegle(factureTiersPayant.getMontantRegle() - invoicePaymentItem.getPaidAmount());
            totalAmount += thirdPartySaleLine.getMontant();
            paidAmount += invoicePaymentItem.getPaidAmount();
            thirdPartySaleLineRepository.save(thirdPartySaleLine);
        }
        paymentTransactionRepository.deleteByOrganismeId(invoicePayment.getId());
        invoicePaymentRepository.delete(invoicePayment);

        updateFactureTiersPayantStatus(factureTiersPayant, totalAmount);
        facturationRepository.save(factureTiersPayant);
        if (groupeFacture != null) {
            factureTiersPayant.setMontantRegle(groupeFacture.getMontantRegle() - paidAmount);
        }
        return totalAmount;
    }

    private void updateFactureTiersPayantStatus(FactureTiersPayant factureTiersPayant, int totalAmount) {
        if (factureTiersPayant.getMontantRegle() == 0) {
            factureTiersPayant.setStatut(InvoiceStatut.NOT_PAID);
        } else if (factureTiersPayant.getMontantRegle() < totalAmount) {
            factureTiersPayant.setStatut(InvoiceStatut.PARTIALLY_PAID);
        } else {
            factureTiersPayant.setStatut(InvoiceStatut.PAID);
        }
        factureTiersPayant.setUpdated(LocalDateTime.now());
    }
}
