package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.reglement.dto.BanqueInfoDTO;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.sale.impl.ConsommationPlafondService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public abstract class AbstractReglementService implements ReglementService {

    private final CashRegisterService cashRegisterService;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final UserService userService;
    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final BanqueRepository banqueRepository;
    private final TransactionIdGeneratorService transactionIdGeneratorService;
    private final InvoicePaymentItemService invoicePaymentItemService;
    private final ReferenceService referenceService;

    protected AbstractReglementService(
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
        this.cashRegisterService = cashRegisterService;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.userService = userService;
        this.facturationRepository = facturationRepository;

        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.banqueRepository = banqueRepository;
        this.transactionIdGeneratorService = transactionIdGeneratorService;
        this.invoicePaymentItemService = invoicePaymentItemService;
        this.referenceService = referenceService;
    }

    private ConsommationPlafondService consommationPlafondService;

    @Autowired
    public void setConsommationPlafondService(ConsommationPlafondService consommationPlafondService) {
        this.consommationPlafondService = consommationPlafondService;
    }

    /**
     * Verrou pessimiste sur la facture réglée, pris avant toute lecture : deux règlements de la même
     * facture s'exécutent l'un après l'autre, et le second voit le reste dû laissé par le premier.
     */
    protected FactureTiersPayant verrouillerFacture(FactureItemId id) {
        FactureTiersPayant facture = facturationRepository.verrouiller(id)
            .orElseThrow(() -> new GenericError("Facture introuvable", "factureNotFound"));
        refuserSiProvisoire(facture);
        return facture;
    }

    /**
     * Une facture provisoire est un brouillon : ses dossiers seront repris par la facture
     * définitive, qui seule se règle. La régler rattacherait le paiement à une facture appelée à
     * être remplacée. Ses règlements antérieurs à cette règle restent annulables.
     */
    protected static void refuserSiProvisoire(FactureTiersPayant facture) {
        if (facture.isFactureProvisoire()) {
            throw new GenericError(
                "La facture " + facture.getNumFacture() + " est provisoire : seule la facture définitive se règle",
                "factureProvisoire"
            );
        }
    }

    /** Groupe puis filles, dans un ordre stable : un règlement individuel d'une fille ne peut pas l'interbloquer. */
    protected FactureTiersPayant verrouillerFactureGroupe(FactureItemId id) {
        FactureTiersPayant groupe = verrouillerFacture(id);
        facturationRepository.verrouillerFilles(id);
        return groupe;
    }

    protected static int resteDu(ThirdPartySaleLine dossier) {
        return Math.max(Objects.requireNonNullElse(dossier.getMontant(), 0) - Objects.requireNonNullElse(dossier.getMontantRegle(), 0), 0);
    }

    protected static int resteDu(FactureTiersPayant facture) {
        return facture.getFacturesDetails().stream().mapToInt(AbstractReglementService::resteDu).sum();
    }

    protected static int resteDuGroupe(FactureTiersPayant groupe) {
        return groupe.getFactureTiersPayants().stream().mapToInt(AbstractReglementService::resteDu).sum();
    }

    /**
     * Statut déduit des dossiers relus sous verrou : soldée si et seulement si plus aucun dossier ne
     * doit rien. Il ne dépend ni du montant envoyé par l'écran (périmé, et limité aux dossiers non
     * soldés), ni du cumul {@code montantRegle} de la facture.
     */
    protected static void appliquerStatut(FactureTiersPayant facture) {
        facture.setStatut(resteDu(facture) > 0 ? InvoiceStatut.PARTIALLY_PAID : InvoiceStatut.PAID);
    }

    protected static void appliquerStatutGroupe(FactureTiersPayant groupe) {
        groupe.setStatut(resteDuGroupe(groupe) > 0 ? InvoiceStatut.PARTIALLY_PAID : InvoiceStatut.PAID);
    }

    /** Un second règlement d'une facture soldée est un doublon : on le refuse au lieu d'enregistrer une pièce à 0. */
    protected static void refuserSiSoldee(int resteDu) {
        if (resteDu <= 0) {
            throw new GenericError("Cette facture est déjà entièrement réglée", "factureDejaReglee");
        }
    }

    protected static void refuserSiRienRegle(int montantPaye) {
        if (montantPaye <= 0) {
            throw new GenericError("Aucun montant à régler : les dossiers sélectionnés sont déjà soldés", "factureDejaReglee");
        }
    }

    protected CashRegister getCashRegister() {
        return cashRegisterService.getCashRegister();
    }

    protected InvoicePaymentItem buildInvoicePaymentItem(ThirdPartySaleLine thirdPartySaleLine, InvoicePayment invoicePayment, int amount) {
        return this.invoicePaymentItemService.buildInvoicePaymentItem(thirdPartySaleLine, invoicePayment, amount);
    }

    protected void updateThirdPartyLine(ThirdPartySaleLine thirdPartySaleLine, int amount) {
        thirdPartySaleLine.setMontantRegle(thirdPartySaleLine.getMontantRegle() + amount);
        if (consommationPlafondService != null) {
            consommationPlafondService.imputerReglement(thirdPartySaleLine, amount);
        }
        thirdPartySaleLine.setEffectiveUpdateDate(LocalDateTime.now());
        thirdPartySaleLine.setUpdated(thirdPartySaleLine.getEffectiveUpdateDate());
        if (thirdPartySaleLine.getMontant() <= thirdPartySaleLine.getMontantRegle()) {
            thirdPartySaleLine.setStatut(ThirdPartySaleStatut.PAID);
        } else {
            thirdPartySaleLine.setStatut(ThirdPartySaleStatut.HALF_PAID);
        }
    }

    private InvoicePayment getNew() {
        InvoicePayment invoice = new InvoicePayment();
        invoice.setId(this.transactionIdGeneratorService.nextId());
        invoice.setTransactionNumber(referenceService.buildNumTransaction());
        return invoice;
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, ReglementParam reglementParam) {
        InvoicePayment invoice = getNew().setFactureTiersPayant(factureTiersPayant);
        invoice
            .setCashRegister(getCashRegister())
            .setMontantVerse(reglementParam.getAmount())
            .setTransactionDate(Objects.requireNonNullElse(reglementParam.getPaymentDate(), LocalDate.now()));
        invoice.setPaymentMode(fromCode(reglementParam.getModePaimentCode()));
        invoice.setBanque(buildBanque(reglementParam.getBanqueInfo()));
        return invoice;
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, InvoicePayment paymentParent) {
        InvoicePayment invoicePayment = getNew();
        invoicePayment.setBanque(paymentParent.getBanque());
        invoicePayment.setFactureTiersPayant(factureTiersPayant);
        invoicePayment.setCashRegister(paymentParent.getCashRegister());
        invoicePayment.setPaymentMode(paymentParent.getPaymentMode());
        invoicePayment.setTransactionDate(paymentParent.getTransactionDate());
        return invoicePayment;
    }

    private PaymentMode fromCode(ModePaimentCode mode) {
        return new PaymentMode().code(mode.name());
    }

    protected void updateFactureTiersPayant(FactureTiersPayant factureTiersPayant, int paidAmount) {
        factureTiersPayant.setMontantRegle(Objects.requireNonNullElse(factureTiersPayant.getMontantRegle(), 0) + paidAmount);
        factureTiersPayant.setUser(userService.getUser());
        factureTiersPayant.setUpdated(LocalDateTime.now());
    }

    protected InvoicePayment saveInvoicePayment(InvoicePayment invoicePayment) {
        invoicePayment.setTypeFinancialTransaction(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT);
        return invoicePaymentRepository.save(invoicePayment);
    }

    protected void saveFactureTiersPayant(FactureTiersPayant factureTiersPayant) {
        facturationRepository.save(factureTiersPayant);
    }

    protected void saveThirdPartyLines(List<ThirdPartySaleLine> thirdPartySaleLines) {
        thirdPartySaleLineRepository.saveAll(thirdPartySaleLines);
    }

    protected Banque buildBanque(BanqueInfoDTO banqueInfo) {
        if (Objects.isNull(banqueInfo)) {
            return null;
        }
        return banqueRepository.save(
            new Banque().setCode(banqueInfo.getCode()).setNom(banqueInfo.getNom()).setBeneficiaire(banqueInfo.getBeneficiaire())
        );
    }

    protected void saveInvoicePayments(List<InvoicePayment> items) {
        // Les paiements fils d'un règlement groupé n'empruntent pas saveInvoicePayment : sans ce
        // marquage, ils partiraient en base avec un type_transaction nul, que la colonne refuse.
        items.forEach(item -> item.setTypeFinancialTransaction(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT));
        invoicePaymentRepository.saveAll(items);
    }

    protected void updateStatut(FactureTiersPayant factureTiersPayant, int montantFacture) {
        if (Objects.requireNonNullElse(factureTiersPayant.getMontantRegle(), 0) < montantFacture) {
            factureTiersPayant.setStatut(InvoiceStatut.PARTIALLY_PAID);
        } else {
            factureTiersPayant.setStatut(InvoiceStatut.PAID);
        }
    }
}
