package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentItemRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.receipt.service.InvoiceReceiptService;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementDataServiceImpl")
class ReglementDataServiceImplTest {

    private static final PaymentId PAYMENT_ID = new PaymentId(55L, LocalDate.of(2026, 4, 18));

    @Mock
    private FacturationRepository facturationRepository;

    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;

    @Mock
    private ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    @Mock
    private InvoicePaymentItemRepository invoicePaymentItemRepository;

    @Mock
    private ReglementReportService reglementReportService;

    @Mock
    private InvoiceReceiptService invoiceReceiptService;

    @Mock
    private Resource resource;

    private ReglementDataServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ReglementDataServiceImpl(
            facturationRepository,
            invoicePaymentRepository,
            thirdPartySaleLineRepository,
            invoicePaymentItemRepository,
            reglementReportService,
            invoiceReceiptService
        );
        Specification<InvoicePayment> spec = (r, q, cb) -> null;
        when(invoicePaymentRepository.periodeCriteria(any(), any())).thenReturn(spec);
        when(invoicePaymentRepository.invoicesTypePredicats(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(spec);
        when(invoicePaymentRepository.filterByOrganismeId(anyLong())).thenReturn(spec);
        when(invoicePaymentRepository.filterByTiersPayantId(anyLong())).thenReturn(spec);
        when(invoicePaymentRepository.specialisationQueryString(anyString())).thenReturn(spec);
        when(invoicePaymentRepository.findByFactureId(any())).thenReturn(spec);
        when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
    }

    private static TiersPayant tiersPayant(int id, String nom) {
        TiersPayant tp = new TiersPayant();
        tp.setId(id);
        tp.setName(nom);
        tp.setFullName(nom);
        return tp;
    }

    private static GroupeTiersPayant groupeTiersPayant(int id, String nom) {
        GroupeTiersPayant g = new GroupeTiersPayant();
        g.setId(id);
        g.setName(nom);
        return g;
    }

    private static FactureTiersPayant facture(TiersPayant tiersPayant, GroupeTiersPayant groupe) {
        FactureTiersPayant facture = new FactureTiersPayant().setId(7L).setMontantRegle(0);
        facture.setNumFacture("FA-0007");
        facture.setTiersPayant(tiersPayant);
        facture.setGroupeTiersPayant(groupe);
        return facture;
    }

    /** InvoicePaymentDTO exige un paiement complet : facture, organisme, mode et caissier. */
    private static InvoicePayment paiement(FactureTiersPayant facture, int expected, int paid) {
        AppUser user = new AppUser();
        user.setFirstName("Awa");
        user.setLastName("KOUAME");
        CashRegister caisse = new CashRegister();
        caisse.setUser(user);
        InvoicePayment paiement = new InvoicePayment();
        paiement.setId(55L);
        paiement.setTransactionDate(LocalDate.of(2026, 4, 18));
        paiement.setCreatedAt(LocalDateTime.of(2026, 4, 18, 10, 0));
        paiement.setFactureTiersPayant(facture);
        paiement.setPaymentMode(new PaymentMode().code("CASH"));
        paiement.setCashRegister(caisse);
        paiement.setExpectedAmount(expected);
        paiement.setPaidAmount(paid);
        paiement.setMontantVerse(expected);
        paiement.setInvoicePaymentItems(new ArrayList<>());
        return paiement;
    }

    private static ThirdPartySaleLine bon(int montant, int montantRegle) {
        AssuredCustomer assure = new AssuredCustomer();
        assure.setFirstName("Kofi");
        assure.setLastName("YAO");
        ClientTiersPayant client = new ClientTiersPayant();
        client.setAssuredCustomer(assure);
        client.setNum("MAT-1");
        return new ThirdPartySaleLine()
            .setMontant(montant)
            .setMontantRegle(montantRegle)
            .setNumBon("BON-1")
            .setCreated(LocalDateTime.of(2026, 4, 18, 10, 0))
            .setClientTiersPayant(client);
    }

    private static InvoicePaymentItem item(ThirdPartySaleLine bon, int amount, int paidAmount) {
        InvoicePaymentItem item = new InvoicePaymentItem();
        item.setThirdPartySaleLine(bon);
        item.setAmount(amount);
        item.setPaidAmount(paidAmount);
        return item;
    }

    @Nested
    @DisplayName("deleteReglement")
    class DeleteReglement {

        @Test
        @DisplayName("annule un reglement simple et remet les bons a leur solde d avant")
        void reglementSimple() {
            FactureTiersPayant facture = facture(tiersPayant(3, "CNAM"), null).setMontantRegle(1000);
            InvoicePayment paiement = paiement(facture, 1000, 1000);
            ThirdPartySaleLine bon = bon(1000, 1000);
            paiement.getInvoicePaymentItems().add(item(bon, 1000, 1000));
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID)).thenReturn(paiement);

            service.deleteReglement(PAYMENT_ID);

            assertThat(bon.getMontantRegle()).isZero();
            assertThat(bon.getStatut()).isEqualTo(ThirdPartySaleStatut.ACTIF);
            assertThat(facture.getMontantRegle()).isZero();
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.NOT_PAID);
            verify(thirdPartySaleLineRepository).save(bon);
            verify(invoicePaymentRepository).delete(paiement);
            verify(facturationRepository).save(facture);
        }

        @Test
        @DisplayName("un bon partiellement rembourse repasse en paiement partiel")
        void bonPartiellementRembourse() {
            FactureTiersPayant facture = facture(tiersPayant(3, "CNAM"), null).setMontantRegle(1000);
            InvoicePayment paiement = paiement(facture, 1000, 1000);
            ThirdPartySaleLine bon = bon(1000, 1000);
            paiement.getInvoicePaymentItems().add(item(bon, 400, 400));
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID)).thenReturn(paiement);

            service.deleteReglement(PAYMENT_ID);

            assertThat(bon.getMontantRegle()).isEqualTo(600);
            assertThat(bon.getStatut()).isEqualTo(ThirdPartySaleStatut.HALF_PAID);
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.PARTIALLY_PAID);
        }

        @Test
        @DisplayName("les soldes ne passent jamais sous zero")
        void soldesPlancherAZero() {
            FactureTiersPayant facture = facture(tiersPayant(3, "CNAM"), null).setMontantRegle(100);
            InvoicePayment paiement = paiement(facture, 1000, 1000);
            ThirdPartySaleLine bon = bon(1000, 100);
            paiement.getInvoicePaymentItems().add(item(bon, 1000, 1000));
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID)).thenReturn(paiement);

            service.deleteReglement(PAYMENT_ID);

            assertThat(bon.getMontantRegle()).isZero();
            assertThat(facture.getMontantRegle()).isZero();
        }

        @Test
        @DisplayName("une facture entierement reglee apres annulation reste payee")
        void factureResteePayee() {
            FactureTiersPayant facture = facture(tiersPayant(3, "CNAM"), null).setMontantRegle(1500);
            InvoicePayment paiement = paiement(facture, 1000, 1000);
            ThirdPartySaleLine bon = bon(1000, 1000);
            paiement.getInvoicePaymentItems().add(item(bon, 400, 400));
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID)).thenReturn(paiement);

            service.deleteReglement(PAYMENT_ID);

            assertThat(facture.getMontantRegle()).isEqualTo(1100);
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.PAID);
        }

        @Test
        @DisplayName("annule un reglement groupe et decremente la facture de groupe")
        void reglementGroupe() {
            FactureTiersPayant groupe = facture(null, groupeTiersPayant(4, "MUGEFCI")).setMontantRegle(1000);
            FactureTiersPayant fille = facture(tiersPayant(3, "CNAM"), null).setMontantRegle(1000);
            InvoicePayment paiementFille = paiement(fille, 1000, 1000);
            ThirdPartySaleLine bon = bon(1000, 1000);
            paiementFille.getInvoicePaymentItems().add(item(bon, 1000, 1000));

            InvoicePayment paiementGroupe = paiement(groupe, 1000, 1000);
            paiementGroupe.setInvoicePayments(new ArrayList<>(List.of(paiementFille)));
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID)).thenReturn(paiementGroupe);

            service.deleteReglement(PAYMENT_ID);

            assertThat(fille.getMontantRegle()).isZero();
            assertThat(groupe.getMontantRegle()).isZero();
            assertThat(groupe.getStatut()).isEqualTo(InvoiceStatut.NOT_PAID);
            verify(invoicePaymentRepository).delete(paiementFille);
            verify(invoicePaymentRepository).delete(paiementGroupe);
            verify(facturationRepository).save(groupe);
        }

        @Test
        @DisplayName("un paiement sans ligne ne touche a aucun bon")
        void paiementSansLigne() {
            FactureTiersPayant facture = facture(tiersPayant(3, "CNAM"), null).setMontantRegle(0);
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID)).thenReturn(paiement(facture, 0, 0));

            service.deleteReglement(PAYMENT_ID);

            verify(thirdPartySaleLineRepository, never()).save(any());
            assertThat(facture.getStatut()).isEqualTo(InvoiceStatut.NOT_PAID);
        }

        @Test
        @DisplayName("annule chaque reglement du lot")
        void lotDeReglements() {
            FactureTiersPayant facture = facture(tiersPayant(3, "CNAM"), null).setMontantRegle(0);
            when(invoicePaymentRepository.getReferenceById(any(PaymentId.class))).thenReturn(paiement(facture, 0, 0));

            service.deleteReglement(Set.of(PAYMENT_ID, new PaymentId(56L, LocalDate.of(2026, 4, 19))));

            verify(invoicePaymentRepository, org.mockito.Mockito.times(2)).delete(any(InvoicePayment.class));
        }
    }

    @Nested
    @DisplayName("fetchInvoicesPayments")
    class FetchInvoicesPayments {

        @Test
        @DisplayName("borne sur le jour quand aucune date n est transmise")
        void periodeParDefaut() {
            service.fetchInvoicesPayments(new InvoicePaymentParam(null, null, null, null, false));

            verify(invoicePaymentRepository).periodeCriteria(LocalDate.now(), LocalDate.now());
        }

        @Test
        @DisplayName("une date de fin absente vaut la date de debut")
        void dateFinParDefaut() {
            LocalDate debut = LocalDate.of(2026, 4, 1);

            service.fetchInvoicesPayments(new InvoicePaymentParam(null, null, debut, null, false));

            verify(invoicePaymentRepository).periodeCriteria(debut, debut);
        }

        @Test
        @DisplayName("respecte la periode transmise")
        void periodeTransmise() {
            LocalDate debut = LocalDate.of(2026, 4, 1);
            LocalDate fin = LocalDate.of(2026, 4, 30);

            service.fetchInvoicesPayments(new InvoicePaymentParam(null, null, debut, fin, false));

            verify(invoicePaymentRepository).periodeCriteria(debut, fin);
        }

        @Test
        @DisplayName("filtre sur le tiers payant en mode simple")
        void filtreTiersPayant() {
            service.fetchInvoicesPayments(new InvoicePaymentParam(null, 3, null, null, false));

            verify(invoicePaymentRepository).filterByTiersPayantId(3L);
            verify(invoicePaymentRepository, never()).filterByOrganismeId(anyLong());
        }

        @Test
        @DisplayName("filtre sur l organisme en mode groupe")
        void filtreOrganisme() {
            service.fetchInvoicesPayments(new InvoicePaymentParam(null, 3, null, null, true));

            verify(invoicePaymentRepository).filterByOrganismeId(3L);
            verify(invoicePaymentRepository, never()).filterByTiersPayantId(anyLong());
        }

        @Test
        @DisplayName("aucun filtre d organisme quand il n est pas transmis")
        void sansOrganisme() {
            service.fetchInvoicesPayments(new InvoicePaymentParam(null, null, null, null, false));

            verify(invoicePaymentRepository, never()).filterByTiersPayantId(anyLong());
            verify(invoicePaymentRepository, never()).filterByOrganismeId(anyLong());
        }

        @Test
        @DisplayName("filtre en recherche libre sur un prefixe")
        void rechercheLibre() {
            service.fetchInvoicesPayments(new InvoicePaymentParam("CNAM", null, null, null, false));

            verify(invoicePaymentRepository).specialisationQueryString("CNAM%");
        }

        @ParameterizedTest(name = "une recherche [{0}] n ajoute aucun filtre de texte")
        @CsvSource({ "''", "'   '" })
        void rechercheBlanche(String search) {
            service.fetchInvoicesPayments(new InvoicePaymentParam(search, null, null, null, false));

            verify(invoicePaymentRepository, never()).specialisationQueryString(anyString());
        }

        @Test
        @DisplayName("trie par tiers payant en mode simple et par organisme en mode groupe")
        @SuppressWarnings("unchecked")
        void triSelonLeMode() {
            service.fetchInvoicesPayments(new InvoicePaymentParam(null, null, null, null, false));
            service.fetchInvoicesPayments(new InvoicePaymentParam(null, null, null, null, true));

            ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
            verify(invoicePaymentRepository, org.mockito.Mockito.times(2)).findAll(any(Specification.class), captor.capture());
            assertThat(captor.getAllValues().get(0).toString()).contains("factureTiersPayant.tiersPayant.name");
            assertThat(captor.getAllValues().get(1).toString()).contains("factureTiersPayant.groupeTiersPayant.name");
        }

        @Test
        @DisplayName("mappe les paiements trouves")
        @SuppressWarnings("unchecked")
        void mappeLesPaiements() {
            when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000)));

            List<InvoicePaymentDTO> paiements = service.fetchInvoicesPayments(new InvoicePaymentParam(null, null, null, null, false));

            assertThat(paiements).hasSize(1);
            assertThat(paiements.getFirst().getCodeFacture()).isEqualTo("FA-0007");
        }
    }

    @Nested
    @DisplayName("lectures")
    class Lectures {

        @Test
        @DisplayName("findByInvoice mappe les reglements d une facture")
        @SuppressWarnings("unchecked")
        void findByInvoice() {
            when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000)));

            assertThat(service.findByInvoice(new FactureItemId(7L, LocalDate.of(2026, 4, 18)))).hasSize(1);
        }

        @Test
        @DisplayName("getInvoicePaymentsItems mappe les lignes d un reglement")
        void getInvoicePaymentsItems() {
            when(invoicePaymentItemRepository.findByInvoicePaymentIdAndInvoicePaymentTransactionDate(55L, LocalDate.of(2026, 4, 18)))
                .thenReturn(List.of(item(bon(1000, 0), 1000, 600)));

            List<InvoicePaymentItemDTO> items = service.getInvoicePaymentsItems(PAYMENT_ID);

            assertThat(items).hasSize(1);
            assertThat(items.getFirst().getNumBon()).isEqualTo("BON-1");
            assertThat(items.getFirst().getCustomer()).isEqualTo("Kofi YAO");
        }

        @Test
        @DisplayName("getInvoicePaymentsGroupItems mappe les reglements filles")
        void getInvoicePaymentsGroupItems() {
            when(invoicePaymentRepository.findInvoicePaymentByParentIdAndParentTransactionDate(55L, LocalDate.of(2026, 4, 18)))
                .thenReturn(List.of(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000)));

            assertThat(service.getInvoicePaymentsGroupItems(PAYMENT_ID)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("impression et export")
    class ImpressionEtExport {

        @Test
        @DisplayName("printReceipt imprime le recu du reglement")
        void printReceipt() {
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID))
                .thenReturn(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000));

            service.printReceipt(PAYMENT_ID);

            verify(invoiceReceiptService).printReceipt(org.mockito.ArgumentMatchers.isNull(), any(InvoicePaymentReceiptDTO.class));
        }

        @Test
        @DisplayName("generateEscPosReceiptForTauri delegue au service de recu")
        void generateEscPos() throws IOException {
            when(invoicePaymentRepository.getReferenceById(PAYMENT_ID))
                .thenReturn(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000));
            when(invoiceReceiptService.generateEscPosReceiptForTauri(any(InvoicePaymentReceiptDTO.class))).thenReturn(new byte[] { 7 });

            assertThat(service.generateEscPosReceiptForTauri(PAYMENT_ID)).containsExactly(7);
        }

        @Test
        @DisplayName("printToPdf refuse un export sans aucun reglement")
        void exportVide() {
            InvoicePaymentParam param = new InvoicePaymentParam(null, null, null, null, false);

            assertThatThrownBy(() -> service.printToPdf(param)).isInstanceOf(ReportFileExportException.class);
        }

        @Test
        @DisplayName("un seul organisme produit un releve simple")
        @SuppressWarnings("unchecked")
        void releveSimple() throws Exception {
            when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000)));
            when(reglementReportService.printToPdf(any(InvoicePaymentWrapper.class))).thenReturn(resource);

            assertThat(service.printToPdf(new InvoicePaymentParam(null, null, null, null, false))).isSameAs(resource);

            verify(reglementReportService, never()).printToPdf(org.mockito.ArgumentMatchers.<List<InvoicePaymentWrapper>>any());
        }

        @Test
        @DisplayName("plusieurs organismes produisent un releve groupe par organisme")
        @SuppressWarnings("unchecked")
        void relevePlusieursOrganismes() throws Exception {
            when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(
                    List.of(
                        paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000),
                        paiement(facture(tiersPayant(4, "MUGEFCI"), null), 500, 500)
                    )
                );
            when(reglementReportService.printToPdf(org.mockito.ArgumentMatchers.<List<InvoicePaymentWrapper>>any())).thenReturn(resource);

            assertThat(service.printToPdf(new InvoicePaymentParam(null, null, null, null, false))).isSameAs(resource);
        }

        @Test
        @DisplayName("le mode groupe regroupe par groupe de tiers payant")
        @SuppressWarnings("unchecked")
        void releveGroupe() throws Exception {
            when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(paiement(facture(null, groupeTiersPayant(4, "MUGEFCI")), 1000, 1000)));
            when(reglementReportService.printToPdf(any(InvoicePaymentWrapper.class))).thenReturn(resource);

            service.printToPdf(new InvoicePaymentParam(null, null, null, null, true));

            ArgumentCaptor<InvoicePaymentWrapper> captor = ArgumentCaptor.forClass(InvoicePaymentWrapper.class);
            verify(reglementReportService).printToPdf(captor.capture());
            assertThat(captor.getValue().getOrganisme().getName()).isEqualTo("MUGEFCI");
        }

        @Test
        @DisplayName("la periode d une journee est affichee comme une date simple")
        @SuppressWarnings("unchecked")
        void periodeDUneJournee() throws Exception {
            when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000)));
            when(reglementReportService.printToPdf(any(InvoicePaymentWrapper.class))).thenReturn(resource);
            LocalDate jour = LocalDate.of(2026, 4, 18);

            service.printToPdf(new InvoicePaymentParam(null, null, jour, jour, false));

            ArgumentCaptor<InvoicePaymentWrapper> captor = ArgumentCaptor.forClass(InvoicePaymentWrapper.class);
            verify(reglementReportService).printToPdf(captor.capture());
            assertThat(captor.getValue().getPeriode()).isEqualTo("18/04/2026");
        }

        @Test
        @DisplayName("une periode de plusieurs jours est affichee en intervalle")
        @SuppressWarnings("unchecked")
        void periodeIntervalle() throws Exception {
            when(invoicePaymentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(paiement(facture(tiersPayant(3, "CNAM"), null), 1000, 1000)));
            when(reglementReportService.printToPdf(any(InvoicePaymentWrapper.class))).thenReturn(resource);

            service.printToPdf(new InvoicePaymentParam(null, null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), false));

            ArgumentCaptor<InvoicePaymentWrapper> captor = ArgumentCaptor.forClass(InvoicePaymentWrapper.class);
            verify(reglementReportService).printToPdf(captor.capture());
            assertThat(captor.getValue().getPeriode()).isEqualTo(" DU 01/04/2026 AU 30/04/2026");
        }
    }
}
