package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
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
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.reglement.dto.LigneSelectionnesDTO;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementFactureSelectionneesService extends AbstractReglementService {

    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final FacturationRepository facturationRepository;

    public ReglementFactureSelectionneesService(
        CashRegisterService cashRegisterService,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        WarehouseCalendarService warehouseCalendarService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
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
            banqueRepository
        );
        this.facturationRepository = facturationRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
    }

    @Override
    public ResponseReglementDTO doReglement(ReglementParam reglementParam)
        throws CashRegisterException, PaymentAmountException, GenericError {
        if (reglementParam.getDossierIds().isEmpty()) {
            throw new GenericError("Aucun dossiers à regler");
        }
        List<ThirdPartySaleLine> thirdPartySaleLinesUpdated = new ArrayList<>();
        FactureTiersPayant factureTiersPayant = facturationRepository.findById(reglementParam.getId()).orElseThrow();
        List<ThirdPartySaleLine> thirdPartySaleLines = getThirdPartySaleLines(reglementParam);

        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, reglementParam);
        int montantPaye = 0;
        int totalAmount = reglementParam.getTotalAmount();
        int montantVerse = reglementParam.getAmount();
        for (ThirdPartySaleLine thirdParty : thirdPartySaleLines) {
            if (montantVerse <= 0) {
                break;
            }

            int itemAmountToPay = thirdParty.getMontant() - thirdParty.getMontantRegle();
            int itemAmount = itemAmountToPay;
            if (montantVerse >= itemAmountToPay) {
                montantVerse -= itemAmountToPay;
            } else {
                itemAmount = montantVerse;
                montantVerse = 0;
            }
            montantPaye += itemAmount;
            invoicePayment.getInvoicePaymentItems().add(super.buildInvoicePaymentItem(thirdParty, invoicePayment, itemAmount));
            super.updateThirdPartyLine(thirdParty, itemAmount);
            thirdPartySaleLinesUpdated.add(thirdParty);
        }

        super.updateFactureTiersPayant(factureTiersPayant, montantPaye);
        super.updateStatut(factureTiersPayant, reglementParam.getMontantFacture());
        super.saveFactureTiersPayant(factureTiersPayant);
        super.saveThirdPartyLines(thirdPartySaleLinesUpdated);
        invoicePayment.setAmount(totalAmount);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment = super.saveInvoicePayment(invoicePayment);
        super.savePaymentTransaction(invoicePayment, reglementParam.getComment());
        return new ResponseReglementDTO(invoicePayment.getId(), factureTiersPayant.getStatut() == InvoiceStatut.PAID);
    }

    public InvoicePayment doReglement(
        InvoicePayment groupeInvoicePayment,
        FactureTiersPayant factureTiersPayant,
        int montantFacture,
        LigneSelectionnesDTO item
    ) {
        List<ThirdPartySaleLine> thirdPartySaleLinesUpdated = new ArrayList<>();
        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, groupeInvoicePayment);
        int montantPaye = 0;
        int totalAmount = item.getMontantAttendu();
        int montantVerse = montantFacture;
        for (ThirdPartySaleLine thirdParty : factureTiersPayant.getFacturesDetails()) {
            if (montantVerse <= 0) {
                break;
            }

            int itemAmountToPay = thirdParty.getMontant() - thirdParty.getMontantRegle();
            int itemAmount = itemAmountToPay;
            if (montantVerse >= itemAmountToPay) {
                montantVerse -= itemAmountToPay;
            } else {
                itemAmount = montantVerse;
                montantVerse = 0;
            }
            montantPaye += itemAmount;
            invoicePayment.getInvoicePaymentItems().add(super.buildInvoicePaymentItem(thirdParty, invoicePayment, itemAmount));
            super.updateThirdPartyLine(thirdParty, itemAmount);
            thirdPartySaleLinesUpdated.add(thirdParty);
        }

        super.updateFactureTiersPayant(factureTiersPayant, montantPaye);
        super.updateStatut(factureTiersPayant, item.getMontantFacture());
        super.saveFactureTiersPayant(factureTiersPayant);
        super.saveThirdPartyLines(thirdPartySaleLinesUpdated);
        invoicePayment.setAmount(totalAmount);
        invoicePayment.setPaidAmount(montantPaye);
        return invoicePayment;
    }

    private List<ThirdPartySaleLine> getThirdPartySaleLines(ReglementParam reglementParam) {
        return this.thirdPartySaleLineRepository.findAll(
                Specification.where(this.thirdPartySaleLineRepository.selectionBonCriteria(Set.copyOf(reglementParam.getDossierIds())))
            );
    }
}
