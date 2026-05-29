package com.kobe.warehouse.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.ReferenceService;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitaires pour {@link SemoisBatchJobService}.
 * Cas couverts : absence magasin, pas de produit, protection flag manuel,
 * update existant, nouvelle suggestion, colisage, réintégration exclusions.
 */
@ExtendWith(MockitoExtension.class)
class SemoisBatchJobServiceTest {

    @Mock private ProduitRepository produitRepository;
    @Mock private SemoisConfigurationRepository semoisConfigurationRepository;
    @Mock private SuggestionRepository suggestionRepository;
    @Mock private SuggestionLineRepository suggestionLineRepository;
    @Mock private OrderLineRepository orderLineRepository;
    @Mock private EtatProduitService etatProduitService;
    @Mock private ReferenceService referenceService;
    @Mock private EntityManager em;

    private SemoisBatchJobService service;
    private Magasin magasin;
    private Fournisseur fournisseur;

    @BeforeEach
    void setUp() {
        service = new SemoisBatchJobService(
            produitRepository, semoisConfigurationRepository,
            suggestionRepository, suggestionLineRepository, orderLineRepository,
            etatProduitService, referenceService, em
        );

        magasin = mock(Magasin.class);
        lenient().when(magasin.getId()).thenReturn(1);
        lenient().when(em.find(Magasin.class, EntityConstant.DEFAULT_MAGASIN)).thenReturn(magasin);

        fournisseur = mock(Fournisseur.class);
        lenient().when(fournisseur.getId()).thenReturn(10);

        lenient().when(orderLineRepository.findPendingQtyByProduitIds(anySet())).thenReturn(List.of());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Produit mockProduit(int id, int seuilMini, int stockActuel, FournisseurProduit fp) {
        Produit p = mock(Produit.class);
        when(p.getId()).thenReturn(id);
        when(p.getQtySeuilMini()).thenReturn(seuilMini);
        when(p.getFournisseurProduitPrincipal()).thenReturn(fp);
        Storage storage = mock(Storage.class);
        when(storage.getMagasin()).thenReturn(magasin);
        StockProduit sp = mock(StockProduit.class);
        when(sp.getStorage()).thenReturn(storage);
        when(sp.getTotalStockQuantity()).thenReturn(stockActuel);
        when(p.getStockProduits()).thenReturn(new HashSet<>(Set.of(sp)));
        return p;
    }

    private FournisseurProduit mockFpColis1(int id) {
        FournisseurProduit fp = mock(FournisseurProduit.class);
        when(fp.getId()).thenReturn(id);
        when(fp.getFournisseur()).thenReturn(fournisseur);
        lenient().when(fp.getQteColis()).thenReturn(1);
        lenient().when(fp.getQteMinimaleCommande()).thenReturn(0);
        lenient().when(fp.appliquerColisage(any(int.class))).thenCallRealMethod();
        return fp;
    }

    private void stubBaseRepos(Produit produit, FournisseurProduit fp,
                                List<SuggestionLine> existingLines,
                                Optional<Suggestion> existingSuggestion) {

        when(semoisConfigurationRepository.findByProduitIdIn(anySet())).thenReturn(List.of());
        when(etatProduitService.produitsNonSuggerables(anySet())).thenReturn(Set.of());
        when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(
            eq(TypeSuggession.AUTO), anySet())).thenReturn(existingLines);
        when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(
            any(), any(), any())).thenReturn(existingSuggestion);
        if (existingSuggestion.isEmpty()) {
            when(referenceService.buildSuggestionReference()).thenReturn("001");
        }
    }

    // ── Sorties anticipées ────────────────────────────────────────────────────

    @Nested @DisplayName("Sorties anticipées")
    class SortiesAnticipees {

        @Test @DisplayName("Pas de magasin → rien persisté")
        void sansMagasin_neRienPersiste() {
            when(em.find(Magasin.class, EntityConstant.DEFAULT_MAGASIN)).thenReturn(null);
            service.creerSuggestionBatch();
            verify(suggestionRepository, never()).saveAll(any());
        }

        @Test @DisplayName("Aucun produit éligible → rien persisté")
        void sansProduits_neRienPersiste() {
            service.creerSuggestionBatch();
            verify(suggestionRepository, never()).saveAll(any());
            verify(suggestionLineRepository, never()).saveAll(any());
        }
    }

    // ── Nouvelle création ─────────────────────────────────────────────────────

    @Nested @DisplayName("Nouvelle création")
    class NouvelleCreation {

        @Test @DisplayName("Produit sous seuil → Suggestion GENEREE + fournisseur correctement renseigné")
        void sousSeuil_suggestionCree() {
            FournisseurProduit fp = mockFpColis1(100);
            Produit produit = mockProduit(1, 10, 2, fp);
            stubBaseRepos(produit, fp, List.of(), Optional.empty());

            service.creerSuggestionBatch();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Suggestion>> cap = ArgumentCaptor.forClass(List.class);
            verify(suggestionRepository).saveAll(cap.capture());
            assertThat(cap.getValue()).hasSize(1);
            Suggestion s = cap.getValue().getFirst();
            assertThat(s.getTypeSuggession()).isEqualTo(TypeSuggession.AUTO);
            assertThat(s.getStatut()).isEqualTo(StatutSuggession.GENEREE);
            assertThat(s.getFournisseur()).isEqualTo(fournisseur);
        }

        @Test @DisplayName("Produit jamais vendu sans seuil manuel → aucune suggestion")
        void jamaisVendu_sansSeuilManuel_aucuneSuggestion() {
            FournisseurProduit fp = mockFpColis1(100);
            Produit produit = mockProduit(1, 0, 0, fp); // seuil 0, stock 0, jamais vendu
            stubBaseRepos(produit, fp, List.of(), Optional.empty());

            service.creerSuggestionBatch();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Suggestion>> cap = ArgumentCaptor.forClass(List.class);
            verify(suggestionRepository).saveAll(cap.capture());
            assertThat(cap.getValue()).isEmpty();
        }

        @Test @DisplayName("Stock suffisant → liste suggestions sauvegardée vide")
        void stockSuffisant_aucuneSuggestion() {
            FournisseurProduit fp = mockFpColis1(100);
            Produit produit = mockProduit(1, 5, 10, fp); // stock(10) >= seuil(5)
            stubBaseRepos(produit, fp, List.of(), Optional.empty());

            service.creerSuggestionBatch();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Suggestion>> cap = ArgumentCaptor.forClass(List.class);
            verify(suggestionRepository).saveAll(cap.capture());
            assertThat(cap.getValue()).isEmpty();
        }
    }

    // ── Protection manuelle ───────────────────────────────────────────────────

    @Nested @DisplayName("Protection quantiteModifieeManuel")
    class ProtectionManuelle {

        @Test @DisplayName("Ligne verrouillée (manuel=true) → setQuantity() jamais appelé")
        void ligneVerrouillee_nonEcrasee() {
            FournisseurProduit fp = mockFpColis1(100);
            Produit produit = mockProduit(1, 10, 2, fp);
            SuggestionLine ligne = mock(SuggestionLine.class);
            when(ligne.getFournisseurProduit()).thenReturn(fp);
            when(ligne.isQuantiteModifieeManuel()).thenReturn(true); // 🔒

            stubBaseRepos(produit, fp, List.of(ligne), Optional.empty());

            service.creerSuggestionBatch();

            verify(ligne, never()).setQuantity(any());
        }

        @Test @DisplayName("Ligne déverrouillée (manuel=false) → setQuantity(8) appelé")
        void ligneDeverrouillee_quantiteMisAJour() {
            FournisseurProduit fp = mockFpColis1(100);
            Produit produit = mockProduit(1, 10, 2, fp); // besoin = 10-2 = 8
            SuggestionLine ligne = mock(SuggestionLine.class);
            when(ligne.getFournisseurProduit()).thenReturn(fp);
            when(ligne.isQuantiteModifieeManuel()).thenReturn(false);

            Suggestion existing = mock(Suggestion.class);
            stubBaseRepos(produit, fp, List.of(ligne), Optional.of(existing));

            service.creerSuggestionBatch();

            verify(ligne, times(1)).setQuantity(8);
        }
    }

    // ── Colisage ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("Colisage (S4.4)")
    class ColisageTest {

        @Test @DisplayName("qteColis=3, brute=5 → setQuantity(6)")
        void colisage_arrondisAuMultiple() {
            // seuil=8, stock=3 → brute = 8-3 = 5 → ceil(5/3)*3 = 6
            FournisseurProduit fp = mock(FournisseurProduit.class);
            when(fp.getId()).thenReturn(200);
            when(fp.getFournisseur()).thenReturn(fournisseur);
            FournisseurProduit realFp = new FournisseurProduit();
            realFp.setQteColis(3);
            realFp.setQteMinimaleCommande(0);
            when(fp.appliquerColisage(5)).thenReturn(realFp.appliquerColisage(5)); // 6

            Produit produit = mockProduit(1, 8, 3, fp);
            SuggestionLine ligne = mock(SuggestionLine.class);
            when(ligne.getFournisseurProduit()).thenReturn(fp);
            when(ligne.isQuantiteModifieeManuel()).thenReturn(false);

            Suggestion existing = mock(Suggestion.class);
            stubBaseRepos(produit, fp, List.of(ligne), Optional.of(existing));

            service.creerSuggestionBatch();

            verify(ligne).setQuantity(6);
        }
    }

    // ── Exclusions ────────────────────────────────────────────────────────────

    @Nested @DisplayName("Réintégration exclusions (S4.3)")
    class Exclusions {

        @Test @DisplayName("reintegrerExclusionsExpirees → repository appelé 1 fois")
        void reintegrer_appelleRepository() {
            when(semoisConfigurationRepository.reintegrerExclusionsExpirees()).thenReturn(2);
            service.reintegrerExclusionsExpirees();
            verify(semoisConfigurationRepository, times(1)).reintegrerExclusionsExpirees();
        }

        @Test @DisplayName("0 expiration → pas d'exception")
        void reintegrer_aucuneExpiration_sanserreur() {
            when(semoisConfigurationRepository.reintegrerExclusionsExpirees()).thenReturn(0);
            service.reintegrerExclusionsExpirees();
            verify(semoisConfigurationRepository, times(1)).reintegrerExclusionsExpirees();
        }
    }
}
