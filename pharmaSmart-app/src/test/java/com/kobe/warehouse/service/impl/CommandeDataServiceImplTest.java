package com.kobe.warehouse.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.PharmaMlEnvoi;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PharmaMlStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.CustomizedCommandeService;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.PharmaMlEnvoiRepository;
import com.kobe.warehouse.service.csv.ExportationCsvService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeDashboardDTO;
import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.FilterCommaneEnCours;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.Sort;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.report.CommandeReportReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommandeDataServiceImpl")
class CommandeDataServiceImplTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 4, 18);
    private static final CommandeId COMMANDE_ID = new CommandeId(12, ORDER_DATE);

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private ExportationCsvService exportationCsvService;

    @Mock
    private CommandeReportReportService commandeReportService;

    @Mock
    private CustomizedCommandeService customizedCommandeService;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private PharmaMlEnvoiRepository pharmaMlEnvoiRepository;

    @Mock
    private EntityManager em;

    @Mock
    private Query nativeQuery;

    private CommandeDataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommandeDataServiceImpl(
            commandeRepository,
            exportationCsvService,
            commandeReportService,
            customizedCommandeService,
            orderLineRepository,
            new ObjectMapper(),
            pharmaMlEnvoiRepository
        );
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());
    }

    // ------------------------------------------------------------------ fixtures

    private static Produit produit(int id, String libelle) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setLibelle(libelle);
        produit.setCodeEanLaboratoire("EAN" + id);
        return produit;
    }

    private static OrderLine orderLine(int id, String libelle, String codeCip, int prixAchat, int orderCostAmount, boolean provisoire) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(id + 1000);
        fp.setCodeCip(codeCip);
        fp.setPrixAchat(prixAchat);
        fp.setPrixUni(prixAchat * 2);
        fp.setProduit(produit(id, libelle));
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setOrderDate(ORDER_DATE);
        line.setFournisseurProduit(fp);
        line.setQuantityRequested(10);
        line.setQuantityReceived(10);
        line.setOrderCostAmount(orderCostAmount);
        line.setOrderUnitPrice(prixAchat * 2);
        line.setFreeQty(0);
        line.setTaxAmount(0);
        line.setInitStock(5);
        line.setProvisionalCode(provisoire);
        line.setUpdated(Boolean.TRUE);
        line.setCreatedAt(LocalDateTime.of(2026, 4, 18, 8, 0).plusMinutes(id));
        line.setUpdatedAt(LocalDateTime.of(2026, 4, 18, 8, 0).plusMinutes(id));
        return line;
    }

    /**
     * Trois lignes : DOLIPRANE (prix conforme, cip definitif), ASPIRINE (prix different) et
     * PARACETAMOL (code provisoire).
     */
    private static List<OrderLine> troisLignes() {
        List<OrderLine> lines = new ArrayList<>();
        lines.add(orderLine(1, "DOLIPRANE 1000MG", "1111111", 400, 400, false));
        lines.add(orderLine(2, "ASPIRINE 500MG", "2222222", 400, 900, false));
        lines.add(orderLine(3, "PARACETAMOL 500MG", "3333333", 400, 400, true));
        return lines;
    }

    private static Commande commande(List<OrderLine> lines) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(3);
        fournisseur.setLibelle("LABOREX");
        Commande commande = new Commande();
        commande.setId(12);
        commande.setOrderDate(ORDER_DATE);
        commande.setOrderReference("CMD-001");
        commande.setReceiptReference("BL-001");
        commande.setGrossAmount(150_000);
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setFournisseur(fournisseur);
        commande.setOrderLines(new ArrayList<>(lines));
        return commande;
    }

    private static CommandeFilterDTO filtre(String search, FilterCommaneEnCours filter, Sort sort) {
        return new CommandeFilterDTO()
            .setCommandeId(12)
            .setOrderDate(ORDER_DATE)
            .setSearch(search)
            .setFilterCommaneEnCours(filter)
            .setOrderBy(sort);
    }

    private void referenceCommande(Commande commande) {
        when(commandeRepository.getReferenceById(any(CommandeId.class))).thenReturn(commande);
    }

    // ------------------------------------------------------------------ tests

    @Nested
    @DisplayName("findOneById et enrichissement de la couverture")
    class FindOneById {

        @Test
        @DisplayName("mappe la commande et renseigne la couverture de stock des lignes connues")
        void couvertureRenseignee() {
            referenceCommande(commande(troisLignes()));
            when(nativeQuery.getResultList()).thenReturn(
                List.<Object[]>of(new Object[] { 1, 12 }, new Object[] { 2, null })
            );

            CommandeDTO dto = service.findOneById(COMMANDE_ID);

            assertThat(dto.getId()).isEqualTo(12);
            assertThat(dto.getOrderLines()).hasSize(3);
            assertThat(dto.getTotalProduits()).isEqualTo(3);
            assertThat(dto.getFournisseur().getId()).isEqualTo(3);
            assertThat(dto.getOrderLines())
                .filteredOn(l -> l.getProduitId() == 1)
                .singleElement()
                .extracting(OrderLineDTO::getCouvertureStockJours)
                .isEqualTo(12);
            assertThat(dto.getOrderLines())
                .filteredOn(l -> l.getProduitId() == 2)
                .singleElement()
                .extracting(OrderLineDTO::getCouvertureStockJours)
                .isNull();
            verify(em).createNativeQuery(anyString());
        }

        @Test
        @DisplayName("n'interroge pas la vue de rotation quand la commande n'a aucune ligne")
        void aucuneLigne() {
            referenceCommande(commande(List.of()));

            CommandeDTO dto = service.findOneById(COMMANDE_ID);

            assertThat(dto.getOrderLines()).isEmpty();
            verify(em, org.mockito.Mockito.never()).createNativeQuery(anyString());
        }
    }

    @Nested
    @DisplayName("consultation et exports")
    class ConsultationEtExports {

        @Test
        @DisplayName("getCommandeById renvoie un CommandeEntryDTO trie par libelle")
        void getCommandeById() {
            referenceCommande(commande(troisLignes()));

            Optional<CommandeEntryDTO> dto = service.getCommandeById(COMMANDE_ID);

            assertThat(dto).isPresent();
            assertThat(dto.get().getOrderLines())
                .extracting(OrderLineDTO::getProduitLibelle)
                .containsExactly("ASPIRINE 500MG", "DOLIPRANE 1000MG", "PARACETAMOL 500MG");
            assertThat(dto.get().getFournisseur().getLibelle()).isEqualTo("LABOREX");
        }

        @Test
        @DisplayName("exportCommandeToCsv expose le fichier produit comme ressource")
        void exportCsv(@TempDir Path dir) throws IOException {
            Path fichier = dir.resolve("commande.csv");
            Files.writeString(fichier, "cip;libelle");
            Commande commande = commande(troisLignes());
            referenceCommande(commande);
            when(exportationCsvService.exportCommandeToCsv(commande)).thenReturn(fichier.toString());

            Resource resource = service.exportCommandeToCsv(COMMANDE_ID);

            assertThat(resource.exists()).isTrue();
            assertThat(resource.contentLength()).isEqualTo(11L);
        }

        @Test
        @DisplayName("exportCommandeToPdf delegue au service de rapport")
        void exportPdf() {
            referenceCommande(commande(troisLignes()));
            when(commandeReportService.export(any(CommandeDTO.class))).thenReturn(new byte[] { 1, 2, 3 });

            assertThat(service.exportCommandeToPdf(COMMANDE_ID)).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("getRuptureCsv delegue au service d'exportation")
        void ruptureCsv() {
            Resource attendu = new org.springframework.core.io.ByteArrayResource(new byte[] { 9 });
            when(exportationCsvService.getRutureFileByOrderReference("CMD-001")).thenReturn(attendu);

            assertThat(service.getRuptureCsv("CMD-001")).isSameAs(attendu);
        }
    }

    @Nested
    @DisplayName("filterCommandeLines avec recherche textuelle")
    class FiltreAvecRecherche {

        @BeforeEach
        void lignes() {
            referenceCommande(commande(troisLignes()));
        }

        @Test
        @DisplayName("recherche par libelle, sans filtre complementaire")
        void rechercheParLibelle() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre("doli", null, Sort.PRODUIT_LIBELLE));

            assertThat(lignes).extracting(OrderLineDTO::getProduitLibelle).containsExactly("DOLIPRANE 1000MG");
        }

        @Test
        @DisplayName("recherche par code CIP")
        void rechercheParCip() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre("2222222", FilterCommaneEnCours.ALL, Sort.PRODUIT_CIP));

            assertThat(lignes).extracting(OrderLineDTO::getProduitCip).containsExactly("2222222");
        }

        @Test
        @DisplayName("recherche croisee avec le filtre des prix differents du tarif fournisseur")
        void rechercheEtPrixDifferent() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre("aspi", FilterCommaneEnCours.NOT_EQUAL, Sort.UPDATE));

            assertThat(lignes).extracting(OrderLineDTO::getProduitLibelle).containsExactly("ASPIRINE 500MG");
        }

        @Test
        @DisplayName("recherche croisee avec le filtre des codes provisoires")
        void rechercheEtCodeProvisoire() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre("para", FilterCommaneEnCours.PROVISOL_CIP, Sort.PRODUIT_LIBELLE));

            assertThat(lignes).extracting(OrderLineDTO::getProduitLibelle).containsExactly("PARACETAMOL 500MG");
        }

        @Test
        @DisplayName("une recherche sans correspondance ne renvoie rien")
        void aucuneCorrespondance() {
            assertThat(service.filterCommandeLines(filtre("INTROUVABLE", null, Sort.PRODUIT_LIBELLE))).isEmpty();
        }
    }

    @Nested
    @DisplayName("filterCommandeLines sans recherche textuelle")
    class FiltreSansRecherche {

        @BeforeEach
        void lignes() {
            referenceCommande(commande(troisLignes()));
        }

        @ParameterizedTest(name = "search=[{0}] renvoie toutes les lignes")
        @NullSource
        @ValueSource(strings = { "" })
        @DisplayName("sans recherche ni filtre, toutes les lignes sont renvoyees")
        void toutesLesLignes(String search) {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre(search, FilterCommaneEnCours.ALL, Sort.PRODUIT_LIBELLE));

            assertThat(lignes)
                .extracting(OrderLineDTO::getProduitLibelle)
                .containsExactly("ASPIRINE 500MG", "DOLIPRANE 1000MG", "PARACETAMOL 500MG");
        }

        @Test
        @DisplayName("filtre nul : toutes les lignes, triees par code CIP")
        void filtreNul() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre(null, null, Sort.PRODUIT_CIP));

            assertThat(lignes).extracting(OrderLineDTO::getProduitCip).containsExactly("1111111", "2222222", "3333333");
        }

        @Test
        @DisplayName("filtre NOT_EQUAL : seules les lignes dont le prix differe du tarif fournisseur")
        void prixDifferent() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre(null, FilterCommaneEnCours.NOT_EQUAL, Sort.PRODUIT_LIBELLE));

            assertThat(lignes).extracting(OrderLineDTO::getProduitLibelle).containsExactly("ASPIRINE 500MG");
        }

        @Test
        @DisplayName("filtre PROVISOL_CIP : seules les lignes a code provisoire")
        void codeProvisoire() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre(null, FilterCommaneEnCours.PROVISOL_CIP, Sort.PRODUIT_LIBELLE));

            assertThat(lignes).extracting(OrderLineDTO::getProduitLibelle).containsExactly("PARACETAMOL 500MG");
        }

        @Test
        @DisplayName("tri UPDATE : la ligne modifiee le plus recemment vient en tete")
        void triParDateDeMiseAJour() {
            List<OrderLineDTO> lignes = service.filterCommandeLines(filtre(null, FilterCommaneEnCours.ALL, Sort.UPDATE));

            assertThat(lignes)
                .extracting(OrderLineDTO::getProduitLibelle)
                .containsExactly("PARACETAMOL 500MG", "ASPIRINE 500MG", "DOLIPRANE 1000MG");
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("fetchCommandes renseigne le nombre de lignes de chaque commande")
        void fetchCommandes() {
            Commande commande = commande(troisLignes());
            CommandeFilterDTO filtre = filtre(null, null, null);
            Pageable pageable = PageRequest.of(0, 1);
            when(customizedCommandeService.countfetchCommandes(filtre)).thenReturn(4L);
            when(customizedCommandeService.fetchCommandes(filtre, pageable)).thenReturn(List.of(commande));
            when(orderLineRepository.countByCommande(commande)).thenReturn(3);

            var page = service.fetchCommandes(filtre, pageable);

            assertThat(page.getTotalElements()).isEqualTo(4L);
            assertThat(page.getContent()).singleElement().extracting(CommandeLiteDTO::getItemSize).isEqualTo(3);
        }

        @Test
        @DisplayName("fetchCommandes renvoie une page vide quand le comptage est nul")
        void fetchCommandesVide() {
            CommandeFilterDTO filtre = filtre(null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            when(customizedCommandeService.countfetchCommandes(filtre)).thenReturn(0L);
            when(customizedCommandeService.fetchCommandes(filtre, pageable)).thenReturn(List.of());

            var page = service.fetchCommandes(filtre, pageable);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("filterCommandeLines pagine les lignes d'une commande")
        void filterCommandeLinesPagine() {
            Pageable pageable = PageRequest.of(0, 2);
            when(orderLineRepository.findByCommandeIdAndCommandeOrderDate(eq(12), eq(ORDER_DATE), eq(pageable)))
                .thenReturn(new PageImpl<>(troisLignes().subList(0, 2), pageable, 3L));

            var page = service.filterCommandeLines(COMMANDE_ID, pageable);

            assertThat(page.getTotalElements()).isEqualTo(3L);
            assertThat(page.getContent()).extracting(OrderLineDTO::getProduitLibelle).containsExactly("DOLIPRANE 1000MG", "ASPIRINE 500MG");
        }
    }

    @Nested
    @DisplayName("rapport tableau du pharmacien")
    class TableauPharmacien {

        private static final String JSON = """
            [{"montantNet":1000,"montantTtc":1200,"groupeGrossiste":"LABOREX"}]""";

        private static MvtParam param(String groupeBy) {
            MvtParam param = new MvtParam();
            param.setGroupeBy(groupeBy);
            param.setFromDate(LocalDate.of(2026, 1, 1));
            param.setToDate(LocalDate.of(2026, 1, 31));
            return param;
        }

        @Test
        @DisplayName("groupement mensuel : appelle la requete mensuelle")
        void mensuel() {
            when(
                commandeRepository.fetchTableauPharmacienReportMensuel(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 31),
                    OrderStatut.CLOSED.name()
                )
            ).thenReturn(JSON);

            List<AchatDTO> achats = service.fetchReportTableauPharmacienData(param("month"));

            assertThat(achats).singleElement().satisfies(a -> {
                assertThat(a.getMontantNet()).isEqualTo(1000L);
                assertThat(a.getGroupeGrossiste()).isEqualTo("LABOREX");
            });
            verify(commandeRepository, org.mockito.Mockito.never()).fetchTableauPharmacienReport(any(), any(), anyString());
        }

        @ParameterizedTest(name = "groupeBy=[{0}]")
        @NullSource
        @ValueSource(strings = { "day", "week" })
        @DisplayName("tout autre groupement passe par la requete detaillee")
        void detaille(String groupeBy) {
            when(commandeRepository.fetchTableauPharmacienReport(any(), any(), anyString())).thenReturn(JSON);

            assertThat(service.fetchReportTableauPharmacienData(param(groupeBy))).hasSize(1);
        }

        @ParameterizedTest(name = "resultat=[{0}]")
        @NullSource
        @ValueSource(strings = { "" })
        @DisplayName("un resultat vide donne une liste vide")
        void resultatVide(String json) {
            when(commandeRepository.fetchTableauPharmacienReport(any(), any(), anyString())).thenReturn(json);

            assertThat(service.fetchReportTableauPharmacienData(param("day"))).isEmpty();
        }

        @Test
        @DisplayName("un JSON illisible est journalise et donne une liste vide")
        void jsonIllisible() {
            when(commandeRepository.fetchTableauPharmacienReport(any(), any(), anyString())).thenReturn("{ceci n'est pas du json");

            assertThat(service.fetchReportTableauPharmacienData(param("day"))).isEmpty();
        }

        @Test
        @DisplayName("une erreur du depot est absorbee")
        void erreurDepot() {
            when(commandeRepository.fetchTableauPharmacienReport(any(), any(), anyString())).thenThrow(new IllegalStateException("boom"));

            assertThat(service.fetchReportTableauPharmacienData(param("day"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("tableau de bord")
    class Dashboard {

        private static Commande commandeDashboard(int id, LocalDate date, OrderStatut statut) {
            Commande commande = commande(List.of());
            commande.setId(id);
            commande.setOrderDate(date);
            commande.setOrderReference("CMD-" + id);
            commande.setOrderStatus(statut);
            commande.setReliquatDeCommandeId(id == 2 ? 99 : null);
            return commande;
        }

        private static PharmaMlEnvoi envoi(int id, Commande commande, LocalDateTime derniereTentative) {
            Fournisseur fournisseur = new Fournisseur();
            fournisseur.setId(3);
            fournisseur.setLibelle("LABOREX");
            PharmaMlEnvoi envoi = new PharmaMlEnvoi()
                .setCommande(commande)
                .setFournisseur(fournisseur)
                .setStatut(PharmaMlStatut.PENDING)
                .setDerniereTentative(derniereTentative);
            ReflectionTestUtils.setField(envoi, "id", id);
            return envoi;
        }

        @SuppressWarnings("unchecked")
        private void stubFindAll(List<Commande> requested, List<Commande> received) {
            when(commandeRepository.findAll(any(Specification.class))).thenReturn(requested, received);
        }

        @Test
        @DisplayName("agrege les commandes en attente, receptionnees et les envois PharmaML")
        void agregation() {
            Commande c1 = commandeDashboard(1, ORDER_DATE, OrderStatut.REQUESTED);
            Commande c2 = commandeDashboard(2, ORDER_DATE.plusDays(1), OrderStatut.REQUESTED);
            Commande c3 = commandeDashboard(3, ORDER_DATE, OrderStatut.RECEIVED);
            stubFindAll(List.of(c1, c2), List.of(c3));
            LocalDateTime tentative = LocalDateTime.of(2026, 4, 19, 10, 0);
            when(pharmaMlEnvoiRepository.findByStatutOrderByCreatedAtDesc(PharmaMlStatut.PENDING)).thenReturn(
                List.of(envoi(7, c1, tentative), envoi(8, c2, null))
            );

            CommandeDashboardDTO dashboard = service.getDashboard();

            assertThat(dashboard.totalRequested()).isEqualTo(2L);
            assertThat(dashboard.totalReceived()).isEqualTo(1L);
            assertThat(dashboard.totalPharmamlPending()).isEqualTo(2L);
            // tri par date de commande decroissante
            assertThat(dashboard.commandesRequested()).extracting(r -> r.id()).containsExactly(2, 1);
            assertThat(dashboard.commandesRequested().getFirst().reliquatDeCommandeId()).isEqualTo(99);
            assertThat(dashboard.commandesRequested().getFirst().orderDate()).isEqualTo(ORDER_DATE.plusDays(1).toString());
            assertThat(dashboard.commandesRequested().getFirst().orderStatus()).isEqualTo("REQUESTED");
            assertThat(dashboard.commandesRequested().getFirst().fournisseurLibelle()).isEqualTo("LABOREX");
            assertThat(dashboard.commandesRequested().getFirst().grossAmount()).isEqualTo(150_000);
            assertThat(dashboard.commandesReceived()).extracting(r -> r.orderReference()).containsExactly("CMD-3");
            assertThat(dashboard.envoisPending()).hasSize(2);
            assertThat(dashboard.envoisPending().getFirst().createdAt()).isEqualTo(tentative);
            assertThat(dashboard.envoisPending().getFirst().commandeId()).isEqualTo(1);
            assertThat(dashboard.envoisPending().getFirst().statut()).isEqualTo("PENDING");
            // sans derniere tentative, on retombe sur la date de creation
            assertThat(dashboard.envoisPending().get(1).createdAt()).isNotNull();
        }

        @Test
        @DisplayName("les specifications ciblent les commandes en attente puis receptionnees")
        @SuppressWarnings("unchecked")
        void specifications() {
            stubFindAll(List.of(), List.of());
            when(pharmaMlEnvoiRepository.findByStatutOrderByCreatedAtDesc(PharmaMlStatut.PENDING)).thenReturn(List.of());

            service.getDashboard();

            ArgumentCaptor<Specification<Commande>> captor = ArgumentCaptor.forClass(Specification.class);
            verify(commandeRepository, org.mockito.Mockito.times(2)).findAll(captor.capture());

            jakarta.persistence.criteria.Root<Commande> root = org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class);
            jakarta.persistence.criteria.CriteriaQuery<?> query = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaQuery.class);
            jakarta.persistence.criteria.CriteriaBuilder cb = org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class);
            jakarta.persistence.criteria.Predicate predicat = org.mockito.Mockito.mock(jakarta.persistence.criteria.Predicate.class);
            when(cb.equal(any(), any(Object.class))).thenReturn(predicat);
            when(cb.isNull(any())).thenReturn(predicat);
            when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predicat);

            for (Specification<Commande> specification : captor.getAllValues()) {
                assertThat(specification.toPredicate((jakarta.persistence.criteria.Root<Commande>) root, query, cb)).isSameAs(predicat);
            }
            // deux egalites (statut + type) et un isNull par specification
            verify(cb, org.mockito.Mockito.times(4)).equal(any(), any(Object.class));
            verify(cb, org.mockito.Mockito.times(2)).isNull(any());
        }

        @Test
        @DisplayName("un montant brut absent est ramene a zero")
        void montantBrutAbsent() {
            Commande c1 = commandeDashboard(1, ORDER_DATE, OrderStatut.REQUESTED);
            c1.setGrossAmount(null);
            stubFindAll(List.of(c1), List.of());
            when(pharmaMlEnvoiRepository.findByStatutOrderByCreatedAtDesc(PharmaMlStatut.PENDING)).thenReturn(List.of());

            CommandeDashboardDTO dashboard = service.getDashboard();

            assertThat(dashboard.commandesRequested().getFirst().grossAmount()).isZero();
            assertThat(dashboard.commandesRequested().getFirst().reliquatDeCommandeId()).isNull();
            assertThat(dashboard.envoisPending()).isEmpty();
        }

        @Test
        @DisplayName("les listes du tableau de bord sont plafonnees a 20 commandes et 50 envois")
        void plafonnement() {
            List<Commande> requested = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                requested.add(commandeDashboard(i + 1, ORDER_DATE.plusDays(i), OrderStatut.REQUESTED));
            }
            stubFindAll(requested, requested);
            List<PharmaMlEnvoi> envois = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                envois.add(envoi(i + 1, requested.get(0), null));
            }
            when(pharmaMlEnvoiRepository.findByStatutOrderByCreatedAtDesc(PharmaMlStatut.PENDING)).thenReturn(envois);

            CommandeDashboardDTO dashboard = service.getDashboard();

            assertThat(dashboard.totalRequested()).isEqualTo(25L);
            assertThat(dashboard.commandesRequested()).hasSize(20);
            assertThat(dashboard.commandesReceived()).hasSize(20);
            assertThat(dashboard.envoisPending()).hasSize(50);
        }
    }
}
