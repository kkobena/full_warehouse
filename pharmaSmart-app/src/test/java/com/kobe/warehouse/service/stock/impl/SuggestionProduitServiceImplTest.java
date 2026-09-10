package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.BudgetCommandeDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO.LigneSelection;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.dto.enumeration.Mois;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.report.pdf.SuggestionPdfReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.dto.QauntiteProduitVendus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SuggestionProduitServiceImpl")
class SuggestionProduitServiceImplTest {

    @Mock
    private SuggestionRepository suggestionRepository;

    @Mock
    private SuggestionLineRepository suggestionLineRepository;

    @Mock
    private FournisseurProduitRepository fournisseurProduitRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private EtatProduitService etatProduitService;

    @Mock
    private CommandService commandService;

    @Mock
    private CsvExportService csvExportService;

    @Mock
    private EntityManager em;

    @Mock
    private SuggestionPdfReportService suggestionPdfReportService;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private Query commandeQuery;

    @Mock
    private Query suggestionQuery;

    private SuggestionProduitServiceImpl service;

    private Storage mainStorage;
    private Magasin magasin;
    private AppUser currentUser;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new SuggestionProduitServiceImpl(
            suggestionRepository,
            suggestionLineRepository,
            fournisseurProduitRepository,
            storageService,
            referenceService,
            appConfigurationService,
            etatProduitService,
            commandService,
            csvExportService,
            em,
            suggestionPdfReportService,
            produitRepository,
            orderLineRepository
        );

        magasin = new Magasin();
        magasin.setId(1);
        mainStorage = new Storage();
        mainStorage.setId(2);
        mainStorage.setMagasin(magasin);
        currentUser = new AppUser();

        when(storageService.getDefaultMagasinMainStorage()).thenReturn(mainStorage);
        when(storageService.getConnectedUserMagasin()).thenReturn(magasin);
        when(storageService.getUser()).thenReturn(currentUser);
        when(appConfigurationService.findSuggestionRetention()).thenReturn(30);
        when(appConfigurationService.getNombreJourRetentionCommande()).thenReturn(15);
        when(appConfigurationService.getNthMoisConsommation()).thenReturn(6);
        when(referenceService.buildSuggestionReference()).thenReturn("0001");

        Specification<Suggestion> spec = (r, q, cb) -> null;
        when(suggestionRepository.filterByDate(anyInt())).thenReturn(spec);
        when(suggestionRepository.filterByType(any())).thenReturn(spec);
        when(suggestionRepository.filterByStatut(any())).thenReturn(spec);
        when(suggestionRepository.filterByFournisseurIds(any())).thenReturn(spec);
        when(suggestionRepository.getAllSuggestion(any(), any(), any())).thenReturn(Page.empty());
    }

    private static Fournisseur fournisseur(int id, String libelle) {
        Fournisseur f = new Fournisseur();
        f.setId(id);
        f.setLibelle(libelle);
        return f;
    }

    private static FournisseurProduit fournisseurProduit(int id, Produit produit, Fournisseur fournisseur) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(id);
        fp.setProduit(produit);
        fp.setFournisseur(fournisseur);
        fp.setCodeCip("CIP" + id);
        return fp;
    }

    private static Produit produit(int id, Status status, int seuilMini) {
        Produit p = new Produit();
        p.setId(id);
        p.setLibelle("PRODUIT " + id);
        p.setStatus(status);
        p.setQtySeuilMini(seuilMini);
        return p;
    }

    private Suggestion suggestion(int id, Fournisseur fournisseur) {
        Suggestion s = new Suggestion();
        s.setId(id);
        s.setSuggessionReference("SUG-" + id);
        s.setFournisseur(fournisseur);
        s.setUpdatedAt(LocalDateTime.now());
        s.setSuggestionLines(new HashSet<>());
        return s;
    }

    /**
     * {@code SuggestionLineRepository} etend aussi {@code JpaSpecificationExecutor}, qui expose
     * un {@code delete(DeleteSpecification)} en plus du {@code delete(T)} de {@code CrudRepository} :
     * javac ne sait plus resoudre {@code .delete(entity)} sur le mock. On elargit le type statique.
     */
    private org.springframework.data.repository.CrudRepository<SuggestionLine, Integer> asCrud(SuggestionLineRepository repo) {
        return repo;
    }

    private static SuggestionLine ligne(int id, FournisseurProduit fp, int quantity) {
        SuggestionLine line = new SuggestionLine();
        line.setId(id);
        line.setFournisseurProduit(fp);
        line.setQuantity(quantity);
        line.setCreatedAt(LocalDateTime.now());
        return line;
    }

    @Nested
    @DisplayName("getAllSuggestion")
    class GetAllSuggestion {

        @Test
        @DisplayName("borne toujours sur la retention configuree")
        void borneSurLaRetention() {
            service.getAllSuggestion(null, null, null, null, PageRequest.of(0, 20));

            verify(suggestionRepository).filterByDate(30);
            verify(suggestionRepository, never()).filterByType(any());
            verify(suggestionRepository, never()).filterByStatut(any());
            verify(suggestionRepository, never()).filterByFournisseurIds(any());
        }

        @Test
        @DisplayName("filtre sur le type")
        void filtreSurLeType() {
            service.getAllSuggestion(null, null, TypeSuggession.AUTO, null, PageRequest.of(0, 20));

            verify(suggestionRepository).filterByType(TypeSuggession.AUTO);
        }

        @Test
        @DisplayName("filtre sur les statuts")
        void filtreSurLesStatuts() {
            service.getAllSuggestion(null, null, null, Set.of(StatutSuggession.GENEREE), PageRequest.of(0, 20));

            verify(suggestionRepository).filterByStatut(any());
        }

        @ParameterizedTest(name = "des statuts vides ou nuls ne filtrent pas")
        @NullAndEmptySource
        void statutsVides(Set<StatutSuggession> statuts) {
            service.getAllSuggestion(null, null, null, statuts, PageRequest.of(0, 20));

            verify(suggestionRepository, never()).filterByStatut(any());
        }

        @Test
        @DisplayName("filtre sur les fournisseurs")
        void filtreSurLesFournisseurs() {
            service.getAllSuggestion(null, Set.of(3), null, null, PageRequest.of(0, 20));

            verify(suggestionRepository).filterByFournisseurIds(Set.of(3));
        }

        @ParameterizedTest(name = "des fournisseurs vides ou nuls ne filtrent pas")
        @NullAndEmptySource
        void fournisseursVides(Set<Integer> ids) {
            service.getAllSuggestion(null, ids, null, null, PageRequest.of(0, 20));

            verify(suggestionRepository, never()).filterByFournisseurIds(any());
        }

        @Test
        @DisplayName("transmet le terme de recherche au depot")
        void transmetLaRecherche() {
            Page<SuggestionProjection> expected = Page.empty();
            when(suggestionRepository.getAllSuggestion(any(), any(), anyString())).thenReturn(expected);

            assertThat(service.getAllSuggestion("dolipra", null, null, null, PageRequest.of(0, 20))).isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("lectures simples")
    class LecturesSimples {

        @Test
        @DisplayName("countByStatut delegue au depot")
        void countByStatut() {
            when(suggestionRepository.countByStatut(StatutSuggession.GENEREE)).thenReturn(7L);

            assertThat(service.countByStatut(StatutSuggession.GENEREE)).isEqualTo(7L);
        }

        @Test
        @DisplayName("getSuggestionById renvoie vide quand la suggestion est introuvable")
        void suggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());

            assertThat(service.getSuggestionById(1)).isEmpty();
        }

        @Test
        @DisplayName("getSuggestionById attache les agregats de lignes")
        void suggestionAvecAgregats() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            Optional<SuggestionDTO> dto = service.getSuggestionById(1);

            assertThat(dto).isPresent();
            verify(suggestionLineRepository).getSuggestionData(1);
        }

        @Test
        @DisplayName("getSuggestionsParFournisseur delegue au depot")
        void suggestionsParFournisseur() {
            List<FournisseurSuggestionSummaryDTO> expected = List.of();
            when(suggestionRepository.getSuggestionsParFournisseur(any(), any(), any())).thenReturn(expected);

            assertThat(service.getSuggestionsParFournisseur(Set.of(StatutSuggession.GENEREE), Set.of(3), "x")).isSameAs(expected);
        }

        @Test
        @DisplayName("getSuggestionLinesByIdWithConsommation resout magasin, retention et profondeur de conso")
        void lignesAvecConsommation() {
            Page<SuggestionLineDTO> expected = Page.empty();
            when(suggestionLineRepository.fetchSuggestionLinesWithConsommation(any(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(expected);

            assertThat(service.getSuggestionLinesByIdWithConsommation(1, "s", "URGENT", PageRequest.of(0, 20))).isSameAs(expected);

            verify(suggestionLineRepository).fetchSuggestionLinesWithConsommation(
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("s"),
                org.mockito.ArgumentMatchers.eq("URGENT"),
                org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(LocalDate.now().minusDays(15)),
                org.mockito.ArgumentMatchers.eq(6),
                any(Pageable.class)
            );
        }

        @Test
        @DisplayName("getAllSuggestionLines renvoie le contenu non pagine")
        void toutesLesLignes() {
            SuggestionLineDTO ligne = ligneDTO(1, 4);
            when(suggestionLineRepository.fetchSuggestionLinesWithConsommation(any(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(ligne)));

            assertThat(service.getAllSuggestionLines(1, null, null)).containsExactly(ligne);
        }
    }

    private static SuggestionLineDTO ligneDTO(int id, int quantity) {
        return ligneDTO(id, quantity, null);
    }

    private static SuggestionLineDTO ligneDTO(int id, int quantity, Map<Mois, Integer> conso) {
        return new SuggestionLineDTO(
            id,
            quantity,
            LocalDateTime.now(),
            LocalDateTime.now(),
            "PRODUIT " + id,
            "CIP" + id,
            "EAN" + id,
            100 + id,
            200 + id,
            5,
            null,
            400,
            800,
            conso,
            "NORMAL",
            10,
            "CLASSIQUE",
            false,
            1,
            0
        );
    }

    @Nested
    @DisplayName("fusionnerSuggestion")
    class FusionnerSuggestion {

        @Test
        @DisplayName("ne fait rien quand aucune suggestion n est trouvee")
        void aucuneSuggestion() {
            when(suggestionRepository.findAllById(Set.of(1, 2))).thenReturn(List.of());

            service.fusionnerSuggestion(Set.of(1, 2));

            verify(suggestionRepository, never()).save(any());
            verify(suggestionRepository, never()).delete(any(Suggestion.class));
        }

        @Test
        @DisplayName("refuse de fusionner des suggestions de fournisseurs differents")
        void fournisseursDifferents() {
            Suggestion s1 = suggestion(1, fournisseur(3, "LABOREX"));
            s1.setUpdatedAt(LocalDateTime.now());
            Suggestion s2 = suggestion(2, fournisseur(4, "COPHARMED"));
            s2.setUpdatedAt(LocalDateTime.now().minusDays(1));
            when(suggestionRepository.findAllById(Set.of(1, 2))).thenReturn(new java.util.ArrayList<>(List.of(s1, s2)));

            assertThatThrownBy(() -> service.fusionnerSuggestion(Set.of(1, 2)))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("fournisseurs differents");
        }

        @Test
        @DisplayName("rattache les lignes absentes a la suggestion la plus recente")
        void rattacheLesLignesAbsentes() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Suggestion recente = suggestion(1, f);
            recente.setUpdatedAt(LocalDateTime.now());
            Suggestion ancienne = suggestion(2, f);
            ancienne.setUpdatedAt(LocalDateTime.now().minusDays(1));
            SuggestionLine ligne = ligne(50, fournisseurProduit(10, produit(100, Status.ENABLE, 5), f), 4);
            ancienne.getSuggestionLines().add(ligne);
            when(suggestionRepository.findAllById(Set.of(1, 2))).thenReturn(new java.util.ArrayList<>(List.of(ancienne, recente)));

            service.fusionnerSuggestion(Set.of(1, 2));

            assertThat(ligne.getSuggestion()).isSameAs(recente);
            verify(suggestionLineRepository).save(ligne);
            verify(suggestionRepository).delete(ancienne);
            verify(suggestionRepository).save(recente);
        }

        @Test
        @DisplayName("supprime les lignes deja presentes dans la suggestion conservee")
        void supprimeLesLignesEnDoublon() {
            Fournisseur f = fournisseur(3, "LABOREX");
            SuggestionLine ligne = ligne(50, fournisseurProduit(10, produit(100, Status.ENABLE, 5), f), 4);
            Suggestion recente = suggestion(1, f);
            recente.setUpdatedAt(LocalDateTime.now());
            recente.getSuggestionLines().add(ligne);
            Suggestion ancienne = suggestion(2, f);
            ancienne.setUpdatedAt(LocalDateTime.now().minusDays(1));
            ancienne.getSuggestionLines().add(ligne);
            when(suggestionRepository.findAllById(Set.of(1, 2))).thenReturn(new java.util.ArrayList<>(List.of(recente, ancienne)));

            service.fusionnerSuggestion(Set.of(1, 2));

            verify(asCrud(suggestionLineRepository)).delete(ligne);
            verify(suggestionLineRepository, never()).save(ligne);
        }

        @Test
        @DisplayName("une suggestion seule est simplement reenregistree")
        void suggestionSeule() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            when(suggestionRepository.findAllById(Set.of(1))).thenReturn(new java.util.ArrayList<>(List.of(s)));

            service.fusionnerSuggestion(Set.of(1));

            verify(suggestionRepository).save(s);
            verify(suggestionRepository, never()).delete(any(Suggestion.class));
        }
    }

    @Nested
    @DisplayName("suppressions")
    class Suppressions {

        @Test
        @DisplayName("deleteSuggestion delegue au depot")
        void deleteSuggestion() {
            service.deleteSuggestion(Set.of(1, 2));

            verify(suggestionRepository).deleteAllById(Set.of(1, 2));
        }

        @Test
        @DisplayName("deleteSuggestionLine delegue au depot des lignes")
        void deleteSuggestionLine() {
            service.deleteSuggestionLine(Set.of(5));

            verify(suggestionLineRepository).deleteAllById(Set.of(5));
        }

        @Test
        @DisplayName("rejeterSuggestion supprime la suggestion trouvee")
        void rejeterSuggestion() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            service.rejeterSuggestion(1);

            verify(suggestionRepository).delete(s);
        }

        @Test
        @DisplayName("rejeterSuggestion ne fait rien quand la suggestion est introuvable")
        void rejeterSuggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());

            service.rejeterSuggestion(1);

            verify(suggestionRepository, never()).delete(any(Suggestion.class));
        }
    }

    @Nested
    @DisplayName("sanitize")
    class Sanitize {

        private SuggestionLine ligneAvecStock(Status status, int seuilMini, Integer stockQty) {
            Produit produit = produit(100, status, seuilMini);
            if (stockQty != null) {
                StockProduit stock = new StockProduit();
                stock.setStorage(mainStorage);
                stock.setQtyStock(stockQty);
                stock.setQtyUG(0);
                produit.setStockProduits(Set.of(stock));
            } else {
                produit.setStockProduits(Set.of());
            }
            return ligne(50, fournisseurProduit(10, produit, fournisseur(3, "LABOREX")), 4);
        }

        @Test
        @DisplayName("ne fait rien quand la suggestion est introuvable")
        void suggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());

            service.sanitize(1);

            verify(suggestionLineRepository, never()).deleteAll(any());
        }

        @Test
        @DisplayName("retire les lignes dont le produit est desactive")
        void produitDesactive() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            SuggestionLine ligne = ligneAvecStock(Status.DISABLE, 10, 2);
            s.getSuggestionLines().add(ligne);
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            service.sanitize(1);

            verify(suggestionLineRepository).deleteAll(List.of(ligne));
            verify(suggestionRepository).save(s);
        }

        @Test
        @DisplayName("retire les lignes dont le stock est repasse au-dessus du seuil")
        void stockAuDessusDuSeuil() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            SuggestionLine ligne = ligneAvecStock(Status.ENABLE, 5, 20);
            s.getSuggestionLines().add(ligne);
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            service.sanitize(1);

            verify(suggestionLineRepository).deleteAll(List.of(ligne));
        }

        @Test
        @DisplayName("ne retient que le stock de l emplacement principal")
        void ignoreLesAutresEmplacements() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            Produit produit = produit(100, Status.ENABLE, 20);
            Storage autre = new Storage();
            autre.setId(99);
            StockProduit ailleurs = new StockProduit();
            ailleurs.setStorage(autre);
            ailleurs.setQtyStock(500);
            ailleurs.setQtyUG(0);
            StockProduit principal = new StockProduit();
            principal.setStorage(mainStorage);
            principal.setQtyStock(5);
            principal.setQtyUG(0);
            produit.setStockProduits(new java.util.LinkedHashSet<>(List.of(principal, ailleurs)));
            s.getSuggestionLines().add(ligne(50, fournisseurProduit(10, produit, fournisseur(3, "LABOREX")), 4));
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            service.sanitize(1);

            // le stock hors emplacement principal (500) n est pas pris en compte
            verify(suggestionLineRepository).deleteAll(List.of());
        }

        @Test
        @DisplayName("conserve les lignes encore sous le seuil")
        void stockSousLeSeuil() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            s.getSuggestionLines().add(ligneAvecStock(Status.ENABLE, 20, 5));
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            service.sanitize(1);

            verify(suggestionLineRepository).deleteAll(List.of());
        }

        @Test
        @DisplayName("un produit sans stock sur l emplacement principal compte pour zero")
        void sansStockSurLEmplacement() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            s.getSuggestionLines().add(ligneAvecStock(Status.ENABLE, 20, null));
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            service.sanitize(1);

            verify(suggestionLineRepository).deleteAll(List.of());
        }
    }

    @Nested
    @DisplayName("commander")
    class Commander {

        @Test
        @DisplayName("supprime la suggestion une fois la commande creee")
        void commandeComplete() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            CommandeId commandeId = new CommandeId(12, LocalDate.now());
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));
            when(commandService.createCommandeFromSuggestion(s, 3)).thenReturn(commandeId);

            assertThat(service.commander(1, 3)).isSameAs(commandeId);

            verify(suggestionRepository).delete(s);
        }

        @Test
        @DisplayName("echoue quand la suggestion est introuvable")
        void suggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.commander(1, 3)).isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("commanderSelection")
    class CommanderSelection {

        private Suggestion suggestionAvecLignes(int nbLignes) {
            Fournisseur f = fournisseur(3, "LABOREX");
            Suggestion s = suggestion(1, f);
            for (int i = 0; i < nbLignes; i++) {
                s.getSuggestionLines().add(ligne(50 + i, fournisseurProduit(10 + i, produit(100 + i, Status.ENABLE, 5), f), 4));
            }
            when(suggestionRepository.getReferenceById(1)).thenReturn(s);
            return s;
        }

        @Test
        @DisplayName("supprime la suggestion quand toutes ses lignes sont commandees")
        void toutesLesLignesCommandees() {
            Suggestion s = suggestionAvecLignes(2);
            CommandeId commandeId = new CommandeId(12, LocalDate.now());
            when(commandService.createCommandeFromSelection(any(), any(), any())).thenReturn(commandeId);
            List<LigneSelection> lignes = List.of(new LigneSelection(50, 4), new LigneSelection(51, 4));

            assertThat(service.commanderSelection(new CommanderSelectionDTO(1, lignes, 3))).isSameAs(commandeId);

            verify(suggestionRepository).delete(s);
            verify(suggestionRepository, never()).save(any());
        }

        @Test
        @DisplayName("conserve la suggestion quand des lignes restent a commander")
        void lignesRestantes() {
            Suggestion s = suggestionAvecLignes(3);
            when(commandService.createCommandeFromSelection(any(), any(), any())).thenReturn(new CommandeId(12, LocalDate.now()));

            service.commanderSelection(new CommanderSelectionDTO(1, List.of(new LigneSelection(50, 4)), 3));

            verify(suggestionRepository, never()).delete(any(Suggestion.class));
            verify(suggestionRepository).save(s);
            assertThat(s.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getBudgetCommande")
    class GetBudgetCommande {

        private void stubMontants(long commande, long estime) {
            when(em.createNativeQuery(anyString())).thenAnswer(inv -> {
                String sql = inv.getArgument(0);
                return sql.contains("suggestion_line") ? suggestionQuery : commandeQuery;
            });
            when(commandeQuery.getSingleResult()).thenReturn(commande);
            when(suggestionQuery.getSingleResult()).thenReturn(estime);
        }

        private void stubBudget(String value) {
            com.kobe.warehouse.domain.AppConfiguration c = new com.kobe.warehouse.domain.AppConfiguration();
            c.setValue(value);
            when(appConfigurationService.findOneById(com.kobe.warehouse.constant.EntityConstant.APP_BUDGET_MENSUEL_COMMANDE))
                .thenReturn(value == null ? Optional.empty() : Optional.of(c));
        }

        @Test
        @DisplayName("budget illimite quand aucun budget n est configure")
        void budgetIllimiteSansConfiguration() {
            stubBudget(null);
            stubMontants(50000L, 20000L);

            BudgetCommandeDTO dto = service.getBudgetCommande();

            assertThat(dto.budgetMensuel()).isZero();
            assertThat(dto.budgetIllimite()).isTrue();
            assertThat(dto.budgetRestant()).isEqualTo(Long.MAX_VALUE);
            assertThat(dto.enDepassement()).isFalse();
        }

        @Test
        @DisplayName("budget illimite quand la valeur configuree vaut zero")
        void budgetIllimiteAvecZero() {
            stubBudget("0");
            stubMontants(50000L, 20000L);

            assertThat(service.getBudgetCommande().budgetIllimite()).isTrue();
        }

        @Test
        @DisplayName("une valeur non numerique est traitee comme un budget illimite")
        void valeurNonNumerique() {
            stubBudget("pas un nombre");
            stubMontants(0L, 0L);

            assertThat(service.getBudgetCommande().budgetMensuel()).isZero();
        }

        @Test
        @DisplayName("les espaces autour de la valeur sont ignores")
        void valeurAvecEspaces() {
            stubBudget("  100000  ");
            stubMontants(0L, 0L);

            assertThat(service.getBudgetCommande().budgetMensuel()).isEqualTo(100000L);
        }

        @Test
        @DisplayName("calcule le budget restant sans depassement")
        void sansDepassement() {
            stubBudget("100000");
            stubMontants(30000L, 20000L);

            BudgetCommandeDTO dto = service.getBudgetCommande();

            assertThat(dto.montantCommande()).isEqualTo(30000L);
            assertThat(dto.montantEstime()).isEqualTo(20000L);
            assertThat(dto.budgetRestant()).isEqualTo(70000L);
            assertThat(dto.enDepassement()).isFalse();
        }

        @Test
        @DisplayName("signale le depassement quand commande et estime depassent le budget")
        void avecDepassement() {
            stubBudget("100000");
            stubMontants(80000L, 30000L);

            BudgetCommandeDTO dto = service.getBudgetCommande();

            assertThat(dto.budgetRestant()).isEqualTo(20000L);
            assertThat(dto.enDepassement()).isTrue();
        }
    }

    @Nested
    @DisplayName("addSuggestionLine")
    class AddSuggestionLine {

        @Test
        @DisplayName("echoue quand la suggestion est introuvable")
        void suggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());
            SuggestionLineDTO dto = ligneDTO(null == null ? 0 : 0, 3);

            assertThatThrownBy(() -> service.addSuggestionLine(1, dto)).isInstanceOf(java.util.NoSuchElementException.class);
        }

        @Test
        @DisplayName("cumule la quantite sur une ligne existante")
        void cumuleSurLigneExistante() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Suggestion s = suggestion(1, f);
            SuggestionLine existante = ligne(50, fournisseurProduit(10, produit(101, Status.ENABLE, 5), f), 4);
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));
            when(suggestionLineRepository.findBySuggestionIdAndFournisseurProduitProduitId(1, 101)).thenReturn(Optional.of(existante));

            service.addSuggestionLine(1, ligneDTO(1, 3));

            assertThat(existante.getQuantity()).isEqualTo(7);
            verify(suggestionLineRepository).save(existante);
            verify(suggestionRepository).save(s);
        }

        @Test
        @DisplayName("cree une ligne pour le fournisseur de la suggestion")
        void creeUneNouvelleLigne() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Suggestion s = suggestion(1, f);
            FournisseurProduit fp = fournisseurProduit(10, produit(101, Status.ENABLE, 5), f);
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));
            when(suggestionLineRepository.findBySuggestionIdAndFournisseurProduitProduitId(1, 101)).thenReturn(Optional.empty());
            when(fournisseurProduitRepository.findOneByProduitIdAndFournisseurId(101, 3)).thenReturn(Optional.of(fp));

            service.addSuggestionLine(1, ligneDTO(1, 3));

            ArgumentCaptor<SuggestionLine> captor = ArgumentCaptor.forClass(SuggestionLine.class);
            verify(suggestionLineRepository).save(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(3);
            assertThat(captor.getValue().getFournisseurProduit()).isSameAs(fp);
            assertThat(captor.getValue().getSuggestion()).isSameAs(s);
            assertThat(s.getSuggestionLines()).hasSize(1);
        }

        @Test
        @DisplayName("echoue quand le produit n est pas reference chez le fournisseur")
        void produitNonReference() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Suggestion s = suggestion(1, f);
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));
            when(suggestionLineRepository.findBySuggestionIdAndFournisseurProduitProduitId(1, 101)).thenReturn(Optional.empty());
            when(fournisseurProduitRepository.findOneByProduitIdAndFournisseurId(101, 3)).thenReturn(Optional.empty());
            SuggestionLineDTO dto = ligneDTO(1, 3);

            assertThatThrownBy(() -> service.addSuggestionLine(1, dto)).isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("quantites manuelles")
    class QuantitesManuelles {

        @Test
        @DisplayName("updateSuggestionLinQuantity marque la ligne comme modifiee a la main")
        void marqueLaModificationManuelle() {
            SuggestionLine line = ligne(50, null, 4);
            when(suggestionLineRepository.findById(1)).thenReturn(Optional.of(line));

            service.updateSuggestionLinQuantity(ligneDTO(1, 9));

            assertThat(line.getQuantity()).isEqualTo(9);
            assertThat(line.isQuantiteModifieeManuel()).isTrue();
            verify(suggestionLineRepository).save(line);
        }

        @Test
        @DisplayName("updateSuggestionLinQuantity echoue quand la ligne est introuvable")
        void ligneIntrouvable() {
            when(suggestionLineRepository.findById(1)).thenReturn(Optional.empty());
            SuggestionLineDTO dto = ligneDTO(1, 9);

            assertThatThrownBy(() -> service.updateSuggestionLinQuantity(dto)).isInstanceOf(java.util.NoSuchElementException.class);
        }

        @Test
        @DisplayName("resetQuantiteManuelle rend la ligne au calcul automatique")
        void resetQuantiteManuelle() {
            SuggestionLine line = ligne(50, null, 4);
            line.setQuantiteModifieeManuel(true);
            when(suggestionLineRepository.findById(50)).thenReturn(Optional.of(line));

            service.resetQuantiteManuelle(50);

            assertThat(line.isQuantiteModifieeManuel()).isFalse();
            verify(suggestionLineRepository).save(line);
        }

        @Test
        @DisplayName("resetQuantiteManuelle ne fait rien quand la ligne est introuvable")
        void resetLigneIntrouvable() {
            when(suggestionLineRepository.findById(50)).thenReturn(Optional.empty());

            service.resetQuantiteManuelle(50);

            verify(suggestionLineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("validerSuggestion")
    class ValiderSuggestion {

        @Test
        @DisplayName("passe la suggestion en validee et trace le valideur")
        void valide() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));

            service.validerSuggestion(1);

            assertThat(s.getStatut()).isEqualTo(StatutSuggession.VALIDEE);
            assertThat(s.getValidePar()).isSameAs(currentUser);
            assertThat(s.getDateValidation()).isNotNull();
            verify(suggestionRepository).save(s);
        }

        @Test
        @DisplayName("ne fait rien quand la suggestion est introuvable")
        void suggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());

            service.validerSuggestion(1);

            verify(suggestionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("exports")
    class Exports {

        @Test
        @DisplayName("exportToCsv echoue quand la suggestion est introuvable")
        void csvSuggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.exportToCsv(1)).isInstanceOf(GenericError.class).hasMessageContaining("non trouvée");
        }

        @Test
        @DisplayName("exportToCsv compose les colonnes fixes quand aucune conso mensuelle n est disponible")
        void csvSansConsommation() throws Exception {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));
            when(suggestionLineRepository.fetchSuggestionLinesWithConsommation(any(), isNull(), isNull(), any(), any(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(ligneDTO(1, 4))));
            when(csvExportService.createSimpleCsvReport(anyString(), any(), any())).thenReturn(new byte[] { 1 });
            when(csvExportService.addUtf8Bom(any())).thenReturn(new byte[] { 9 });

            assertThat(service.exportToCsv(1)).containsExactly(9);

            ArgumentCaptor<String[]> headers = ArgumentCaptor.forClass(String[].class);
            verify(csvExportService).createSimpleCsvReport(anyString(), headers.capture(), any());
            assertThat(headers.getValue()).containsExactly(
                "Code CIP",
                "Code EAN",
                "Désignation",
                "Stock",
                "Qté suggérée",
                "Prix achat",
                "Prix vente"
            );
        }

        @Test
        @DisplayName("exportToCsv ajoute une colonne par mois de consommation")
        void csvAvecConsommation() throws Exception {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            Map<Mois, Integer> conso = new LinkedHashMap<>();
            conso.put(Mois.JANVIER, 4);
            conso.put(Mois.FEVRIER, 7);
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));
            when(suggestionLineRepository.fetchSuggestionLinesWithConsommation(any(), isNull(), isNull(), any(), any(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(ligneDTO(1, 4, conso), ligneDTO(2, 6))));
            when(csvExportService.createSimpleCsvReport(anyString(), any(), any())).thenReturn(new byte[] { 1 });
            when(csvExportService.addUtf8Bom(any())).thenReturn(new byte[] { 9 });

            service.exportToCsv(1);

            ArgumentCaptor<String[]> headers = ArgumentCaptor.forClass(String[].class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String[]>> rows = ArgumentCaptor.forClass(List.class);
            verify(csvExportService).createSimpleCsvReport(anyString(), headers.capture(), rows.capture());
            assertThat(headers.getValue()).hasSize(9);
            assertThat(headers.getValue()[7]).isEqualTo("Conso. " + Mois.JANVIER.getLibelle());
            // la ligne sans consommation retombe sur des zeros
            assertThat(rows.getValue().get(1)[7]).isEqualTo("0");
            assertThat(rows.getValue().get(0)[7]).isEqualTo("4");
        }

        @Test
        @DisplayName("exportToPdf echoue quand la suggestion est introuvable")
        void pdfSuggestionIntrouvable() {
            when(suggestionRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.exportToPdf(1)).isInstanceOf(GenericError.class).hasMessageContaining("non trouvée");
        }

        @Test
        @DisplayName("exportToPdf transmet reference, fournisseur et lignes au service de rapport")
        void pdf() {
            Suggestion s = suggestion(1, fournisseur(3, "LABOREX"));
            SuggestionLineDTO ligne = ligneDTO(1, 4);
            when(suggestionRepository.findById(1)).thenReturn(Optional.of(s));
            when(suggestionLineRepository.fetchSuggestionLinesWithConsommation(any(), isNull(), isNull(), any(), any(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(ligne)));
            when(suggestionPdfReportService.export(anyString(), anyString(), any())).thenReturn(new byte[] { 7 });

            assertThat(service.exportToPdf(1)).containsExactly(7);

            verify(suggestionPdfReportService).export("SUG-1", "LABOREX", List.of(ligne));
        }
    }

    @Nested
    @DisplayName("suggestionQuantiteProduitVendus")
    class SuggestionQuantiteProduitVendus {

        private Produit produitAvecPrincipal(int id, Fournisseur fournisseur) {
            Produit p = produit(id, Status.ENABLE, 5);
            p.setFournisseurProduitPrincipal(fournisseurProduit(id * 10, p, fournisseur));
            return p;
        }

        @ParameterizedTest(name = "une liste vide ou nulle ne suggere rien")
        @NullAndEmptySource
        void listeVide(List<QauntiteProduitVendus> vendus) {
            assertThat(service.suggestionQuantiteProduitVendus(vendus, Boolean.TRUE)).isZero();

            verifyNoInteractions(suggestionRepository);
        }

        @Test
        @DisplayName("ecarte les produits sans identifiant")
        void produitSansIdentifiant() {
            assertThat(service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(null, 3, 2, 5)), Boolean.TRUE)).isZero();
        }

        @Test
        @DisplayName("ecarte les produits que l etat interdit de suggerer")
        void produitNonSuggerable() {
            when(etatProduitService.canSuggere(101)).thenReturn(false);

            assertThat(service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(101, 3, 2, 5)), Boolean.TRUE)).isZero();

            verifyNoInteractions(produitRepository);
        }

        @Test
        @DisplayName("ecarte les produits sans fournisseur principal")
        void produitSansFournisseurPrincipal() {
            when(etatProduitService.canSuggere(101)).thenReturn(true);
            when(produitRepository.findAllById(Set.of(101))).thenReturn(List.of(produit(101, Status.ENABLE, 5)));

            assertThat(service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(101, 3, 2, 5)), Boolean.TRUE)).isZero();
        }

        @Test
        @DisplayName("cree une suggestion AUTO et sa ligne quand rien n existe")
        void creeUneSuggestionEtUneLigne() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Produit p = produitAvecPrincipal(101, f);
            when(etatProduitService.canSuggere(101)).thenReturn(true);
            when(produitRepository.findAllById(Set.of(101))).thenReturn(List.of(p));
            when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(any(), any())).thenReturn(List.of());
            when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(TypeSuggession.AUTO, 3, 1))
                .thenReturn(Optional.empty());

            int count = service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(101, 3, 2, 5)), Boolean.TRUE);

            assertThat(count).isEqualTo(1);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Suggestion>> captor = ArgumentCaptor.forClass(List.class);
            verify(suggestionRepository).saveAll(captor.capture());
            Suggestion saved = captor.getValue().getFirst();
            assertThat(saved.getTypeSuggession()).isEqualTo(TypeSuggession.AUTO);
            assertThat(saved.getFournisseur()).isSameAs(f);
            assertThat(saved.getMagasin()).isSameAs(magasin);
            assertThat(saved.getLastUserEdit()).isSameAs(currentUser);
            assertThat(saved.getSuggessionReference()).endsWith("0001");
            assertThat(saved.getSuggestionLines()).hasSize(1);
            assertThat(saved.getSuggestionLines().iterator().next().getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("prend la quantite de reappro quand la quantite vendue n est pas demandee")
        void quantiteDeReappro() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Produit p = produitAvecPrincipal(101, f);
            when(etatProduitService.canSuggere(101)).thenReturn(true);
            when(produitRepository.findAllById(Set.of(101))).thenReturn(List.of(p));
            when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(any(), any())).thenReturn(List.of());
            when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

            service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(101, 3, 8, 5)), Boolean.FALSE);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Suggestion>> captor = ArgumentCaptor.forClass(List.class);
            verify(suggestionRepository).saveAll(captor.capture());
            assertThat(captor.getValue().getFirst().getSuggestionLines().iterator().next().getQuantity()).isEqualTo(8);
        }

        @Test
        @DisplayName("ignore les quantites nulles ou negatives")
        void quantiteNonPositive() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Produit p = produitAvecPrincipal(101, f);
            when(etatProduitService.canSuggere(101)).thenReturn(true);
            when(produitRepository.findAllById(Set.of(101))).thenReturn(List.of(p));
            when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(any(), any())).thenReturn(List.of());
            when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

            assertThat(service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(101, 0, 0, 5)), Boolean.TRUE)).isZero();

            verify(suggestionLineRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("remplace la quantite d une ligne AUTO deja presente")
        void remplaceUneLigneExistante() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Produit p = produitAvecPrincipal(101, f);
            SuggestionLine existante = ligne(50, p.getFournisseurProduitPrincipal(), 4);
            when(etatProduitService.canSuggere(101)).thenReturn(true);
            when(produitRepository.findAllById(Set.of(101))).thenReturn(List.of(p));
            when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(TypeSuggession.AUTO, Set.of(1010)))
                .thenReturn(List.of(existante));
            when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

            service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(101, 9, 2, 5)), Boolean.TRUE);

            assertThat(existante.getQuantity()).isEqualTo(9);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SuggestionLine>> captor = ArgumentCaptor.forClass(List.class);
            verify(suggestionLineRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).containsExactly(existante);
        }

        @Test
        @DisplayName("enregistre explicitement les lignes ajoutees a une suggestion deja persistee")
        void ajouteUneLigneAUneSuggestionExistante() {
            Fournisseur f = fournisseur(3, "LABOREX");
            Produit p = produitAvecPrincipal(101, f);
            Suggestion existante = suggestion(1, f);
            when(etatProduitService.canSuggere(101)).thenReturn(true);
            when(produitRepository.findAllById(Set.of(101))).thenReturn(List.of(p));
            when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(any(), any())).thenReturn(List.of());
            when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(TypeSuggession.AUTO, 3, 1))
                .thenReturn(Optional.of(existante));

            service.suggestionQuantiteProduitVendus(List.of(new QauntiteProduitVendus(101, 3, 2, 5)), Boolean.TRUE);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SuggestionLine>> captor = ArgumentCaptor.forClass(List.class);
            verify(suggestionLineRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(existante.getSuggestionLines()).hasSize(1);
        }

        @Test
        @DisplayName("regroupe les produits par fournisseur principal")
        void regroupeParFournisseur() {
            Fournisseur f1 = fournisseur(3, "LABOREX");
            Fournisseur f2 = fournisseur(4, "COPHARMED");
            Produit p1 = produitAvecPrincipal(101, f1);
            Produit p2 = produitAvecPrincipal(102, f2);
            when(etatProduitService.canSuggere(anyInt())).thenReturn(true);
            when(produitRepository.findAllById(any())).thenReturn(List.of(p1, p2));
            when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(any(), any())).thenReturn(List.of());
            when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

            int count = service.suggestionQuantiteProduitVendus(
                List.of(new QauntiteProduitVendus(101, 3, 2, 5), new QauntiteProduitVendus(102, 4, 2, 5)),
                Boolean.TRUE
            );

            assertThat(count).isEqualTo(2);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Suggestion>> captor = ArgumentCaptor.forClass(List.class);
            verify(suggestionRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }
    }
}
