package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.reglement.dto.LigneSelectionnesDTO;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementGroupeSelectionFactureService extends AbstractReglementService {

    private final FacturationRepository facturationRepository;
    private final ReglementFactureSelectionneesService reglementFactureSelectionneesService;

    public ReglementGroupeSelectionFactureService(
        CashRegisterService cashRegisterService,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        BanqueRepository banqueRepository,
        ReglementFactureSelectionneesService reglementFactureSelectionneesService,
        TransactionIdGeneratorService transactionIdGeneratorService,
        InvoicePaymentItemService invoicePaymentItemService,
        ReferenceService referenceService
    ) {
        super(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            transactionIdGeneratorService,
            invoicePaymentItemService,
            referenceService
        );
        this.facturationRepository = facturationRepository;
        this.reglementFactureSelectionneesService = reglementFactureSelectionneesService;
    }

    @Override
    public ResponseReglementDTO doReglement(ReglementParam reglementParam)
        throws CashRegisterException, PaymentAmountException, GenericError {
        List<LigneSelectionnesDTO> ligneSelectionnes = reglementParam.getLigneSelectionnes();
        if (ligneSelectionnes.isEmpty()) {
            throw new GenericError("Aucun dossiers à regler");
        }
        FactureTiersPayant factureTiersPayant = this.facturationRepository.getReferenceById(reglementParam.getId());

        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, reglementParam);
        invoicePayment.setGrouped(true);
        int montantPaye = 0;
        int montantVerse = reglementParam.getAmount();

        List<InvoicePayment> invoicePayments = new ArrayList<>();
        int totalAmount = reglementParam.getTotalAmount();
        for (LigneSelectionnesDTO item : ligneSelectionnes) {
            if (montantVerse <= 0) {
                break;
            }
            int itemAmountToPay = item.getMontantVerse();
            int itemAmount = itemAmountToPay;
            if (montantVerse >= itemAmountToPay) {
                montantVerse -= itemAmountToPay;
            } else {
                itemAmount = montantVerse;
                montantVerse = 0;
            }
            montantPaye += itemAmount;
            FactureTiersPayant facture = this.facturationRepository.getReferenceById(item.getId());
            var invoicePaymentItem = this.reglementFactureSelectionneesService.doReglement(invoicePayment, facture, itemAmount, item);
            invoicePayments.add(invoicePaymentItem);
        }
        super.updateFactureTiersPayant(factureTiersPayant, montantPaye);
        factureTiersPayant.setStatut(
            factureTiersPayant.getMontantRegle() < reglementParam.getMontantFacture() ? InvoiceStatut.PARTIALLY_PAID : InvoiceStatut.PAID
        );
        super.saveFactureTiersPayant(factureTiersPayant);
        invoicePayment.setExpectedAmount(totalAmount);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment.setReelAmount(montantPaye);
        invoicePayment = super.saveInvoicePayment(invoicePayment);
        for (InvoicePayment item : invoicePayments) {
            item.setParent(invoicePayment);
        }
        super.saveInvoicePayments(invoicePayments);
        return new ResponseReglementDTO(invoicePayment.getId(), factureTiersPayant.getStatut() == InvoiceStatut.PAID);
    }
}
