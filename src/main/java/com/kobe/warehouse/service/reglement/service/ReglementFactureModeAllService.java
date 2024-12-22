package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentItemRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementFactureModeAllService extends AbstractReglementService {

    private final FacturationRepository facturationRepository;

    public ReglementFactureModeAllService(
        CashRegisterService cashRegisterService,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        WarehouseCalendarService warehouseCalendarService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        InvoicePaymentItemRepository invoicePaymentItemRepository,
        BanqueRepository banqueRepository
    ) {
        super(
            cashRegisterService,
            paymentTransactionRepository,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            warehouseCalendarService,
            thirdPartySaleLineRepository,
            invoicePaymentItemRepository,
            banqueRepository
        );
        this.facturationRepository = facturationRepository;
    }

    @Override
    public ResponseReglementDTO doReglement(ReglementParam reglementParam) throws CashRegisterException, PaymentAmountException {
        FactureTiersPayant factureTiersPayant = getFactureTiersPayant(reglementParam);

        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, reglementParam);
        int montantPaye = 0;
        int totalAmount = 0;
        for (ThirdPartySaleLine thirdParty : factureTiersPayant.getFacturesDetails()) {
            totalAmount = thirdParty.getMontant();
            int itemAmount = thirdParty.getMontant() - thirdParty.getMontantRegle();
            montantPaye += itemAmount;
            invoicePayment.getInvoicePaymentItems().add(super.buildInvoicePaymentItem(thirdParty, invoicePayment, itemAmount));
            super.updateThirdPartyLine(thirdParty, itemAmount);
        }
        if (montantPaye > reglementParam.getAmount()) {
            throw new PaymentAmountException();
        }
        super.updateFactureTiersPayant(factureTiersPayant, totalAmount, montantPaye);
        super.saveFactureTiersPayant(factureTiersPayant);
        super.saveThirdPartyLines(factureTiersPayant.getFacturesDetails());
        invoicePayment.setAmount(montantPaye);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment.setRestToPay(totalAmount - montantPaye);
        invoicePayment = super.saveInvoicePayment(invoicePayment);
        super.savePaymentTransaction(invoicePayment, reglementParam.getComment());
        return new ResponseReglementDTO(invoicePayment.getId());
    }

    private FactureTiersPayant getFactureTiersPayant(ReglementParam reglementParam) {
        return facturationRepository.findById(reglementParam.getDossierIds().getFirst()).orElseThrow();
    }

    public InvoicePayment doReglement(InvoicePayment groupeInvoicePayment, FactureTiersPayant factureTiersPayant) {
        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, groupeInvoicePayment);
        int montantPaye = 0;
        int totalAmount = 0;
        for (ThirdPartySaleLine thirdParty : factureTiersPayant.getFacturesDetails()) {
            totalAmount = thirdParty.getMontant();
            int itemAmount = thirdParty.getMontant() - thirdParty.getMontantRegle();
            montantPaye += itemAmount;
            invoicePayment.getInvoicePaymentItems().add(super.buildInvoicePaymentItem(thirdParty, invoicePayment, itemAmount));
            super.updateThirdPartyLine(thirdParty, itemAmount);
        }
        super.updateFactureTiersPayant(factureTiersPayant, totalAmount, montantPaye);
        super.saveFactureTiersPayant(factureTiersPayant);
        super.saveThirdPartyLines(factureTiersPayant.getFacturesDetails());
        invoicePayment.setAmount(montantPaye);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment.setRestToPay(totalAmount - montantPaye);
        return invoicePayment;
    }
}
