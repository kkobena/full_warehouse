package com.kobe.warehouse.service.reassort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.repository.LigneReassortRepository;
import com.kobe.warehouse.repository.SuggestionReassortRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.reassort.dto.LigneReassortDto;
import com.kobe.warehouse.service.reassort.dto.ReassortRecord;
import com.kobe.warehouse.service.reassort.dto.SuggestionReassortDto;
import com.kobe.warehouse.service.reassort.impl.SuggestionReassortServiceImpl;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Une suggestion de réassort est une proposition, pas un mouvement : elle dit ce qu'il faudrait
 * déplacer entre rayon et réserve, et n'est appliquée qu'à sa validation. Deux choses la
 * gouvernent, et ce sont celles qu'on éprouve ici.
 *
 * <p>D'abord <b>quand</b> proposer : un seuil mini absent, un stock déjà suffisant, un produit
 * rangé dans un seul emplacement ou une réserve vide n'ont rien à suggérer. Une suggestion émise
 * à tort revient à demander au préparateur un déplacement inutile ; une suggestion oubliée laisse
 * un rayon se vider alors que la réserve est pleine.
 *
 * <p>Ensuite <b>combien</b> : la quantité proposée est bornée par ce que la source peut céder,
 * jamais par le seul besoin de la destination. Et une même suggestion ouverte sert de réceptacle
 * à toutes les détections du jour — la contrainte d'unicité (suggestion, stock) impose de mettre
 * à jour la ligne existante plutôt que d'en ajouter une seconde.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuggestionReassortService — détection des besoins de réassort")
class SuggestionReassortServiceImplTest {

    private static final int MAGASIN_ID = 1;
    private static final int RAYON_ID = 1;
    private static final int RESERVE_ID = 3;
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    @Mock
    private SuggestionReassortRepository suggestionReassortRepository;

    @Mock
    private LigneReassortRepository ligneReassortRepository;

    @Mock
    private RepartitionStockService repartitionStockService;

    @Mock
    private StorageService storageService;

    @Mock
    private ReferenceService referenceService;

    private SuggestionReassortService service;

    private Storage rayon;
    private Storage reserve;
    private AppUser operateur;
    private Magasin magasin;

    @BeforeEach
    void init() {
        service = new SuggestionReassortServiceImpl(
            suggestionReassortRepository,
            ligneReassortRepository,
            repartitionStockService,
            storageService,
            referenceService
        );
        magasin = new Magasin();
        magasin.setId(MAGASIN_ID);
        rayon = storage(RAYON_ID, "Stock rayon", StorageType.PRINCIPAL);
        reserve = storage(RESERVE_ID, "Stock réserve", StorageType.SAFETY_STOCK);
        operateur = new AppUser();
        operateur.setId(1);
        operateur.setFirstName("Awa");
        operateur.setLastName("Kone");
        operateur.setMagasin(magasin);
    }

    @Nested
    @DisplayName("Détection à la réception d'une commande")
    class DetectionALaReception {

        @Test
        @DisplayName("Sans ligne à examiner, rien n'est ouvert")
        void listeVide() {
            service.createLigneReassort(List.of());
            service.createLigneReassort(null, operateur);

            verifyNoInteractions(suggestionReassortRepository, referenceService, ligneReassortRepository);
        }

        @Test
        @DisplayName("Un stock de réserve sans seuil mini ou déjà au-dessus du seuil ne suggère rien")
        void riensASuggerer() {
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.empty());
            when(referenceService.buildNumReassort()).thenReturn("RE-1");

            StockProduit sansSeuil = stock(produit(10), reserve, 0);
            StockProduit auDessusDuSeuil = stock(produit(11), reserve, 12);
            auDessusDuSeuil.setSeuilMini(10);

            service.createLigneReassort(
                List.of(new ReassortRecord(sansSeuil, 50), new ReassortRecord(auDessusDuSeuil, 50)),
                operateur
            );

            assertTrue(capturerLaSuggestion().getLigneReassorts().isEmpty());
        }

        @Test
        @DisplayName("La quantité proposée comble l'écart au seuil, plafonné par ce que la réception apporte")
        void quantitePlafonneeParLaReception() {
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.empty());
            when(referenceService.buildNumReassort()).thenReturn("RE-1");

            StockProduit besoinCouvert = stock(produit(10), reserve, 4);
            besoinCouvert.setSeuilMini(20);
            StockProduit besoinPlafonne = stock(produit(11), reserve, 4);
            besoinPlafonne.setSeuilMini(20);

            service.createLigneReassort(
                List.of(new ReassortRecord(besoinCouvert, 50), new ReassortRecord(besoinPlafonne, 6)),
                operateur
            );

            SuggestionReassort suggestion = capturerLaSuggestion();
            assertEquals(MAGASIN_ID, suggestion.getMagasin().getId());
            assertEquals(TypeReassort.RESERVE, suggestion.getTypeReassort());
            assertEquals("RE-1", suggestion.getReference());
            assertSame(operateur, suggestion.getLastUserEdit());

            assertEquals(16, quantiteProposeePour(suggestion, besoinCouvert), "20 - 4, la réception suffit");
            assertEquals(6, quantiteProposeePour(suggestion, besoinPlafonne), "on ne propose pas plus que reçu");
            verify(ligneReassortRepository, never()).save(any(LigneReassort.class));
        }

        @Test
        @DisplayName("Une suggestion déjà ouverte accueille la nouvelle ligne, qui est enregistrée séparément")
        void suggestionOuverteReutilisee() {
            SuggestionReassort ouverte = suggestion(7, TypeReassort.RESERVE, StatutReassort.OPEN);
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.of(ouverte));

            StockProduit stockReserve = stock(produit(10), reserve, 4);
            stockReserve.setSeuilMini(20);

            service.createLigneReassort(List.of(new ReassortRecord(stockReserve, 50)), operateur);

            assertEquals(1, ouverte.getLigneReassorts().size());
            verify(ligneReassortRepository).save(any(LigneReassort.class));
            verify(referenceService, never()).buildNumReassort();
        }

        @Test
        @DisplayName("Sans utilisateur fourni, la détection retombe sur l'opérateur connecté")
        void utilisateurConnecteParDefaut() {
            when(storageService.getUser()).thenReturn(operateur);
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.empty());
            when(referenceService.buildNumReassort()).thenReturn("RE-1");

            StockProduit stockReserve = stock(produit(10), reserve, 4);
            stockReserve.setSeuilMini(20);

            service.createLigneReassort(List.of(new ReassortRecord(stockReserve, 50)));

            assertSame(operateur, capturerLaSuggestion().getLastUserEdit());
        }
    }

    @Nested
    @DisplayName("Détection d'un rayon à recompléter")
    class DetectionRayon {

        @Test
        @DisplayName("Rien à proposer sans quantité de réassort paramétrée")
        void sansQuantiteDeReassort() {
            StockProduit stockRayon = stock(produit(10), rayon, 2);
            stockRayon.setStockReassort(0);

            service.createRayonSuggestionReassort(stockRayon);

            verifyNoInteractions(suggestionReassortRepository, storageService);
        }

        @Test
        @DisplayName("Un produit rangé dans un seul emplacement n'a rien d'où puiser")
        void produitDansUnSeulEmplacement() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(produit, rayon, 2);
            stockRayon.setStockReassort(10);

            service.createRayonSuggestionReassort(stockRayon);

            verifyNoInteractions(suggestionReassortRepository);
        }

        @Test
        @DisplayName("Un emplacement non vendable ne se recomplète pas depuis la réserve")
        void emplacementNonVendable() {
            Produit produit = produit(10);
            StockProduit quarantaine = stock(produit, storage(9, "Quarantaine", StorageType.QUARANTAINE), 2);
            quarantaine.setStockReassort(10);
            stock(produit, reserve, 40);

            service.createRayonSuggestionReassort(quarantaine);

            verifyNoInteractions(suggestionReassortRepository);
        }

        @Test
        @DisplayName("Une réserve vide ne peut rien céder")
        void reserveVide() {
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            Produit produit = produit(10);
            StockProduit stockRayon = stock(produit, rayon, 2);
            stockRayon.setStockReassort(10);
            stock(produit, reserve, 0);

            service.createRayonSuggestionReassort(stockRayon);

            verifyNoInteractions(suggestionReassortRepository);
        }

        @Test
        @DisplayName("La quantité proposée est celle du paramétrage, bornée par le stock de réserve")
        void quantiteBorneeParLaReserve() {
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            when(storageService.getUser()).thenReturn(operateur);
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RAYON))
                .thenReturn(Optional.empty());
            when(referenceService.buildNumReassort()).thenReturn("RE-9");

            Produit produit = produit(10);
            StockProduit stockRayon = stock(produit, rayon, 2);
            stockRayon.setStockReassort(30);
            stock(produit, reserve, 12);

            service.createRayonSuggestionReassort(stockRayon);

            SuggestionReassort suggestion = capturerLaSuggestion();
            assertEquals(TypeReassort.RAYON, suggestion.getTypeReassort());
            assertEquals(12, quantiteProposeePour(suggestion, stockRayon), "la réserve ne contient que 12");
        }
    }

    @Nested
    @DisplayName("Détection d'une réserve à recompléter")
    class DetectionReserve {

        @Test
        @DisplayName("Un rayon sous son seuil mini garde son stock pour lui")
        void rayonSousSonSeuil() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(produit, rayon, 8);
            stockRayon.setSeuilMini(10);
            stockRayon.setStockMaxi(5);

            service.createReserveSuggestionReassort(stockRayon);

            verifyNoInteractions(suggestionReassortRepository, storageService);
        }

        @Test
        @DisplayName("Sans stock maxi, ou tant qu'il n'est pas dépassé, il n'y a pas de surplus")
        void pasDeSurplus() {
            Produit sansMaxi = produit(10);
            StockProduit rayonSansMaxi = stock(sansMaxi, rayon, 80);
            stock(sansMaxi, reserve, 0);

            Produit sousLeMaxi = produit(11);
            StockProduit rayonSousLeMaxi = stock(sousLeMaxi, rayon, 40);
            rayonSousLeMaxi.setStockMaxi(60);
            stock(sousLeMaxi, reserve, 0);

            service.createReserveSuggestionReassort(rayonSansMaxi);
            service.createReserveSuggestionReassort(rayonSousLeMaxi);

            verifyNoInteractions(suggestionReassortRepository);
        }

        @Test
        @DisplayName("Une réserve déjà au-dessus de son seuil n'a pas besoin du surplus")
        void reserveDejaSuffisante() {
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            Produit produit = produit(10);
            StockProduit stockRayon = stock(produit, rayon, 80);
            stockRayon.setStockMaxi(50);
            StockProduit stockReserve = stock(produit, reserve, 30);
            stockReserve.setSeuilMini(20);

            service.createReserveSuggestionReassort(stockRayon);

            verifyNoInteractions(suggestionReassortRepository);
        }

        @Test
        @DisplayName("Le surplus au-delà du stock maxi du rayon est proposé à la réserve")
        void surplusProposeALaReserve() {
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            when(storageService.getUser()).thenReturn(operateur);
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.empty());
            when(referenceService.buildNumReassort()).thenReturn("RE-3");

            Produit produit = produit(10);
            StockProduit stockRayon = stock(produit, rayon, 80);
            stockRayon.setStockMaxi(50);
            StockProduit stockReserve = stock(produit, reserve, 2);
            stockReserve.setSeuilMini(20);

            service.createReserveSuggestionReassort(stockRayon);

            SuggestionReassort suggestion = capturerLaSuggestion();
            assertEquals(TypeReassort.RESERVE, suggestion.getTypeReassort());
            assertEquals(30, quantiteProposeePour(suggestion, stockReserve), "80 - 50, la ligne porte le stock réserve");
        }
    }

    @Nested
    @DisplayName("Cycle de vie d'une suggestion")
    class CycleDeVie {

        @Test
        @DisplayName("Valider une suggestion ouverte applique la répartition puis la clôt")
        void validationDUneSuggestionOuverte() {
            when(storageService.getUser()).thenReturn(operateur);
            SuggestionReassort ouverte = suggestion(7, TypeReassort.RAYON, StatutReassort.OPEN);
            when(suggestionReassortRepository.getReferenceById(7)).thenReturn(ouverte);

            service.validateSuggestionReassort(7);

            verify(repartitionStockService).process(ouverte);
            assertEquals(StatutReassort.CLOSED, ouverte.getStatut());
            assertSame(operateur, ouverte.getLastUserEdit(), "la répartition lit l'auteur sur la suggestion");
            verify(suggestionReassortRepository).save(ouverte);
        }

        @Test
        @DisplayName("Une suggestion déjà close n'est pas rejouée")
        void validationDUneSuggestionClose() {
            SuggestionReassort close = suggestion(7, TypeReassort.RAYON, StatutReassort.CLOSED);
            when(suggestionReassortRepository.getReferenceById(7)).thenReturn(close);

            service.validateSuggestionReassort(7);

            verifyNoInteractions(repartitionStockService);
            verify(suggestionReassortRepository, never()).save(any());
        }

        @Test
        @DisplayName("Corriger la quantité d'une ligne l'horodate")
        void correctionDuneLigne() {
            LigneReassort ligne = new LigneReassort();
            ligne.setId(4);
            ligne.setQuantity(10);
            when(ligneReassortRepository.getReferenceById(4)).thenReturn(ligne);

            service.updateLigneReassort(4, 25);

            assertEquals(25, ligne.getQuantity());
            assertNotNull(ligne.getUpdatedAt(), "la correction est horodatée");
            verify(ligneReassortRepository).save(ligne);
        }

        @Test
        @DisplayName("Supprimer une ligne ou une suggestion passe directement au repository")
        void suppressions() {
            service.deleteLigneReassort(4);
            service.deleteSuggestionReassort(7);

            verify(ligneReassortRepository).deleteById(4);
            verify(suggestionReassortRepository).deleteById(7);
        }
    }

    @Nested
    @DisplayName("Consultation des suggestions ouvertes")
    class Consultation {

        @Test
        @DisplayName("Sans filtre de type, toutes les suggestions ouvertes du magasin remontent")
        void sansFiltreDeType() {
            when(storageService.getUser()).thenReturn(operateur);
            when(suggestionReassortRepository.findAllByStatutAndMagasinId(StatutReassort.OPEN, MAGASIN_ID))
                .thenReturn(List.of(suggestion(7, TypeReassort.RAYON, StatutReassort.OPEN)));

            assertEquals(1, service.getOpenningSuggestions(null).size());
            verify(suggestionReassortRepository, never())
                .findAllByStatutAndMagasinIdAndTypeReassort(any(), any(), any());
        }

        @Test
        @DisplayName("Chaque ligne rend lisible le produit, son emplacement et l'écart au seuil")
        void contenuDuneLigne() {
            when(storageService.getUser()).thenReturn(operateur);
            SuggestionReassort ouverte = suggestion(7, TypeReassort.RESERVE, StatutReassort.OPEN);
            Produit produit = produitAvecFournisseur(10, "DOLIPRANE 500", "CIP-10", "EAN-10");
            StockProduit stockReserve = stock(produit, reserve, 4);
            stockReserve.setSeuilMini(20);
            ouverte.getLigneReassorts().add(ligne(stockReserve, 16));
            when(
                suggestionReassortRepository.findAllByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN,
                    MAGASIN_ID,
                    TypeReassort.RESERVE
                )
            ).thenReturn(List.of(ouverte));

            SuggestionReassortDto dto = service.getOpenningSuggestions(TypeReassort.RESERVE).getFirst();

            assertEquals(7, dto.getId());
            assertEquals(StatutReassort.OPEN, dto.getStatut());
            assertEquals(TypeReassort.RESERVE, dto.getTypeReassort());
            assertEquals("K.Awa", dto.getUserFullName());

            LigneReassortDto ligne = dto.getLigneReassorts().getFirst();
            assertEquals(16, ligne.getQuantity());
            assertEquals(stockReserve.getId(), ligne.getStockProduitId());
            assertEquals("Stock réserve", ligne.getStorageName());
            assertEquals("DOLIPRANE 500", ligne.getProduitLibelle());
            assertEquals("CIP-10", ligne.getProduitCode());
            assertEquals("EAN-10", ligne.getCodeEanFabricant());
            assertEquals(20, ligne.getSeuilMini());
            assertEquals(4, ligne.getStockActuel());
            assertEquals(4, ligne.getStockAvailable());
        }
    }

    /**
     * À la réception, le surplus détecté n'attend pas la validation d'un préparateur : il descend
     * seul en réserve. Ce qui se joue alors est le nettoyage — une ligne exécutée doit quitter la
     * suggestion, sans quoi l'écran la reproposerait, et une suggestion vidée doit se clore.
     */
    @Nested
    @DisplayName("Exécution automatique du surplus de réception")
    class ExecutionAutomatique {

        @Test
        @DisplayName("Sans produit reçu, ou sans suggestion ouverte, il n'y a rien à exécuter")
        void rienAExecuter() {
            service.autoExecuteOverflowForProducts(Set.of());

            when(storageService.getUser()).thenReturn(operateur);
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.empty());

            service.autoExecuteOverflowForProducts(Set.of(10));

            verifyNoInteractions(repartitionStockService);
            verify(suggestionReassortRepository, never()).save(any());
        }

        @Test
        @DisplayName("Seules les lignes des produits reçus sont exécutées, et la suggestion reste ouverte")
        void seulesLesLignesDesProduitsRecus() {
            preparerLeContexte();
            SuggestionReassort ouverte = suggestion(7, TypeReassort.RESERVE, StatutReassort.OPEN);

            Produit recu = produit(10);
            StockProduit rayonRecu = stock(recu, rayon, 80);
            StockProduit reserveRecu = stock(recu, reserve, 2);
            ouverte.getLigneReassorts().add(ligne(reserveRecu, 30));

            Produit absent = produit(11);
            stock(absent, rayon, 50);
            StockProduit reserveAbsent = stock(absent, reserve, 1);
            ouverte.getLigneReassorts().add(ligne(reserveAbsent, 5));

            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.of(ouverte));

            service.autoExecuteOverflowForProducts(Set.of(10));

            verify(repartitionStockService).autoPutawayRayonToReserve(rayonRecu, reserveRecu, 30);
            verify(repartitionStockService, times(1)).autoPutawayRayonToReserve(any(), any(), anyInt());
            assertEquals(1, ouverte.getLigneReassorts().size(), "la ligne exécutée quitte la suggestion");
            assertEquals(StatutReassort.OPEN, ouverte.getStatut(), "il reste du travail à faire");
            verify(suggestionReassortRepository).save(ouverte);
        }

        @Test
        @DisplayName("Une suggestion entièrement exécutée se clôt d'elle-même")
        void suggestionVideeEstClose() {
            preparerLeContexte();
            SuggestionReassort ouverte = suggestion(7, TypeReassort.RESERVE, StatutReassort.OPEN);
            Produit recu = produit(10);
            StockProduit rayonRecu = stock(recu, rayon, 80);
            StockProduit reserveRecu = stock(recu, reserve, 2);
            LigneReassort ligne = ligne(reserveRecu, 30);
            ouverte.getLigneReassorts().add(ligne);
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.of(ouverte));

            service.autoExecuteOverflowForProducts(Set.of(10));

            verify(repartitionStockService).autoPutawayRayonToReserve(rayonRecu, reserveRecu, 30);
            verify(ligneReassortRepository).delete(ligne);
            assertTrue(ouverte.getLigneReassorts().isEmpty());
            assertEquals(StatutReassort.CLOSED, ouverte.getStatut());
        }

        @Test
        @DisplayName("Un produit sans emplacement rayon est passé, sa ligne reste à traiter à la main")
        void produitSansEmplacementRayon() {
            preparerLeContexte();
            SuggestionReassort ouverte = suggestion(7, TypeReassort.RESERVE, StatutReassort.OPEN);
            Produit produit = produit(10);
            StockProduit stockReserve = stock(produit, reserve, 2);
            ouverte.getLigneReassorts().add(ligne(stockReserve, 30));
            when(suggestionReassortRepository.findOneByStatutAndMagasinIdAndTypeReassort(
                    StatutReassort.OPEN, MAGASIN_ID, TypeReassort.RESERVE))
                .thenReturn(Optional.of(ouverte));

            service.autoExecuteOverflowForProducts(Set.of(10));

            verifyNoInteractions(repartitionStockService);
            assertEquals(1, ouverte.getLigneReassorts().size());
            assertEquals(StatutReassort.OPEN, ouverte.getStatut());
        }

        private void preparerLeContexte() {
            when(storageService.getUser()).thenReturn(operateur);
            when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        }
    }

    // ===== fabriques du jeu d'essai =====

    private SuggestionReassort capturerLaSuggestion() {
        ArgumentCaptor<SuggestionReassort> capteur = ArgumentCaptor.forClass(SuggestionReassort.class);
        verify(suggestionReassortRepository).save(capteur.capture());
        return capteur.getValue();
    }

    private static int quantiteProposeePour(SuggestionReassort suggestion, StockProduit stockProduit) {
        return suggestion
            .getLigneReassorts()
            .stream()
            .filter(ligne -> ligne.getStockProduit().getId().equals(stockProduit.getId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("aucune ligne proposée pour le stock " + stockProduit.getId()))
            .getQuantity();
    }

    private static Storage storage(int id, String nom, StorageType type) {
        Storage storage = new Storage();
        storage.setId(id);
        storage.setName(nom);
        storage.setStorageType(type);
        return storage;
    }

    private static Produit produit(int id) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setLibelle("PRODUIT " + id);
        produit.setStockProduits(new HashSet<>());
        return produit;
    }

    private static Produit produitAvecFournisseur(int id, String libelle, String codeCip, String codeEan) {
        Produit produit = produit(id);
        produit.setLibelle(libelle);
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setCodeCip(codeCip);
        fournisseurProduit.setCodeEan(codeEan);
        fournisseurProduit.setFournisseur(new Fournisseur());
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        return produit;
    }

    private static StockProduit stock(Produit produit, Storage storage, int quantite) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setId(COMPTEUR.incrementAndGet());
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite);
        stockProduit.setQtyUG(0);
        produit.getStockProduits().add(stockProduit);
        return stockProduit;
    }

    private SuggestionReassort suggestion(int id, TypeReassort type, StatutReassort statut) {
        SuggestionReassort suggestion = new SuggestionReassort();
        suggestion.setId(id);
        suggestion.setTypeReassort(type);
        suggestion.setStatut(statut);
        suggestion.setMagasin(magasin);
        suggestion.setLastUserEdit(operateur);
        suggestion.setCreatedAt(LocalDateTime.now());
        suggestion.setUpdatedAt(LocalDateTime.now());
        return suggestion;
    }

    /** Comme pour {@code StockProduit}, l'id distingue deux lignes dans le {@code Set}. */
    private static LigneReassort ligne(StockProduit stockProduit, int quantite) {
        LigneReassort ligne = new LigneReassort();
        ligne.setId(COMPTEUR.incrementAndGet());
        ligne.setStockProduit(stockProduit);
        ligne.setQuantity(quantite);
        ligne.setUpdatedAt(LocalDateTime.now());
        return ligne;
    }
}
