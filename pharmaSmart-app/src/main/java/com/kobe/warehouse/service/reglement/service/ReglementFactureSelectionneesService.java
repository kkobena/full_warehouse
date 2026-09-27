package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
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
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementFactureSelectionneesService extends AbstractReglementService {

    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public ReglementFactureSelectionneesService(
        CashRegisterService cashRegisterService,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        BanqueRepository banqueRepository,
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
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
    }

    @Override
    public ResponseReglementDTO doReglement(ReglementParam reglementParam)
        throws CashRegisterException, PaymentAmountException, GenericError {
        if (reglementParam.getDossierIds().isEmpty()) {
            throw new GenericError("Aucun dossiers à regler");
        }
        List<ThirdPartySaleLine> thirdPartySaleLinesUpdated = new ArrayList<>();
        FactureTiersPayant factureTiersPayant = verrouillerFacture(reglementParam.getId());
        List<ThirdPartySaleLine> thirdPartySaleLines = getThirdPartySaleLines(reglementParam);
        verifierDossiers(factureTiersPayant, thirdPartySaleLines, reglementParam.getDossierIds());

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

        refuserSiRienRegle(montantPaye);
        super.updateFactureTiersPayant(factureTiersPayant, montantPaye);
        appliquerStatut(factureTiersPayant);
        super.saveFactureTiersPayant(factureTiersPayant);
        super.saveThirdPartyLines(thirdPartySaleLinesUpdated);
        invoicePayment.setExpectedAmount(totalAmount);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment.setReelAmount(montantPaye);
        invoicePayment = super.saveInvoicePayment(invoicePayment);
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
        appliquerStatut(factureTiersPayant);
        super.saveFactureTiersPayant(factureTiersPayant);
        super.saveThirdPartyLines(thirdPartySaleLinesUpdated);
        invoicePayment.setExpectedAmount(totalAmount);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment.setReelAmount(montantPaye);
        return invoicePayment;
    }

    /**
     * Les dossiers sont chargés par leurs seuls identifiants : on vérifie qu'ils existent tous, qu'ils
     * appartiennent à la facture réglée, et qu'aucun n'a été soldé depuis l'affichage de l'écran —
     * ce dernier cas est précisément celui d'un double règlement.
     */
    private static void verifierDossiers(FactureTiersPayant facture, List<ThirdPartySaleLine> dossiers, List<Long> dossierIds) {
        if (dossiers.size() != Set.copyOf(dossierIds).size()) {
            throw new GenericError("Un ou plusieurs dossiers sélectionnés sont introuvables ; rechargez la facture", "dossierNotFound");
        }
        for (ThirdPartySaleLine dossier : dossiers) {
            FactureTiersPayant factureDuDossier = dossier.getFactureTiersPayant();
            if (factureDuDossier == null || !facture.getId().equals(factureDuDossier.getId())) {
                throw new GenericError("Un dossier sélectionné n'appartient pas à cette facture", "dossierHorsFacture");
            }
            if (resteDu(dossier) <= 0) {
                throw new GenericError("Un ou plusieurs dossiers sélectionnés sont déjà réglés ; rechargez la facture", "dossierDejaRegle");
            }
        }
    }

    private List<ThirdPartySaleLine> getThirdPartySaleLines(ReglementParam reglementParam) {
        return this.thirdPartySaleLineRepository.findAll(
                this.thirdPartySaleLineRepository.selectionBonCriteria(Set.copyOf(reglementParam.getDossierIds()))
            );
    }
}
