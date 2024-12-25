package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementGroupeFactureService extends AbstractReglementService {

    private final FacturationRepository facturationRepository;
    private final ReglementFactureModeAllService reglementFactureModeAllService;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public ReglementGroupeFactureService(
        CashRegisterService cashRegisterService,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        WarehouseCalendarService warehouseCalendarService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        BanqueRepository banqueRepository,
        ReglementFactureModeAllService reglementFactureModeAllService
    ) {
        super(
            cashRegisterService,
            paymentTransactionRepository,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            warehouseCalendarService,
            thirdPartySaleLineRepository,
            banqueRepository
        );
        this.facturationRepository = facturationRepository;
        this.reglementFactureModeAllService = reglementFactureModeAllService;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
    }

    @Override
    public ResponseReglementDTO doReglement(ReglementParam reglementParam) throws CashRegisterException, PaymentAmountException {
        FactureTiersPayant factureTiersPayant = this.facturationRepository.findById(reglementParam.getId()).orElseThrow();

        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, reglementParam);
        int montantPaye = 0;

        List<InvoicePayment> invoicePayments = new ArrayList<>();
        int totalAmount = (int) this.thirdPartySaleLineRepository.sumMontantAttenduGroupeFacture(factureTiersPayant.getId());
        if (totalAmount > reglementParam.getAmount()) {
            throw new PaymentAmountException();
        }
        boolean partielPayment = false;
        for (FactureTiersPayant item : factureTiersPayant.getFactureTiersPayants()) {
            var invoicePaymentItem = this.reglementFactureModeAllService.doReglement(invoicePayment, item);
            montantPaye += invoicePaymentItem.getPaidAmount();
            invoicePayments.add(invoicePaymentItem);
            if (item.getStatut() == InvoiceStatut.PARTIALLY_PAID) {
                partielPayment = true;
            }
        }

        super.updateFactureTiersPayant(factureTiersPayant, montantPaye);
        factureTiersPayant.setStatut(partielPayment ? InvoiceStatut.PARTIALLY_PAID : InvoiceStatut.PAID);
        super.saveFactureTiersPayant(factureTiersPayant);
        invoicePayment.setAmount(totalAmount);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment.setRestToPay(totalAmount - montantPaye);
        invoicePayment = super.saveInvoicePayment(invoicePayment);
        for (InvoicePayment item : invoicePayments) {
            item.setParent(invoicePayment);
        }
        super.saveInvoicePayments(invoicePayments);
        super.savePaymentTransaction(invoicePayment, reglementParam.getComment());
        return new ResponseReglementDTO(invoicePayment.getId(), factureTiersPayant.getStatut() == InvoiceStatut.PAID);
    }
}
