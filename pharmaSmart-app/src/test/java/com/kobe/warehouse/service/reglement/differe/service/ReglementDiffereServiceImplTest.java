package com.kobe.warehouse.service.reglement.differe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.DifferePayment;
import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.DifferePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.receipt.service.DiffereReceiptService;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import com.kobe.warehouse.service.reglement.differe.dto.Differe;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummary;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.CustomerReglementDiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.NewDifferePaymentDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereResponse;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;
import com.kobe.warehouse.service.reglement.differe.dto.Solde;
import com.kobe.warehouse.service.reglement.dto.BanqueInfoDTO;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementDiffereServiceImpl")
class ReglementDiffereServiceImplTest {

    private static final PaymentId PAYMENT_ID = new PaymentId(55L, LocalDate.of(2026, 4, 18));

    @Mock
    private DifferePaymentRepository differePaymentRepository;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CashRegisterService cashRegisterService;

    @Mock
    private ReglementDiffereReportService reglementDiffereReportService;

    @Mock
    private BanqueRepository banqueRepository;

    @Mock
    private DiffereReceiptService differeReceiptService;

    @Mock
    private TransactionIdGeneratorService transactionIdGeneratorService;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private ReportExcelExportService reportExcelExportService;

    @Mock
    private Resource resource;

    private ReglementDiffereServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ReglementDiffereServiceImpl(
            differePaymentRepository,
            salesRepository,
            customerRepository,
            cashRegisterService,
            reglementDiffereReportService,
            banqueRepository,
            differeReceiptService,
            transactionIdGeneratorService,
            referenceService,
            reportExcelExportService
        );
        Specification<Sales> salesSpec = (r, q, cb) -> null;
        when(salesRepository.hasStatut(any())).thenReturn(salesSpec);
        when(salesRepository.filterByCustomerId(anyInt())).thenReturn(salesSpec);
        when(salesRepository.filterByPaymentStatus(any())).thenReturn(salesSpec);
        Specification<DifferePayment> paymentSpec = (r, q, cb) -> null;
        when(differePaymentRepository.filterByPeriode(any(), any())).thenReturn(paymentSpec);
        when(differePaymentRepository.filterByCustomerId(anyInt())).thenReturn(paymentSpec);
        when(cashRegisterService.getCashRegister()).thenReturn(cashRegister());
        when(transactionIdGeneratorService.nextId()).thenReturn(55L);
        when(referenceService.buildNumTransaction()).thenReturn("TR-0001");
        when(banqueRepository.save(any(Banque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(differePaymentRepository.save(any(DifferePayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesRepository.getDiffereItems(any(), any(Pageable.class))).thenReturn(Page.empty());
    }

    private static CashRegister cashRegister() {
        AppUser user = new AppUser();
        user.setFirstName("Awa");
        user.setLastName("KOUAME");
        CashRegister caisse = new CashRegister();
        caisse.setUser(user);
        return caisse;
    }

    private static Customer customer() {
        Customer customer = new Customer();
        customer.setId(3);
        customer.setFirstName("Kofi");
        customer.setLastName("YAO");
        return customer;
    }

    private static Sales vente(long id, int restToPay, Integer payrollAmount) {
        Sales vente = new com.kobe.warehouse.domain.CashSale();
        vente.setId(id);
        vente.setRestToPay(restToPay);
        vente.setPayrollAmount(payrollAmount);
        return vente;
    }

    private static NewDifferePaymentDTO commande(int amount, LocalDate paymentDate, BanqueInfoDTO banqueInfo) {
        return new NewDifferePaymentDTO(3, Set.of(1L), 5000, amount, ModePaimentCode.CASH, paymentDate, banqueInfo);
    }

    @Nested
    @DisplayName("lectures")
    class Lectures {

        @Test
        @DisplayName("getClientDiffere delegue au depot des ventes")
        void getClientDiffere() {
            Page<ClientDiffere> attendu = Page.empty();
            when(salesRepository.getClientDiffere(any(Pageable.class))).thenReturn(attendu);

            assertThat(service.getClientDiffere()).isSameAs(attendu);
        }

        @Test
        @DisplayName("getDiffereItems restreint aux ventes cloturees du client")
        void getDiffereItems() {
            service.getDiffereItems(3, null, null, null, Set.of(PaymentStatus.IMPAYE), Pageable.unpaged());

            verify(salesRepository).hasStatut(any());
            verify(salesRepository).filterByCustomerId(3);
            verify(salesRepository).filterByPaymentStatus(Set.of(PaymentStatus.IMPAYE));
        }

        @Test
        @DisplayName("un client absent ne filtre pas sur le client")
        void sansClient() {
            service.getDiffereItems(null, null, null, null, Set.of(PaymentStatus.IMPAYE), Pageable.unpaged());

            verify(salesRepository, never()).filterByCustomerId(anyInt());
        }

        @Test
        @DisplayName("getDiffere assemble chaque client avec le detail de ses ventes")
        @SuppressWarnings("unchecked")
        void getDiffere() {
            Differe differe = new Differe(3, "Kofi", "YAO", 10000L, 4000L, 6000L);
            when(salesRepository.getDiffere(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(differe)));
            DiffereItem item = new DiffereItem("Kofi", "YAO", "V-1", 1000, 0, 1000, LocalDateTime.now(), 1L, 3);
            when(salesRepository.getDiffereItems(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(item)));

            Page<DiffereDTO> page = service.getDiffere(3, Set.of(PaymentStatus.IMPAYE), Pageable.unpaged());

            assertThat(page).hasSize(1);
            DiffereDTO dto = page.getContent().getFirst();
            assertThat(dto.customerId()).isEqualTo(3);
            assertThat(dto.customerfullName()).isEqualTo("Kofi YAO");
            assertThat(dto.saleAmount()).isEqualTo(10000L);
            assertThat(dto.differeItems()).containsExactly(item);
        }

        @Test
        @DisplayName("getOne remonte le premier differe du client impaye")
        @SuppressWarnings("unchecked")
        void getOne() {
            Differe differe = new Differe(3, "Kofi", "YAO", 10000L, 4000L, 6000L);
            when(salesRepository.getDiffere(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(differe)));

            assertThat(service.getOne(3)).get().extracting(DiffereDTO::customerId).isEqualTo(3);

            // getOne force le filtre sur les impayes, en tete de page comme sur le detail
            verify(salesRepository, org.mockito.Mockito.atLeastOnce()).filterByPaymentStatus(Set.of(PaymentStatus.IMPAYE));
        }

        @Test
        @DisplayName("getDiffereSummary delegue au depot des ventes")
        void getDiffereSummary() {
            DiffereSummary attendu = new DiffereSummary(10000L, 4000L, 6000L);
            when(salesRepository.getDiffereSummary(any())).thenReturn(attendu);

            assertThat(service.getDiffereSummary(3, Set.of(PaymentStatus.IMPAYE))).isSameAs(attendu);
        }

        @Test
        @DisplayName("getDifferePaymentSummary ne remonte que le montant regle")
        void getDifferePaymentSummary() {
            when(differePaymentRepository.getDiffereSummary(any())).thenReturn(new DifferePaymentSummary(4000L));

            DifferePaymentSummaryDTO dto = service.getDifferePaymentSummary(3, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

            assertThat(dto.paidAmount()).isEqualTo(4000L);
            assertThat(dto.solde()).isNull();
        }

        @Test
        @DisplayName("une periode absente est bornee au jour")
        void periodeParDefaut() {
            when(differePaymentRepository.getDiffereSummary(any())).thenReturn(new DifferePaymentSummary(0L));

            service.getDifferePaymentSummary(null, null, null);

            verify(differePaymentRepository).filterByPeriode(LocalDate.now(), LocalDate.now());
            verify(differePaymentRepository, never()).filterByCustomerId(anyInt());
        }
    }

    @Nested
    @DisplayName("doReglement")
    class DoReglement {

        @Test
        @DisplayName("numerote le reglement et le rattache au client et a la caisse")
        void enteteDuReglement() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            when(salesRepository.findSalesByIdIn(Set.of(1L))).thenReturn(List.of());

            ReglementDiffereResponse response = service.doReglement(commande(5000, LocalDate.of(2026, 4, 18), null));

            ArgumentCaptor<DifferePayment> captor = ArgumentCaptor.forClass(DifferePayment.class);
            verify(differePaymentRepository).save(captor.capture());
            DifferePayment paiement = captor.getValue();
            assertThat(paiement.getId().getId()).isEqualTo(55L);
            assertThat(paiement.getTransactionNumber()).isEqualTo("TR-0001");
            assertThat(paiement.getDiffereCustomer().getId()).isEqualTo(3);
            assertThat(paiement.getMontantVerse()).isEqualTo(5000);
            assertThat(paiement.getExpectedAmount()).isEqualTo(5000);
            assertThat(paiement.getTransactionDate()).isEqualTo(LocalDate.of(2026, 4, 18));
            assertThat(paiement.getPaymentMode().getCode()).isEqualTo("CASH");
            assertThat(response.idReglement()).isNotNull();
        }

        @Test
        @DisplayName("une date de paiement absente vaut la date du jour")
        void dateParDefaut() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of());

            service.doReglement(commande(5000, null, null));

            ArgumentCaptor<DifferePayment> captor = ArgumentCaptor.forClass(DifferePayment.class);
            verify(differePaymentRepository).save(captor.capture());
            assertThat(captor.getValue().getTransactionDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("enregistre la banque quand un cheque est renseigne")
        void avecBanque() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of());
            BanqueInfoDTO banqueInfo = new BanqueInfoDTO().setCode("BQ1").setNom("SGBCI").setBeneficiaire("PHARMACIE");

            service.doReglement(commande(5000, null, banqueInfo));

            ArgumentCaptor<Banque> captor = ArgumentCaptor.forClass(Banque.class);
            verify(banqueRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("BQ1");
        }

        @Test
        @DisplayName("aucune banque enregistree sans information bancaire")
        void sansBanque() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of());

            service.doReglement(commande(5000, null, null));

            verifyNoInteractions(banqueRepository);
        }

        @Test
        @DisplayName("solde integralement les ventes couvertes par le versement")
        void soldeIntegral() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            Sales v1 = vente(1L, 3000, 0);
            Sales v2 = vente(2L, 2000, 0);
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of(v1, v2));

            service.doReglement(commande(5000, null, null));

            assertThat(v1.getPaymentStatus()).isEqualTo(PaymentStatus.PAYE);
            assertThat(v1.getRestToPay()).isZero();
            assertThat(v2.getPaymentStatus()).isEqualTo(PaymentStatus.PAYE);
            assertThat(v2.getRestToPay()).isZero();

            ArgumentCaptor<DifferePayment> captor = ArgumentCaptor.forClass(DifferePayment.class);
            verify(differePaymentRepository).save(captor.capture());
            assertThat(captor.getValue().getPaidAmount()).isEqualTo(5000);
            assertThat(captor.getValue().getReelAmount()).isEqualTo(5000);
            assertThat(captor.getValue().getDifferePaymentItems()).hasSize(2);
        }

        @Test
        @DisplayName("ecrete la derniere vente quand le versement ne suffit pas")
        void ecreteLaDerniereVente() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            Sales v1 = vente(1L, 3000, 0);
            Sales v2 = vente(2L, 2000, 0);
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of(v1, v2));

            service.doReglement(commande(4000, null, null));

            assertThat(v1.getPaymentStatus()).isEqualTo(PaymentStatus.PAYE);
            assertThat(v2.getPaymentStatus()).isEqualTo(PaymentStatus.IMPAYE);
            assertThat(v2.getRestToPay()).isEqualTo(1000);

            ArgumentCaptor<DifferePayment> captor = ArgumentCaptor.forClass(DifferePayment.class);
            verify(differePaymentRepository).save(captor.capture());
            assertThat(captor.getValue().getPaidAmount()).isEqualTo(4000);
        }

        @Test
        @DisplayName("s arrete des que le versement est epuise")
        void sArreteQuandEpuise() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            Sales v1 = vente(1L, 3000, 0);
            Sales v2 = vente(2L, 2000, 0);
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of(v1, v2));

            service.doReglement(commande(3000, null, null));

            assertThat(v1.getPaymentStatus()).isEqualTo(PaymentStatus.PAYE);
            // la seconde vente n est pas touchee : le versement etait deja consomme
            assertThat(v2.getPaymentStatus()).isNull();
            assertThat(v2.getRestToPay()).isEqualTo(2000);

            ArgumentCaptor<DifferePayment> captor = ArgumentCaptor.forClass(DifferePayment.class);
            verify(differePaymentRepository).save(captor.capture());
            assertThat(captor.getValue().getDifferePaymentItems()).hasSize(1);
        }

        @Test
        @DisplayName("un montant deja verse absent sur la vente compte pour zero")
        void payrollAmountAbsent() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            Sales vente = vente(1L, 3000, null);
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of(vente));

            service.doReglement(commande(3000, null, null));

            assertThat(vente.getPayrollAmount()).isEqualTo(3000);
        }

        @Test
        @DisplayName("cumule le versement sur le montant deja verse de la vente")
        void cumulePayrollAmount() {
            when(customerRepository.getReferenceById(3)).thenReturn(customer());
            Sales vente = vente(1L, 3000, 1000);
            when(salesRepository.findSalesByIdIn(any())).thenReturn(List.of(vente));

            service.doReglement(commande(3000, null, null));

            assertThat(vente.getPayrollAmount()).isEqualTo(4000);
        }
    }

    @Nested
    @DisplayName("recu de reglement")
    class RecuDeReglement {

        private DifferePayment paiement(BigDecimal solde) {
            DifferePayment paiement = new DifferePayment();
            paiement.setId(55L);
            paiement.setTransactionDate(LocalDate.of(2026, 4, 18));
            paiement.setTransactionNumber("TR-0001");
            paiement.setCashRegister(cashRegister());
            paiement.setDiffereCustomer(customer());
            paiement.setPaymentMode(new PaymentMode().code("CASH"));
            paiement.setExpectedAmount(5000);
            paiement.setMontantVerse(5000);
            paiement.setPaidAmount(5000);
            when(differePaymentRepository.getReferenceById(PAYMENT_ID)).thenReturn(paiement);
            when(salesRepository.getDiffereSoldeByCustomerId(3)).thenReturn(solde);
            return paiement;
        }

        @Test
        @DisplayName("compose le recu avec le caissier, le client et le solde restant")
        void composeLeRecu() {
            paiement(BigDecimal.valueOf(6000));

            ReglementDiffereReceiptDTO recu = service.getReglementDiffereReceipt(PAYMENT_ID);

            assertThat(recu.userFirstName()).isEqualTo("Awa");
            assertThat(recu.userLastName()).isEqualTo("KOUAME");
            assertThat(recu.firstName()).isEqualTo("Kofi");
            assertThat(recu.lastName()).isEqualTo("YAO");
            assertThat(recu.expectedAmount()).isEqualTo(5000);
            assertThat(recu.montantVerse()).isEqualTo(5000);
            assertThat(recu.paidAmount()).isEqualTo(5000);
            assertThat(recu.mode()).isEqualTo("CASH");
            assertThat(recu.solde()).isEqualTo(6000);
            assertThat(recu.reference()).isEqualTo("TR-0001");
        }

        @Test
        @DisplayName("un solde absent est ramene a zero")
        void soldeAbsent() {
            paiement(null);

            assertThat(service.getReglementDiffereReceipt(PAYMENT_ID).solde()).isZero();
        }

        @Test
        @DisplayName("printReceipt imprime le recu du reglement")
        void printReceipt() {
            paiement(BigDecimal.ZERO);

            service.printReceipt(PAYMENT_ID);

            verify(differeReceiptService).printReceipt(org.mockito.ArgumentMatchers.isNull(), any(ReglementDiffereReceiptDTO.class));
        }

        @Test
        @DisplayName("generateEscPosReceiptForTauri delegue au service de recu")
        void generateEscPos() throws IOException {
            paiement(BigDecimal.ZERO);
            when(differeReceiptService.generateEscPosReceiptForTauri(any(ReglementDiffereReceiptDTO.class))).thenReturn(new byte[] { 7 });

            assertThat(service.generateEscPosReceiptForTauri(PAYMENT_ID)).containsExactly(7);
        }
    }

    @Nested
    @DisplayName("exports")
    class Exports {

        @SuppressWarnings("unchecked")
        private void stubDiffere(Long saleAmount, Long paidAmount, Long rest) {
            Differe differe = new Differe(3, "Kofi", "YAO", saleAmount, paidAmount, rest);
            when(salesRepository.getDiffere(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(differe)));
        }

        @Test
        @DisplayName("l export Excel alimente une ligne par client")
        void exportExcel() throws Exception {
            stubDiffere(10000L, 4000L, 6000L);
            when(reportExcelExportService.createExcelReport(anyString(), any(), any(), any())).thenReturn(new byte[] { 9 });

            assertThat(service.exportDifferesToExcel(3, Set.of(PaymentStatus.IMPAYE))).containsExactly(9);

            ArgumentCaptor<String[]> headers = ArgumentCaptor.forClass(String[].class);
            verify(reportExcelExportService).createExcelReport(
                org.mockito.ArgumentMatchers.eq("Différés clients"),
                headers.capture(),
                any(),
                any()
            );
            assertThat(headers.getValue()).hasSize(4);
        }

        @Test
        @DisplayName("l export Excel ecrit les montants du client, zero si absents")
        @SuppressWarnings("unchecked")
        void ecritLesMontants() throws Exception {
            stubDiffere(null, null, null);
            when(reportExcelExportService.createExcelReport(anyString(), any(), any(), any())).thenAnswer(inv -> {
                List<DiffereDTO> data = inv.getArgument(2);
                java.util.function.BiConsumer<org.apache.poi.ss.usermodel.Row, DiffereDTO> mapper = inv.getArgument(3);
                try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                    org.apache.poi.ss.usermodel.Row row = wb.createSheet().createRow(0);
                    mapper.accept(row, data.getFirst());
                    assertThat(row.getCell(0).getStringCellValue()).isEqualTo("Kofi YAO");
                    assertThat(row.getCell(1).getNumericCellValue()).isZero();
                    assertThat(row.getCell(2).getNumericCellValue()).isZero();
                    assertThat(row.getCell(3).getNumericCellValue()).isZero();
                }
                return new byte[] { 9 };
            });

            service.exportDifferesToExcel(3, Set.of(PaymentStatus.IMPAYE));
        }

        @Test
        @DisplayName("l export Excel ecrit les montants renseignes du client")
        @SuppressWarnings("unchecked")
        void ecritLesMontantsRenseignes() throws Exception {
            stubDiffere(10000L, 4000L, 6000L);
            when(reportExcelExportService.createExcelReport(anyString(), any(), any(), any())).thenAnswer(inv -> {
                List<DiffereDTO> data = inv.getArgument(2);
                java.util.function.BiConsumer<org.apache.poi.ss.usermodel.Row, DiffereDTO> mapper = inv.getArgument(3);
                try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                    org.apache.poi.ss.usermodel.Row row = wb.createSheet().createRow(0);
                    mapper.accept(row, data.getFirst());
                    assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(10000d);
                    assertThat(row.getCell(2).getNumericCellValue()).isEqualTo(4000d);
                    assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(6000d);
                }
                return new byte[] { 9 };
            });

            service.exportDifferesToExcel(3, Set.of(PaymentStatus.IMPAYE));
        }

        @Test
        @DisplayName("une erreur de generation Excel remonte en erreur applicative")
        void exportExcelEnErreur() throws Exception {
            stubDiffere(10000L, 4000L, 6000L);
            when(reportExcelExportService.createExcelReport(anyString(), any(), any(), any())).thenThrow(new IOException("disque plein"));

            assertThatThrownBy(() -> service.exportDifferesToExcel(3, Set.of(PaymentStatus.IMPAYE)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erreur génération Excel différés");
        }

        @Test
        @DisplayName("printListToPdf transmet la synthese et la liste au rapport")
        @SuppressWarnings("unchecked")
        void printListToPdf() {
            stubDiffere(10000L, 4000L, 6000L);
            DiffereSummary summary = new DiffereSummary(10000L, 4000L, 6000L);
            when(salesRepository.getDiffereSummary(any())).thenReturn(summary);
            when(reglementDiffereReportService.printListToPdf(any(), any())).thenReturn(resource);

            assertThat(service.printListToPdf(3, Set.of(PaymentStatus.IMPAYE))).isSameAs(resource);

            verify(reglementDiffereReportService).printListToPdf(any(), org.mockito.ArgumentMatchers.eq(summary));
        }

        @Test
        @DisplayName("printReglementToPdf transmet la periode demandee au rapport")
        @SuppressWarnings("unchecked")
        void printReglementToPdf() {
            when(differePaymentRepository.getDiffereSummary(any())).thenReturn(new DifferePaymentSummary(4000L));
            when(differePaymentRepository.getDifferePayments(any(), any(Pageable.class))).thenReturn(Page.empty());
            when(reglementDiffereReportService.printReglementToPdf(any(), any(), any())).thenReturn(resource);
            LocalDate debut = LocalDate.of(2026, 4, 1);
            LocalDate fin = LocalDate.of(2026, 4, 30);

            assertThat(service.printReglementToPdf(3, debut, fin)).isSameAs(resource);

            verify(reglementDiffereReportService).printReglementToPdf(
                any(),
                any(),
                org.mockito.ArgumentMatchers.eq(new ReportPeriode(debut, fin))
            );
        }
    }

    @Nested
    @DisplayName("getReglementsDifferes")
    class GetReglementsDifferes {

        @Test
        @DisplayName("assemble chaque reglement avec son detail et le solde du client")
        @SuppressWarnings("unchecked")
        void assembleLesReglements() {
            CustomerReglementDiffereDTO reglement = new CustomerReglementDiffereDTO(3, "Kofi", "YAO", 4000L);
            when(differePaymentRepository.getDifferePayments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reglement)));
            ReglementDiffereDTO detail = new ReglementDiffereDTO(55L, "Kofi", "YAO", LocalDateTime.now(), 5000, 5000, 5000, "CASH", "Espèces");
            when(differePaymentRepository.getDifferePaymentsByCustomerId(any())).thenReturn(List.of(detail));
            when(salesRepository.getSolde(any())).thenReturn(new Solde(6000L));

            Page<ReglementDiffereWrapperDTO> page = service.getReglementsDifferes(
                3,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Pageable.unpaged()
            );

            assertThat(page).hasSize(1);
            ReglementDiffereWrapperDTO dto = page.getContent().getFirst();
            assertThat(dto.id()).isEqualTo(3);
            assertThat(dto.customerfullName()).isEqualTo("Kofi YAO");
            assertThat(dto.paidAmount()).isEqualTo(4000L);
            assertThat(dto.solde()).isEqualTo(6000L);
            assertThat(dto.items()).containsExactly(detail);
        }

        @Test
        @DisplayName("un reglement sans client identifie ne filtre pas le solde sur le client")
        @SuppressWarnings("unchecked")
        void reglementSansClient() {
            CustomerReglementDiffereDTO reglement = new CustomerReglementDiffereDTO(null, "Kofi", "YAO", 4000L);
            when(differePaymentRepository.getDifferePayments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reglement)));
            when(differePaymentRepository.getDifferePaymentsByCustomerId(any())).thenReturn(List.of());
            when(salesRepository.getSolde(any())).thenReturn(new Solde(0L));

            Page<ReglementDiffereWrapperDTO> page = service.getReglementsDifferes(null, null, null, Pageable.unpaged());

            assertThat(page).hasSize(1);
            assertThat(page.getContent().getFirst().id()).isNull();
            verify(salesRepository, never()).filterByCustomerId(anyInt());
        }

        @Test
        @DisplayName("une page vide ne sollicite ni detail ni solde")
        @SuppressWarnings("unchecked")
        void pageVide() {
            when(differePaymentRepository.getDifferePayments(any(), any(Pageable.class))).thenReturn(Page.empty());

            assertThat(service.getReglementsDifferes(3, null, null, Pageable.unpaged())).isEmpty();

            verify(differePaymentRepository, never()).getDifferePaymentsByCustomerId(any());
        }
    }
}
