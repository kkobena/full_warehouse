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
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.scheduler.dto.SemoisEligibleItem;
import com.kobe.warehouse.service.EtatProduitService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

/**
 * Tests unitaires pour {@link SemoisBatchJobService}.
 *
 * <p>Le batch parcourt le catalogue page par page : le repository ne rend pas des entités
 * {@link com.kobe.warehouse.domain.Produit} mais une projection {@link SemoisEligibleItem} portant
 * déjà le stock total, l'objectif calculé et le conditionnement. Les tests simulent donc des pages
 * de projections, ce qui est aussi ce que le batch reçoit en production.
 *
 * <p>Cas couverts : absence de magasin, page vide, création, protection du flag manuel, mise à jour,
 * colisage, exclusion active, réintégration des exclusions.
 */
@ExtendWith(MockitoExtension.class)
class SemoisBatchJobServiceTest {

    private static final int MAGASIN_ID = 1;
    // Types boites : SemoisEligibleItem porte deux constructeurs (long et Long) et tout appel
    // melant litteraux et boxing devient ambigu. Passer des Integer laisse le constructeur
    // canonique s appliquer directement.
    private static final Integer FOURNISSEUR_ID = 10;
    private static final Integer QTY_APPRO = 0;
    private static final Integer COLIS_UNITAIRE = 1;
    private static final Integer SANS_MINIMUM = 0;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private SemoisConfigurationRepository semoisConfigurationRepository;

    @Mock
    private SuggestionRepository suggestionRepository;

    @Mock
    private SuggestionLineRepository suggestionLineRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private EtatProduitService etatProduitService;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private EntityManager em;

    private SemoisBatchJobService service;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new SemoisBatchJobService(
            produitRepository,
            semoisConfigurationRepository,
            suggestionRepository,
            suggestionLineRepository,
            orderLineRepository,
            etatProduitService,
            referenceService,
            em
        );

        magasin = mock(Magasin.class);
        lenient().when(magasin.getId()).thenReturn(MAGASIN_ID);
        lenient().when(em.find(Magasin.class, EntityConstant.DEFAULT_MAGASIN)).thenReturn(magasin);
        lenient().when(em.getReference(eq(Fournisseur.class), any())).thenReturn(mock(Fournisseur.class));
        lenient().when(em.getReference(eq(Magasin.class), any())).thenReturn(magasin);
        lenient().when(em.getReference(eq(FournisseurProduit.class), any())).thenReturn(mock(FournisseurProduit.class));

        lenient().when(orderLineRepository.findPendingQtyByProduitIds(anySet())).thenReturn(List.of());
        lenient().when(etatProduitService.produitsNonSuggerables(anySet())).thenReturn(Set.of());
        lenient().when(referenceService.buildSuggestionReference()).thenReturn("001");
    }

    // ── Sorties anticipées ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sorties anticipées")
    class SortiesAnticipees {

        @Test
        @DisplayName("Pas de magasin → aucune requête, rien persisté")
        void sansMagasin_neRienPersiste() {
            when(em.find(Magasin.class, EntityConstant.DEFAULT_MAGASIN)).thenReturn(null);

            service.creerSuggestionBatch();

            verify(produitRepository, never()).findSemoisEligibleItemsSlice(any(), any());
            verify(suggestionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Aucun produit éligible → rien persisté, pas de nettoyage")
        void sansProduits_neRienPersiste() {
            pageDe();

            service.creerSuggestionBatch();

            verify(suggestionRepository, never()).saveAll(any());
            verify(suggestionLineRepository, never()).saveAll(any());
            verify(suggestionRepository, never()).deleteEmptyAutoSuggestions();
        }
    }

    // ── Nouvelle création ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Nouvelle création")
    class NouvelleCreation {

        @Test
        @DisplayName("Produit sous l'objectif → suggestion AUTO/GENEREE portant sa ligne")
        void sousObjectif_suggestionCreee() {
            pageDe(item(1, 100, 2L, 10));
            lignesExistantes();
            aucuneSuggestionExistante();

            service.creerSuggestionBatch();

            Suggestion suggestion = suggestionEnregistree();
            assertThat(suggestion.getTypeSuggession()).isEqualTo(TypeSuggession.AUTO);
            assertThat(suggestion.getStatut()).isEqualTo(StatutSuggession.GENEREE);
            assertThat(suggestion.getSuggessionReference()).endsWith("001");
            assertThat(suggestion.getSuggestionLines()).hasSize(1);
            assertThat(suggestion.getSuggestionLines().iterator().next().getQuantity()).isEqualTo(8);
        }

        @Test
        @DisplayName("Stock suffisant → aucune suggestion créée")
        void stockSuffisant_aucuneSuggestion() {
            pageDe(item(1, 100, 10L, 5));
            lignesExistantes();
            aucuneSuggestionExistante();

            service.creerSuggestionBatch();

            assertThat(suggestionsEnregistrees()).isEmpty();
        }

        @Test
        @DisplayName("Une commande en cours comble le besoin : le stock virtuel compte")
        void commandeEnCours_aucuneSuggestion() {
            pageDe(item(1, 100, 2L, 10));
            lignesExistantes();
            aucuneSuggestionExistante();
            when(orderLineRepository.findPendingQtyByProduitIds(anySet())).thenReturn(List.<Object[]>of(new Object[] { 1, 8 }));

            service.creerSuggestionBatch();

            assertThat(suggestionsEnregistrees()).isEmpty();
        }

        @Test
        @DisplayName("Produit non suggérable (périmé, retiré…) → écarté")
        void produitNonSuggerable_ecarte() {
            pageDe(item(1, 100, 0L, 10));
            lignesExistantes();
            aucuneSuggestionExistante();
            when(etatProduitService.produitsNonSuggerables(anySet())).thenReturn(Set.of(1));

            service.creerSuggestionBatch();

            assertThat(suggestionsEnregistrees()).isEmpty();
        }

        @Test
        @DisplayName("Sans objectif calculé, le seuil saisi par le pharmacien prend le relais")
        void sansObjectifCalcule_seuilManuel() {
            pageDe(projection(1, 100, 0L, null, 6, COLIS_UNITAIRE, SANS_MINIMUM, null, null));
            lignesExistantes();
            aucuneSuggestionExistante();

            service.creerSuggestionBatch();

            assertThat(suggestionEnregistree().getSuggestionLines().iterator().next().getQuantity()).isEqualTo(6);
        }
    }

    // ── Protection manuelle ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Protection quantiteModifieeManuel")
    class ProtectionManuelle {

        @Test
        @DisplayName("Ligne verrouillée (manuel=true) → setQuantity() jamais appelé")
        void ligneVerrouillee_nonEcrasee() {
            SuggestionLine ligne = ligneExistante(100, true);
            pageDe(item(1, 100, 2L, 10));
            lignesExistantes(ligne);
            aucuneSuggestionExistante();

            service.creerSuggestionBatch();

            verify(ligne, never()).setQuantity(any());
        }

        @Test
        @DisplayName("Ligne déverrouillée (manuel=false) → quantité mise à jour")
        void ligneDeverrouillee_quantiteMiseAJour() {
            SuggestionLine ligne = ligneExistante(100, false);
            pageDe(item(1, 100, 2L, 10));
            lignesExistantes(ligne);
            suggestionExistante();

            service.creerSuggestionBatch();

            verify(ligne, times(1)).setQuantity(8);
        }

        @Test
        @DisplayName("Le besoin comblé retire la ligne, sauf si elle est verrouillée")
        void besoinComble_ligneSupprimee() {
            SuggestionLine libre = ligneExistante(100, false);
            SuggestionLine verrouillee = ligneExistante(200, true);
            pageDe(item(1, 100, 10L, 5), item(2, 200, 10L, 5));
            lignesExistantes(libre, verrouillee);
            suggestionExistante();

            service.creerSuggestionBatch();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SuggestionLine>> supprimees = ArgumentCaptor.forClass(List.class);
            verify(suggestionLineRepository).deleteAll(supprimees.capture());
            assertThat(supprimees.getValue()).containsExactly(libre);
        }
    }

    // ── Colisage ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Colisage (S4.4)")
    class ColisageTest {

        @Test
        @DisplayName("qteColis=3, besoin=5 → quantité arrondie à 6")
        void colisage_arrondiAuMultiple() {
            SuggestionLine ligne = ligneExistante(200, false);
            // objectif 8, stock 3 → besoin 5 → ceil(5/3) × 3 = 6
            pageDe(projection(1, 200, 3L, 8, 8, 3, SANS_MINIMUM, null, null));
            lignesExistantes(ligne);
            suggestionExistante();

            service.creerSuggestionBatch();

            verify(ligne).setQuantity(6);
        }

        @Test
        @DisplayName("La quantité minimale de commande l'emporte sur le besoin réel")
        void quantiteMinimaleDeCommande() {
            SuggestionLine ligne = ligneExistante(200, false);
            pageDe(projection(1, 200, 8L, 10, 10, COLIS_UNITAIRE, 20, null, null));
            lignesExistantes(ligne);
            suggestionExistante();

            service.creerSuggestionBatch();

            verify(ligne).setQuantity(20);
        }
    }

    // ── Exclusions ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Réintégration exclusions (S4.3)")
    class Exclusions {

        @Test
        @DisplayName("Une exclusion encore active écarte le produit et retire sa ligne")
        void exclusionActive_produitEcarte() {
            SuggestionLine ligne = ligneExistante(100, false);
            pageDe(projection(1, 100, 0L, 10, 10, COLIS_UNITAIRE, SANS_MINIMUM, LocalDateTime.now().minusDays(2), 30));
            lignesExistantes(ligne);
            suggestionExistante();

            service.creerSuggestionBatch();

            verify(ligne, never()).setQuantity(any());
            verify(suggestionLineRepository).deleteAll(List.of(ligne));
        }

        @Test
        @DisplayName("Une exclusion expirée laisse le produit revenir dans le batch")
        void exclusionExpiree_produitRepris() {
            SuggestionLine ligne = ligneExistante(100, false);
            pageDe(projection(1, 100, 2L, 10, 10, COLIS_UNITAIRE, SANS_MINIMUM, LocalDateTime.now().minusDays(40), 30));
            lignesExistantes(ligne);
            suggestionExistante();

            service.creerSuggestionBatch();

            verify(ligne).setQuantity(8);
        }

        @Test
        @DisplayName("reintegrerExclusionsExpirees → repository appelé une fois")
        void reintegrer_appelleRepository() {
            when(semoisConfigurationRepository.reintegrerExclusionsExpirees()).thenReturn(2);

            service.reintegrerExclusionsExpirees();

            verify(semoisConfigurationRepository, times(1)).reintegrerExclusionsExpirees();
        }

        @Test
        @DisplayName("0 expiration → pas d'exception")
        void reintegrer_aucuneExpiration_sansErreur() {
            when(semoisConfigurationRepository.reintegrerExclusionsExpirees()).thenReturn(0);

            service.reintegrerExclusionsExpirees();

            verify(semoisConfigurationRepository, times(1)).reintegrerExclusionsExpirees();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Un produit à conditionnement unitaire, sans exclusion. */
    private SemoisEligibleItem item(Integer produitId, Integer fpId, long stock, Integer objectif) {
        return projection(produitId, fpId, stock, objectif, objectif, COLIS_UNITAIRE, SANS_MINIMUM, null, null);
    }

    private SemoisEligibleItem projection(
        Integer produitId,
        Integer fpId,
        long stock,
        Integer objectif,
        Integer seuilMini,
        Integer qteColis,
        Integer qteMinimaleCommande,
        LocalDateTime exclusionDate,
        Integer exclusionDureeJours
    ) {
        return new SemoisEligibleItem(
            produitId,
            fpId,
            FOURNISSEUR_ID,
            qteColis,
            qteMinimaleCommande,
            stock,
            objectif,
            seuilMini,
            QTY_APPRO,
            exclusionDate,
            exclusionDureeJours
        );
    }

    /** Une seule page, sans suivante : le batch s'arrête après elle. */
    private void pageDe(SemoisEligibleItem... items) {
        Slice<SemoisEligibleItem> page = new SliceImpl<>(List.of(items), Pageable.unpaged(), false);
        when(produitRepository.findSemoisEligibleItemsSlice(eq(MAGASIN_ID), any())).thenReturn(page);
    }

    private void lignesExistantes(SuggestionLine... lignes) {
        when(suggestionLineRepository.findAllByTypeSuggessionAndFournisseurProduitIdIn(eq(TypeSuggession.AUTO), anySet())).thenReturn(
            List.of(lignes)
        );
    }

    private SuggestionLine ligneExistante(int fpId, boolean quantiteManuelle) {
        FournisseurProduit fp = mock(FournisseurProduit.class);
        when(fp.getId()).thenReturn(fpId);
        SuggestionLine ligne = mock(SuggestionLine.class);
        when(ligne.getFournisseurProduit()).thenReturn(fp);
        lenient().when(ligne.isQuantiteModifieeManuel()).thenReturn(quantiteManuelle);
        return ligne;
    }

    private void aucuneSuggestionExistante() {
        when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(any(), any(), any())).thenReturn(Optional.empty());
    }

    private void suggestionExistante() {
        when(suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(any(), any(), any())).thenReturn(
            Optional.of(mock(Suggestion.class))
        );
    }

    private List<Suggestion> suggestionsEnregistrees() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Suggestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(suggestionRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Suggestion suggestionEnregistree() {
        List<Suggestion> suggestions = suggestionsEnregistrees();
        assertThat(suggestions).hasSize(1);
        return suggestions.getFirst();
    }
}
