package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementGroupeFactureService")
class ReglementGroupeFactureServiceTest {

    private static final FactureItemId GROUPE_ID = new FactureItemId(7L, LocalDate.of(2026, 4, 18));

    @Mock
    private CashRegisterService cashRegisterService;

    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;

    @Mock
    private UserService userService;

    @Mock
    private FacturationRepository facturationRepository;

    @Mock
    private ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    @Mock
    private BanqueRepository banqueRepository;

    @Mock
    private ReglementFactureModeAllService reglementFactureModeAllService;

    @Mock
    private TransactionIdGeneratorService transactionIdGeneratorService;

    @Mock
    private InvoicePaymentItemService invoicePaymentItemService;

    @Mock
    private ReferenceService referenceService;

    private ReglementGroupeFactureService service;

    @BeforeEach
    void setUp() {
        service = new ReglementGroupeFactureService(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            reglementFactureModeAllService,
            transactionIdGeneratorService,
            invoicePaymentItemService,
            referenceService
        );
        when(cashRegisterService.getCashRegister()).thenReturn(new CashRegister());
        when(userService.getUser()).thenReturn(new AppUser());
        when(transactionIdGeneratorService.nextId()).thenReturn(55L);
        when(referenceService.buildNumTransaction()).thenReturn("TR-0001");
        when(invoicePaymentRepository.save(any(InvoicePayment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private FactureTiersPayant groupe(FactureTiersPayant... filles) {
        FactureTiersPayant groupe = new FactureTiersPayant().setId(7L).setMontantRegle(0);
        groupe.setFactureTiersPayants(new ArrayList<>(List.of(filles)));
        when(facturationRepository.getReferenceById(GROUPE_ID)).thenReturn(groupe);
        return groupe;
    }

    private static FactureTiersPayant fille(long id) {
        return new FactureTiersPayant().setId(id).setMontantRegle(0);
    }

    /** Chaque fille est reglee par le service de facture complete, qui renvoie son paiement. */
    private void stubReglementFille(FactureTiersPayant fille, int paidAmount) {
        InvoicePayment paiementFille = new InvoicePayment();
        paiementFille.setPaidAmount(paidAmount);
        when(reglementFactureModeAllService.doReglement(any(InvoicePayment.class), org.mockito.ArgumentMatchers.eq(fille)))
            .thenReturn(paiementFille);
    }

    private static ReglementParam param(int amount, int totalAmount, int montantFacture) {
        return new ReglementParam()
            .setId(GROUPE_ID)
            .setAmount(amount)
            .setTotalAmount(totalAmount)
            .setMontantFacture(montantFacture)
            .setModePaimentCode(ModePaimentCode.CASH)
            .setComment("reglement groupe");
    }

    @Test
    @DisplayName("refuse un total attendu superieur au montant verse")
    void totalSuperieurAuVerse() {
        groupe(fille(9L));
        ReglementParam param = param(1000, 1500, 1500);

        assertThatThrownBy(() -> service.doReglement(param)).isInstanceOf(PaymentAmountException.class);

        verifyNoInteractions(reglementFactureModeAllService);
    }

    @Test
    @DisplayName("accepte un total attendu egal au montant verse")
    void totalEgalAuVerse() {
        FactureTiersPayant f1 = fille(9L);
        groupe(f1);
        stubReglementFille(f1, 1000);

        assertThat(service.doReglement(param(1000, 1000, 1000)).total()).isTrue();
    }

    @Test
    @DisplayName("marque le paiement comme groupe et cumule le regle de chaque fille")
    void cumuleLesFilles() {
        FactureTiersPayant f1 = fille(9L);
        FactureTiersPayant f2 = fille(10L);
        FactureTiersPayant groupe = groupe(f1, f2);
        stubReglementFille(f1, 1000);
        stubReglementFille(f2, 400);

        ResponseReglementDTO response = service.doReglement(param(2000, 1400, 1400));

        assertThat(groupe.getMontantRegle()).isEqualTo(1400);
        assertThat(groupe.getStatut()).isEqualTo(InvoiceStatut.PAID);
        assertThat(response.total()).isTrue();

        ArgumentCaptor<InvoicePayment> captor = ArgumentCaptor.forClass(InvoicePayment.class);
        verify(invoicePaymentRepository).save(captor.capture());
        InvoicePayment paiement = captor.getValue();
        assertThat(paiement.isGrouped()).isTrue();
        assertThat(paiement.getExpectedAmount()).isEqualTo(1400);
        assertThat(paiement.getPaidAmount()).isEqualTo(1400);
        assertThat(paiement.getReelAmount()).isEqualTo(1400);
        assertThat(paiement.getCommentaire()).isEqualTo("reglement groupe");
    }

    @Test
    @DisplayName("reste partiellement payee quand le regle n atteint pas le montant facture")
    void groupePartiellementPaye() {
        FactureTiersPayant f1 = fille(9L);
        FactureTiersPayant groupe = groupe(f1);
        stubReglementFille(f1, 400);

        ResponseReglementDTO response = service.doReglement(param(1000, 1000, 5000));

        assertThat(groupe.getStatut()).isEqualTo(InvoiceStatut.PARTIALLY_PAID);
        assertThat(response.total()).isFalse();
    }

    @Test
    @DisplayName("rattache chaque paiement fille au paiement de groupe")
    void rattacheLesPaiementsFilles() {
        FactureTiersPayant f1 = fille(9L);
        FactureTiersPayant f2 = fille(10L);
        groupe(f1, f2);
        stubReglementFille(f1, 1000);
        stubReglementFille(f2, 400);

        service.doReglement(param(2000, 1400, 1400));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InvoicePayment>> captor = ArgumentCaptor.forClass(List.class);
        verify(invoicePaymentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allSatisfy(p -> assertThat(p.getParent()).isNotNull());
    }

    @Test
    @DisplayName("un groupe sans facture fille ne regle rien")
    void groupeSansFille() {
        FactureTiersPayant groupe = groupe();

        service.doReglement(param(1000, 0, 0));

        assertThat(groupe.getMontantRegle()).isZero();
        assertThat(groupe.getStatut()).isEqualTo(InvoiceStatut.PAID);
        verify(invoicePaymentRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("enregistre la facture de groupe")
    void persiste() {
        FactureTiersPayant groupe = groupe();

        service.doReglement(param(1000, 0, 0));

        verify(facturationRepository).save(groupe);
    }
}
