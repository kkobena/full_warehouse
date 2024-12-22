package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementGroupeSelectionFactureService extends AbstractReglementService {

    private final FacturationRepository facturationRepository;
    private final ReglementFactureSelectionneesService reglementFactureSelectionneesService;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public ReglementGroupeSelectionFactureService(
        CashRegisterService cashRegisterService,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        WarehouseCalendarService warehouseCalendarService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        InvoicePaymentItemRepository invoicePaymentItemRepository,
        BanqueRepository banqueRepository,
        ReglementFactureSelectionneesService reglementFactureSelectionneesService
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
        this.reglementFactureSelectionneesService = reglementFactureSelectionneesService;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
    }

    @Override
    public ResponseReglementDTO doReglement(ReglementParam reglementParam) throws CashRegisterException, PaymentAmountException {
        FactureTiersPayant factureTiersPayant =
            this.facturationRepository.findById(reglementParam.getDossierIds().getFirst()).orElseThrow();

        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, reglementParam);
        List<FactureTiersPayant> factureTiersPayants;
        if (reglementParam.getDossierIds().isEmpty()) {
            factureTiersPayants = factureTiersPayant.getFactureTiersPayants();
        } else {
            factureTiersPayants = this.facturationRepository.findAll(
                    Specification.where(this.facturationRepository.fetchByIs(new HashSet<>(reglementParam.getDossierIds())))
                );
        }

        int montantPaye = 0;
        int montantVerse = reglementParam.getAmount();

        List<InvoicePayment> invoicePayments = new ArrayList<>();
        int totalAmount = (int) this.thirdPartySaleLineRepository.sumMontantAttenduGroupeFacture(factureTiersPayant.getId());
        for (FactureTiersPayant item : factureTiersPayants) {
            if (montantVerse <= 0) {
                break;
            }
            int itemAmountToPay = (int) this.thirdPartySaleLineRepository.sumMontantAttenduByFactureTiersPayantId(item.getId());

            int itemAmount = itemAmountToPay;
            if (montantVerse >= itemAmountToPay) {
                montantVerse -= itemAmountToPay;
            } else {
                itemAmount = montantVerse;
                montantVerse = 0;
            }
            montantPaye += itemAmount;
            var invoicePaymentItem = this.reglementFactureSelectionneesService.doReglement(invoicePayment, item, itemAmount);
            invoicePayments.add(invoicePaymentItem);
        }

        super.updateFactureTiersPayant(factureTiersPayant, totalAmount, montantPaye);
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
        return new ResponseReglementDTO(invoicePayment.getId());
    }
}
