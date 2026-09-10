package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.CashRegister;
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
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.reglement.dto.BanqueInfoDTO;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AbstractReglementService")
class AbstractReglementServiceTest {

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

    private CashRegister cashRegister;
    private AppUser currentUser;

    /** Sous-classe minimale : la classe testee est abstraite, seul doReglement est a fournir. */
    private static final class TestReglementService extends AbstractReglementService {

        TestReglementService(
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
        }

        @Override
        public ResponseReglementDTO doReglement(ReglementParam reglementParam) {
            return null;
        }
    }

    private TestReglementService service;

    @BeforeEach
    void setUp() {
        service = new TestReglementService(
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
        cashRegister = new CashRegister();
        currentUser = new AppUser();
        when(cashRegisterService.getCashRegister()).thenReturn(cashRegister);
        when(userService.getUser()).thenReturn(currentUser);
        when(transactionIdGeneratorService.nextId()).thenReturn(55L);
        when(referenceService.buildNumTransaction()).thenReturn("TR-0001");
        when(banqueRepository.save(any(Banque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePaymentRepository.save(any(InvoicePayment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ThirdPartySaleLine ligne(int montant, int montantRegle) {
        return new ThirdPartySaleLine().setMontant(montant).setMontantRegle(montantRegle);
    }

    private static FactureTiersPayant facture() {
        return new FactureTiersPayant().setId(7L);
    }

    private static ReglementParam param(int amount, LocalDate paymentDate, BanqueInfoDTO banqueInfo) {
        return new ReglementParam()
            .setAmount(amount)
            .setPaymentDate(paymentDate)
            .setBanqueInfo(banqueInfo)
            .setModePaimentCode(ModePaimentCode.CASH);
    }

    @Test
    @DisplayName("getCashRegister remonte la caisse ouverte")
    void getCashRegister() {
        assertThat(service.getCashRegister()).isSameAs(cashRegister);
    }

    @Test
    @DisplayName("buildInvoicePaymentItem delegue au service dedie")
    void buildInvoicePaymentItem() {
        ThirdPartySaleLine ligne = ligne(1000, 0);
        InvoicePayment paiement = new InvoicePayment();
        InvoicePaymentItem attendu = new InvoicePaymentItem();
        when(invoicePaymentItemService.buildInvoicePaymentItem(ligne, paiement, 600)).thenReturn(attendu);

        assertThat(service.buildInvoicePaymentItem(ligne, paiement, 600)).isSameAs(attendu);
    }

    @Nested
    @DisplayName("updateThirdPartyLine")
    class UpdateThirdPartyLine {

        @Test
        @DisplayName("cumule le regle et horodate la ligne")
        void cumuleEtHorodate() {
            ThirdPartySaleLine ligne = ligne(1000, 200);
            LocalDateTime avant = LocalDateTime.now();

            service.updateThirdPartyLine(ligne, 300);

            assertThat(ligne.getMontantRegle()).isEqualTo(500);
            assertThat(ligne.getEffectiveUpdateDate()).isAfterOrEqualTo(avant);
            assertThat(ligne.getUpdated()).isEqualTo(ligne.getEffectiveUpdateDate());
        }

        @ParameterizedTest(name = "bon de {0} regle a {1} apres versement de {2} : {3}")
        @CsvSource({ "1000, 0, 400, HALF_PAID", "1000, 600, 400, PAID", "1000, 0, 1200, PAID", "1000, 999, 1, PAID" })
        void statutSelonLeSolde(int montant, int dejaRegle, int verse, ThirdPartySaleStatut attendu) {
            ThirdPartySaleLine ligne = ligne(montant, dejaRegle);

            service.updateThirdPartyLine(ligne, verse);

            assertThat(ligne.getStatut()).isEqualTo(attendu);
        }
    }

    @Nested
    @DisplayName("buildInvoicePayment depuis un parametre de reglement")
    class BuildInvoicePaymentFromParam {

        @Test
        @DisplayName("numerote la transaction et la rattache a la facture et a la caisse")
        void enteteDuPaiement() {
            FactureTiersPayant facture = facture();

            InvoicePayment paiement = service.buildInvoicePayment(facture, param(5000, LocalDate.of(2026, 4, 18), null));

            assertThat(paiement.getId().getId()).isEqualTo(55L);
            assertThat(paiement.getTransactionNumber()).isEqualTo("TR-0001");
            assertThat(paiement.getFactureTiersPayant()).isSameAs(facture);
            assertThat(paiement.getCashRegister()).isSameAs(cashRegister);
            assertThat(paiement.getMontantVerse()).isEqualTo(5000);
            assertThat(paiement.getTransactionDate()).isEqualTo(LocalDate.of(2026, 4, 18));
        }

        @Test
        @DisplayName("une date de paiement absente vaut la date du jour")
        void dateParDefaut() {
            InvoicePayment paiement = service.buildInvoicePayment(facture(), param(5000, null, null));

            assertThat(paiement.getTransactionDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("reprend le mode de paiement demande")
        void modeDePaiement() {
            ReglementParam param = param(5000, null, null).setModePaimentCode(ModePaimentCode.WAVE);

            InvoicePayment paiement = service.buildInvoicePayment(facture(), param);

            assertThat(paiement.getPaymentMode().getCode()).isEqualTo("WAVE");
        }

        @Test
        @DisplayName("enregistre la banque quand un cheque est renseigne")
        void avecBanque() {
            BanqueInfoDTO banqueInfo = new BanqueInfoDTO().setCode("BQ1").setNom("SGBCI").setBeneficiaire("PHARMACIE");

            InvoicePayment paiement = service.buildInvoicePayment(facture(), param(5000, null, banqueInfo));

            ArgumentCaptor<Banque> captor = ArgumentCaptor.forClass(Banque.class);
            verify(banqueRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("BQ1");
            assertThat(captor.getValue().getNom()).isEqualTo("SGBCI");
            assertThat(captor.getValue().getBeneficiaire()).isEqualTo("PHARMACIE");
            assertThat(paiement.getBanque()).isNotNull();
        }

        @Test
        @DisplayName("aucune banque enregistree sans information bancaire")
        void sansBanque() {
            InvoicePayment paiement = service.buildInvoicePayment(facture(), param(5000, null, null));

            assertThat(paiement.getBanque()).isNull();
            verifyNoInteractions(banqueRepository);
        }
    }

    @Nested
    @DisplayName("buildInvoicePayment depuis un paiement de groupe")
    class BuildInvoicePaymentFromParent {

        @Test
        @DisplayName("herite banque, caisse, mode et date du paiement parent")
        void heriteDuParent() {
            Banque banque = new Banque().setCode("BQ1");
            PaymentMode mode = new PaymentMode().code("CASH");
            InvoicePayment parent = new InvoicePayment();
            parent.setBanque(banque);
            parent.setCashRegister(cashRegister);
            parent.setPaymentMode(mode);
            parent.setTransactionDate(LocalDate.of(2026, 4, 18));
            FactureTiersPayant facture = facture();

            InvoicePayment paiement = service.buildInvoicePayment(facture, parent);

            assertThat(paiement.getId().getId()).isEqualTo(55L);
            assertThat(paiement.getTransactionNumber()).isEqualTo("TR-0001");
            assertThat(paiement.getFactureTiersPayant()).isSameAs(facture);
            assertThat(paiement.getBanque()).isSameAs(banque);
            assertThat(paiement.getCashRegister()).isSameAs(cashRegister);
            assertThat(paiement.getPaymentMode()).isSameAs(mode);
            assertThat(paiement.getTransactionDate()).isEqualTo(LocalDate.of(2026, 4, 18));
        }
    }

    @Nested
    @DisplayName("updateFactureTiersPayant")
    class UpdateFactureTiersPayant {

        @Test
        @DisplayName("cumule le regle, trace l utilisateur et horodate")
        void cumuleEtTrace() {
            FactureTiersPayant facture = facture().setMontantRegle(1000);
            LocalDateTime avant = LocalDateTime.now();

            service.updateFactureTiersPayant(facture, 500);

            assertThat(facture.getMontantRegle()).isEqualTo(1500);
            assertThat(facture.getUser()).isSameAs(currentUser);
            assertThat(facture.getUpdated()).isAfterOrEqualTo(avant);
        }
    }

    @Nested
    @DisplayName("updateStatut")
    class UpdateStatut {

        @ParameterizedTest(name = "facture de {1} reglee a {0} : {2}")
        @CsvSource({ "0, 1000, PARTIALLY_PAID", "400, 1000, PARTIALLY_PAID", "1000, 1000, PAID", "1200, 1000, PAID" })
        void statutSelonLeSolde(int montantRegle, int montantFacture, InvoiceStatut attendu) {
            FactureTiersPayant facture = facture().setMontantRegle(montantRegle);

            service.updateStatut(facture, montantFacture);

            assertThat(facture.getStatut()).isEqualTo(attendu);
        }
    }

    @Nested
    @DisplayName("persistance")
    class Persistance {

        @Test
        @DisplayName("saveInvoicePayment marque la transaction comme reglement tiers payant")
        void saveInvoicePayment() {
            InvoicePayment paiement = new InvoicePayment();

            InvoicePayment enregistre = service.saveInvoicePayment(paiement);

            assertThat(enregistre).isSameAs(paiement);
            assertThat(paiement.getTypeFinancialTransaction()).isEqualTo(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT);
            verify(invoicePaymentRepository).save(paiement);
        }

        @Test
        @DisplayName("saveFactureTiersPayant delegue au depot de facturation")
        void saveFactureTiersPayant() {
            FactureTiersPayant facture = facture();

            service.saveFactureTiersPayant(facture);

            verify(facturationRepository).save(facture);
        }

        @Test
        @DisplayName("saveThirdPartyLines delegue au depot des bons")
        void saveThirdPartyLines() {
            List<ThirdPartySaleLine> lignes = List.of(ligne(1000, 0));

            service.saveThirdPartyLines(lignes);

            verify(thirdPartySaleLineRepository).saveAll(lignes);
        }

        @Test
        @DisplayName("saveInvoicePayments enregistre le lot de paiements")
        void saveInvoicePayments() {
            List<InvoicePayment> paiements = List.of(new InvoicePayment());

            service.saveInvoicePayments(paiements);

            verify(invoicePaymentRepository).saveAll(paiements);
        }
    }
}
