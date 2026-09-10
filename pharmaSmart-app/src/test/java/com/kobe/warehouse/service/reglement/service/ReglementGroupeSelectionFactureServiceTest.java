package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementGroupeSelectionFactureService")
class ReglementGroupeSelectionFactureServiceTest {

    private static final FactureItemId GROUPE_ID = new FactureItemId(7L, LocalDate.of(2026, 4, 18));
    private static final FactureItemId FILLE_1 = new FactureItemId(9L, LocalDate.of(2026, 4, 18));
    private static final FactureItemId FILLE_2 = new FactureItemId(10L, LocalDate.of(2026, 4, 18));

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
    private ReglementFactureSelectionneesService reglementFactureSelectionneesService;

    @Mock
    private TransactionIdGeneratorService transactionIdGeneratorService;

    @Mock
    private InvoicePaymentItemService invoicePaymentItemService;

    @Mock
    private ReferenceService referenceService;

    private ReglementGroupeSelectionFactureService service;

    @BeforeEach
    void setUp() {
        service = new ReglementGroupeSelectionFactureService(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            reglementFactureSelectionneesService,
            transactionIdGeneratorService,
            invoicePaymentItemService,
            referenceService
        );
        when(cashRegisterService.getCashRegister()).thenReturn(new CashRegister());
        when(userService.getUser()).thenReturn(new AppUser());
        when(transactionIdGeneratorService.nextId()).thenReturn(55L);
        when(referenceService.buildNumTransaction()).thenReturn("TR-0001");
        when(invoicePaymentRepository.save(any(InvoicePayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reglementFactureSelectionneesService.doReglement(any(), any(), anyInt(), any())).thenReturn(new InvoicePayment());
    }

    private FactureTiersPayant groupe() {
        FactureTiersPayant groupe = new FactureTiersPayant().setId(7L).setMontantRegle(0);
        when(facturationRepository.getReferenceById(GROUPE_ID)).thenReturn(groupe);
        return groupe;
    }

    private void stubFille(FactureItemId id, long entityId) {
        when(facturationRepository.getReferenceById(id)).thenReturn(new FactureTiersPayant().setId(entityId).setMontantRegle(0));
    }

    private static LigneSelectionnesDTO selection(FactureItemId id, int montantVerse) {
        return new LigneSelectionnesDTO().setId(id).setMontantVerse(montantVerse).setMontantAttendu(montantVerse).setMontantFacture(
            montantVerse
        );
    }

    private static ReglementParam param(int amount, int totalAmount, int montantFacture, LigneSelectionnesDTO... lignes) {
        return new ReglementParam()
            .setId(GROUPE_ID)
            .setAmount(amount)
            .setTotalAmount(totalAmount)
            .setMontantFacture(montantFacture)
            .setLigneSelectionnes(new ArrayList<>(List.of(lignes)))
            .setModePaimentCode(ModePaimentCode.CASH);
    }

    @Test
    @DisplayName("refuse un reglement sans ligne selectionnee")
    void aucuneLigne() {
        ReglementParam param = param(1000, 1000, 1000);

        assertThatThrownBy(() -> service.doReglement(param))
            .isInstanceOf(GenericError.class)
            .hasMessageContaining("Aucun dossiers à regler");

        verifyNoInteractions(facturationRepository);
    }

    @Test
    @DisplayName("marque le paiement comme groupe et ventile le versement sur chaque facture")
    void ventileSurChaqueFacture() {
        FactureTiersPayant groupe = groupe();
        stubFille(FILLE_1, 9L);
        stubFille(FILLE_2, 10L);

        ResponseReglementDTO response = service.doReglement(
            param(1500, 1500, 1500, selection(FILLE_1, 1000), selection(FILLE_2, 500))
        );

        assertThat(groupe.getMontantRegle()).isEqualTo(1500);
        assertThat(groupe.getStatut()).isEqualTo(InvoiceStatut.PAID);
        assertThat(response.total()).isTrue();

        ArgumentCaptor<InvoicePayment> captor = ArgumentCaptor.forClass(InvoicePayment.class);
        verify(invoicePaymentRepository).save(captor.capture());
        assertThat(captor.getValue().isGrouped()).isTrue();
        assertThat(captor.getValue().getExpectedAmount()).isEqualTo(1500);
        assertThat(captor.getValue().getPaidAmount()).isEqualTo(1500);
        assertThat(captor.getValue().getReelAmount()).isEqualTo(1500);
    }

    @Test
    @DisplayName("ecrete la derniere facture quand le versement ne suffit pas")
    void ecreteLaDerniereFacture() {
        FactureTiersPayant groupe = groupe();
        stubFille(FILLE_1, 9L);
        stubFille(FILLE_2, 10L);

        service.doReglement(param(1200, 1500, 1500, selection(FILLE_1, 1000), selection(FILLE_2, 500)));

        assertThat(groupe.getMontantRegle()).isEqualTo(1200);
        assertThat(groupe.getStatut()).isEqualTo(InvoiceStatut.PARTIALLY_PAID);
        // la seconde facture ne recoit que le reliquat
        verify(reglementFactureSelectionneesService).doReglement(any(), any(), org.mockito.ArgumentMatchers.eq(1000), any());
        verify(reglementFactureSelectionneesService).doReglement(any(), any(), org.mockito.ArgumentMatchers.eq(200), any());
    }

    @Test
    @DisplayName("s arrete des que le versement est epuise")
    void sArreteQuandEpuise() {
        groupe();
        stubFille(FILLE_1, 9L);
        stubFille(FILLE_2, 10L);

        service.doReglement(param(1000, 1500, 1500, selection(FILLE_1, 1000), selection(FILLE_2, 500)));

        // la seconde facture n est jamais chargee : le versement etait deja consomme
        verify(facturationRepository, org.mockito.Mockito.never()).getReferenceById(FILLE_2);
        verify(reglementFactureSelectionneesService, org.mockito.Mockito.times(1)).doReglement(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("un versement nul ne regle aucune facture")
    void versementNul() {
        FactureTiersPayant groupe = groupe();

        service.doReglement(param(0, 1000, 1000, selection(FILLE_1, 1000)));

        assertThat(groupe.getMontantRegle()).isZero();
        verifyNoInteractions(reglementFactureSelectionneesService);
        verify(invoicePaymentRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("rattache chaque paiement fille au paiement de groupe")
    void rattacheLesPaiementsFilles() {
        groupe();
        stubFille(FILLE_1, 9L);
        stubFille(FILLE_2, 10L);

        service.doReglement(param(1500, 1500, 1500, selection(FILLE_1, 1000), selection(FILLE_2, 500)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InvoicePayment>> captor = ArgumentCaptor.forClass(List.class);
        verify(invoicePaymentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allSatisfy(p -> assertThat(p.getParent()).isNotNull());
    }

    @Test
    @DisplayName("enregistre la facture de groupe")
    void persiste() {
        FactureTiersPayant groupe = groupe();
        stubFille(FILLE_1, 9L);

        service.doReglement(param(1000, 1000, 1000, selection(FILLE_1, 1000)));

        verify(facturationRepository).save(groupe);
    }
}
