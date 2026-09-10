package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
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
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementFactureModeAllService")
class ReglementFactureModeAllServiceTest {

    private static final FactureItemId FACTURE_ID = new FactureItemId(7L, LocalDate.of(2026, 4, 18));

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
    private TransactionIdGeneratorService transactionIdGeneratorService;

    @Mock
    private InvoicePaymentItemService invoicePaymentItemService;

    @Mock
    private ReferenceService referenceService;

    private ReglementFactureModeAllService service;

    @BeforeEach
    void setUp() {
        service = new ReglementFactureModeAllService(
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
        when(cashRegisterService.getCashRegister()).thenReturn(new CashRegister());
        when(userService.getUser()).thenReturn(new AppUser());
        when(transactionIdGeneratorService.nextId()).thenReturn(55L);
        when(referenceService.buildNumTransaction()).thenReturn("TR-0001");
        when(invoicePaymentRepository.save(any(InvoicePayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePaymentItemService.buildInvoicePaymentItem(any(), any(), anyInt())).thenAnswer(inv -> {
            InvoicePaymentItem item = new InvoicePaymentItem();
            item.setThirdPartySaleLine(inv.getArgument(0));
            item.setInvoicePayment(inv.getArgument(1));
            item.setPaidAmount(inv.getArgument(2));
            return item;
        });
    }

    private static ThirdPartySaleLine ligne(int montant, int montantRegle) {
        return new ThirdPartySaleLine().setMontant(montant).setMontantRegle(montantRegle);
    }

    private FactureTiersPayant facture(ThirdPartySaleLine... lignes) {
        FactureTiersPayant facture = new FactureTiersPayant().setId(7L).setMontantRegle(0);
        facture.setFacturesDetails(new ArrayList<>(List.of(lignes)));
        when(facturationRepository.getReferenceById(FACTURE_ID)).thenReturn(facture);
        return facture;
    }

    private static ReglementParam param(int amount, int montantFacture) {
        return new ReglementParam()
            .setId(FACTURE_ID)
            .setAmount(amount)
            .setMontantFacture(montantFacture)
            .setModePaimentCode(ModePaimentCode.CASH)
            .setComment("solde de tout compte");
    }

    @Nested
    @DisplayName("doReglement depuis un parametre")
    class DoReglementDepuisParam {

        @Test
        @DisplayName("solde chaque bon et cloture la facture")
        void soldeLaFacture() {
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 100);
            FactureTiersPayant facture = facture(l1, l2);

            ResponseReglementDTO response = service.doReglement(param(1400, 1400));

            assertThat(l1.getMontantRegle()).isEqualTo(1000);
            assertThat(l1.getStatut()).isEqualTo(ThirdPartySaleStatut.PAID);
            assertThat(l2.getMontantRegle()).isEqualTo(500);
            assertThat(l2.getStatut()).isEqualTo(ThirdPartySaleStatut.PAID);
            assertThat(facture.getMontantRegle()).isEqualTo(1400);
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.PAID);
            assertThat(response.total()).isTrue();
            assertThat(response.id().getId()).isEqualTo(55L);
        }

        @Test
        @DisplayName("valorise le paiement au total effectivement solde")
        void valorisationDuPaiement() {
            facture(ligne(1000, 0), ligne(500, 100));

            service.doReglement(param(1400, 1400));

            ArgumentCaptor<InvoicePayment> captor = ArgumentCaptor.forClass(InvoicePayment.class);
            verify(invoicePaymentRepository).save(captor.capture());
            InvoicePayment paiement = captor.getValue();
            assertThat(paiement.getReelAmount()).isEqualTo(1400);
            assertThat(paiement.getPaidAmount()).isEqualTo(1400);
            assertThat(paiement.getExpectedAmount()).isEqualTo(1400);
            assertThat(paiement.getCommentaire()).isEqualTo("solde de tout compte");
            assertThat(paiement.getInvoicePaymentItems()).hasSize(2);
        }

        @Test
        @DisplayName("reste partiellement payee quand le montant facture depasse le solde des bons")
        void resteFacturePartielle() {
            FactureTiersPayant facture = facture(ligne(1000, 0));

            ResponseReglementDTO response = service.doReglement(param(1000, 5000));

            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.PARTIALLY_PAID);
            assertThat(response.total()).isFalse();
        }

        @Test
        @DisplayName("une facture sans bon ne solde rien")
        void factureSansBon() {
            FactureTiersPayant facture = facture();

            service.doReglement(param(0, 0));

            assertThat(facture.getMontantRegle()).isZero();
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.PAID);
        }

        @Test
        @DisplayName("un bon deja solde n ajoute rien au paiement")
        void bonDejaSolde() {
            ThirdPartySaleLine l1 = ligne(1000, 1000);
            FactureTiersPayant facture = facture(l1);

            service.doReglement(param(0, 0));

            assertThat(l1.getMontantRegle()).isEqualTo(1000);
            assertThat(facture.getMontantRegle()).isZero();
        }

        @Test
        @DisplayName("enregistre la facture et ses bons")
        void persiste() {
            FactureTiersPayant facture = facture(ligne(1000, 0));

            service.doReglement(param(1000, 1000));

            verify(facturationRepository).save(facture);
            verify(thirdPartySaleLineRepository).saveAll(facture.getFacturesDetails());
        }
    }

    @Nested
    @DisplayName("doReglement depuis un paiement de groupe")
    class DoReglementDepuisGroupe {

        private InvoicePayment paiementParent() {
            InvoicePayment parent = new InvoicePayment();
            parent.setBanque(new Banque().setCode("BQ1"));
            parent.setCashRegister(new CashRegister());
            parent.setPaymentMode(new PaymentMode().code("CASH"));
            parent.setTransactionDate(LocalDate.of(2026, 4, 18));
            return parent;
        }

        @Test
        @DisplayName("solde chaque bon de la facture fille")
        void soldeLaFactureFille() {
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 200);
            FactureTiersPayant fille = new FactureTiersPayant().setId(9L).setMontantRegle(0);
            fille.setFacturesDetails(new ArrayList<>(List.of(l1, l2)));

            InvoicePayment paiement = service.doReglement(paiementParent(), fille);

            assertThat(l1.getMontantRegle()).isEqualTo(1000);
            assertThat(l2.getMontantRegle()).isEqualTo(500);
            assertThat(fille.getMontantRegle()).isEqualTo(1300);
            assertThat(paiement.getPaidAmount()).isEqualTo(1300);
            assertThat(paiement.getReelAmount()).isEqualTo(1300);
            assertThat(paiement.getExpectedAmount()).isEqualTo(1300);
            assertThat(paiement.getInvoicePaymentItems()).hasSize(2);
        }

        @Test
        @DisplayName("herite du contexte de paiement du groupe et ne fixe pas de statut")
        void heriteDuGroupe() {
            InvoicePayment parent = paiementParent();
            FactureTiersPayant fille = new FactureTiersPayant().setId(9L).setMontantRegle(0);
            fille.setFacturesDetails(new ArrayList<>());

            InvoicePayment paiement = service.doReglement(parent, fille);

            assertThat(paiement.getBanque()).isSameAs(parent.getBanque());
            assertThat(paiement.getPaymentMode()).isSameAs(parent.getPaymentMode());
            assertThat(paiement.getTransactionDate()).isEqualTo(parent.getTransactionDate());
            assertThat(paiement.getFactureTiersPayant()).isSameAs(fille);
            // le paiement de la fille n est pas persiste ici : c est le groupe qui l enregistre
            verify(invoicePaymentRepository, org.mockito.Mockito.never()).save(paiement);
        }

        @Test
        @DisplayName("enregistre la facture fille et ses bons")
        void persiste() {
            FactureTiersPayant fille = new FactureTiersPayant().setId(9L).setMontantRegle(0);
            fille.setFacturesDetails(new ArrayList<>(List.of(ligne(1000, 0))));

            service.doReglement(paiementParent(), fille);

            verify(facturationRepository).save(fille);
            verify(thirdPartySaleLineRepository).saveAll(fille.getFacturesDetails());
        }
    }
}
