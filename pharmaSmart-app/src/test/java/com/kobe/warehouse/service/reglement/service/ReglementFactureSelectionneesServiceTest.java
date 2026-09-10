package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.reglement.dto.LigneSelectionnesDTO;
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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementFactureSelectionneesService")
class ReglementFactureSelectionneesServiceTest {

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

    private ReglementFactureSelectionneesService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ReglementFactureSelectionneesService(
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
            item.setPaidAmount(inv.getArgument(2));
            return item;
        });
        Specification<ThirdPartySaleLine> spec = (r, q, cb) -> null;
        when(thirdPartySaleLineRepository.selectionBonCriteria(any())).thenReturn(spec);
    }

    private static ThirdPartySaleLine ligne(int montant, int montantRegle) {
        return new ThirdPartySaleLine().setMontant(montant).setMontantRegle(montantRegle);
    }

    private FactureTiersPayant facture() {
        FactureTiersPayant facture = new FactureTiersPayant().setId(7L).setMontantRegle(0);
        facture.setFacturesDetails(new ArrayList<>());
        when(facturationRepository.getReferenceById(FACTURE_ID)).thenReturn(facture);
        return facture;
    }

    @SuppressWarnings("unchecked")
    private void stubBonsSelectionnes(ThirdPartySaleLine... lignes) {
        when(thirdPartySaleLineRepository.findAll(any(Specification.class))).thenReturn(List.of(lignes));
    }

    private static ReglementParam param(int amount, int totalAmount, int montantFacture, Long... dossierIds) {
        return new ReglementParam()
            .setId(FACTURE_ID)
            .setAmount(amount)
            .setTotalAmount(totalAmount)
            .setMontantFacture(montantFacture)
            .setDossierIds(new ArrayList<>(List.of(dossierIds)))
            .setModePaimentCode(ModePaimentCode.CASH);
    }

    @Nested
    @DisplayName("doReglement depuis un parametre")
    class DoReglementDepuisParam {

        @Test
        @DisplayName("refuse un reglement sans dossier selectionne")
        void aucunDossier() {
            ReglementParam param = param(1000, 1000, 1000);

            assertThatThrownBy(() -> service.doReglement(param))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Aucun dossiers à regler");

            verifyNoInteractions(facturationRepository);
        }

        @Test
        @DisplayName("solde integralement les bons couverts par le versement")
        void soldeIntegral() {
            FactureTiersPayant facture = facture();
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 100);
            stubBonsSelectionnes(l1, l2);

            ResponseReglementDTO response = service.doReglement(param(2000, 1400, 1400, 1L, 2L));

            assertThat(l1.getMontantRegle()).isEqualTo(1000);
            assertThat(l1.getStatut()).isEqualTo(ThirdPartySaleStatut.PAID);
            assertThat(l2.getMontantRegle()).isEqualTo(500);
            assertThat(facture.getMontantRegle()).isEqualTo(1400);
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.PAID);
            assertThat(response.total()).isTrue();
        }

        @Test
        @DisplayName("ecrete le dernier bon quand le versement ne suffit pas")
        void ecreteLeDernierBon() {
            FactureTiersPayant facture = facture();
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 0);
            stubBonsSelectionnes(l1, l2);

            service.doReglement(param(1200, 1500, 1500, 1L, 2L));

            assertThat(l1.getMontantRegle()).isEqualTo(1000);
            assertThat(l1.getStatut()).isEqualTo(ThirdPartySaleStatut.PAID);
            assertThat(l2.getMontantRegle()).isEqualTo(200);
            assertThat(l2.getStatut()).isEqualTo(ThirdPartySaleStatut.HALF_PAID);
            assertThat(facture.getMontantRegle()).isEqualTo(1200);
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.PARTIALLY_PAID);
        }

        @Test
        @DisplayName("s arrete des que le versement est epuise")
        void sArreteQuandEpuise() {
            facture();
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 0);
            stubBonsSelectionnes(l1, l2);

            service.doReglement(param(1000, 1500, 1500, 1L, 2L));

            assertThat(l1.getMontantRegle()).isEqualTo(1000);
            // le second bon n est pas touche : le versement etait deja consomme
            assertThat(l2.getMontantRegle()).isZero();
        }

        @Test
        @DisplayName("un versement nul ne touche aucun bon")
        void versementNul() {
            FactureTiersPayant facture = facture();
            ThirdPartySaleLine l1 = ligne(1000, 0);
            stubBonsSelectionnes(l1);

            service.doReglement(param(0, 1000, 1000, 1L));

            assertThat(l1.getMontantRegle()).isZero();
            assertThat(facture.getMontantRegle()).isZero();
            verify(thirdPartySaleLineRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("valorise le paiement au total attendu et au total effectivement verse")
        void valorisationDuPaiement() {
            facture();
            stubBonsSelectionnes(ligne(1000, 0));

            service.doReglement(param(1200, 1500, 1500, 1L));

            ArgumentCaptor<InvoicePayment> captor = ArgumentCaptor.forClass(InvoicePayment.class);
            verify(invoicePaymentRepository).save(captor.capture());
            assertThat(captor.getValue().getExpectedAmount()).isEqualTo(1500);
            assertThat(captor.getValue().getPaidAmount()).isEqualTo(1000);
            assertThat(captor.getValue().getReelAmount()).isEqualTo(1000);
        }

        @Test
        @DisplayName("n enregistre que les bons effectivement touches")
        void persisteLesBonsTouches() {
            facture();
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 0);
            stubBonsSelectionnes(l1, l2);

            service.doReglement(param(1000, 1500, 1500, 1L, 2L));

            verify(thirdPartySaleLineRepository).saveAll(List.of(l1));
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

        private static LigneSelectionnesDTO selection(int montantAttendu, int montantFacture) {
            return new LigneSelectionnesDTO().setMontantAttendu(montantAttendu).setMontantFacture(montantFacture);
        }

        private static FactureTiersPayant fille(ThirdPartySaleLine... lignes) {
            FactureTiersPayant fille = new FactureTiersPayant().setId(9L).setMontantRegle(0);
            fille.setFacturesDetails(new ArrayList<>(List.of(lignes)));
            return fille;
        }

        @Test
        @DisplayName("solde les bons de la facture fille dans la limite du montant alloue")
        void soldeDansLaLimite() {
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 0);
            FactureTiersPayant fille = fille(l1, l2);

            InvoicePayment paiement = service.doReglement(paiementParent(), fille, 1200, selection(1500, 1500));

            assertThat(l1.getMontantRegle()).isEqualTo(1000);
            assertThat(l2.getMontantRegle()).isEqualTo(200);
            assertThat(fille.getMontantRegle()).isEqualTo(1200);
            assertThat(fille.getStatut()).isEqualTo(InvoiceStatut.PARTIALLY_PAID);
            assertThat(paiement.getExpectedAmount()).isEqualTo(1500);
            assertThat(paiement.getPaidAmount()).isEqualTo(1200);
            assertThat(paiement.getReelAmount()).isEqualTo(1200);
        }

        @Test
        @DisplayName("cloture la fille quand le montant alloue couvre sa facture")
        void clotureLaFille() {
            FactureTiersPayant fille = fille(ligne(1000, 0));

            service.doReglement(paiementParent(), fille, 1000, selection(1000, 1000));

            assertThat(fille.getStatut()).isEqualTo(InvoiceStatut.PAID);
        }

        @Test
        @DisplayName("s arrete des que le montant alloue est epuise")
        void sArreteQuandEpuise() {
            ThirdPartySaleLine l1 = ligne(1000, 0);
            ThirdPartySaleLine l2 = ligne(500, 0);
            FactureTiersPayant fille = fille(l1, l2);

            service.doReglement(paiementParent(), fille, 1000, selection(1500, 1500));

            assertThat(l2.getMontantRegle()).isZero();
        }

        @Test
        @DisplayName("un montant alloue nul ne touche aucun bon")
        void montantAlloueNul() {
            ThirdPartySaleLine l1 = ligne(1000, 0);
            FactureTiersPayant fille = fille(l1);

            InvoicePayment paiement = service.doReglement(paiementParent(), fille, 0, selection(1000, 1000));

            assertThat(l1.getMontantRegle()).isZero();
            assertThat(paiement.getPaidAmount()).isZero();
        }

        @Test
        @DisplayName("herite du contexte de paiement du groupe sans etre persiste")
        void heriteDuGroupe() {
            InvoicePayment parent = paiementParent();
            FactureTiersPayant fille = fille();

            InvoicePayment paiement = service.doReglement(parent, fille, 1000, selection(1000, 1000));

            assertThat(paiement.getBanque()).isSameAs(parent.getBanque());
            assertThat(paiement.getPaymentMode()).isSameAs(parent.getPaymentMode());
            assertThat(paiement.getFactureTiersPayant()).isSameAs(fille);
            verify(invoicePaymentRepository, org.mockito.Mockito.never()).save(paiement);
        }

        @Test
        @DisplayName("enregistre la facture fille et les bons touches")
        void persiste() {
            ThirdPartySaleLine l1 = ligne(1000, 0);
            FactureTiersPayant fille = fille(l1);

            service.doReglement(paiementParent(), fille, 1000, selection(1000, 1000));

            verify(facturationRepository).save(fille);
            verify(thirdPartySaleLineRepository).saveAll(List.of(l1));
        }
    }
}
