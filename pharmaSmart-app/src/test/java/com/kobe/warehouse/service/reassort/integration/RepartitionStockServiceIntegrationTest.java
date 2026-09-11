package com.kobe.warehouse.service.reassort.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.domain.enumeration.TypeRepartition;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reassort.dto.RepartionQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Déplacer du stock entre deux emplacements écrit dans quatre endroits du même geste : les deux
 * {@code stock_produit}, la trace {@code repartition_stock_produit}, le journal
 * {@code inventory_transaction} et la répartition des lots dans {@code lot_stock_location}. Ces
 * écritures doivent rester d'accord entre elles — un stock qui se met à diverger de ses lots ne
 * se rattrape qu'à l'inventaire, et l'ordre FEFO du déplacement ne tient que dans les requêtes de
 * la base.
 */
@DisplayName("RepartitionStockService — répartition de stock sur PostgreSQL")
class RepartitionStockServiceIntegrationTest extends AbstractReassortIntegrationTest {

    // ===== répartition manuelle =====

    @Test
    @DisplayName("Une répartition rayon → réserve déplace stock, lots et journal du même geste")
    void repartitionManuelleVersLaReserve() {
        Produit produit = produit(unique("DOLIPRANE"));
        StockProduit stockRayon = stock(produit, rayon, 40);
        StockProduit stockReserve = stock(produit, reserve, 5);
        Lot proche = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(2), rayon, 10);
        Lot lointain = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusYears(2), rayon, 30);
        viderLeCache();

        services.repartitionStockService.process(List.of(new RepartionQueryDto(stockRayon.getId(), stockReserve.getId(), 15, null, false)));
        viderLeCache();

        assertEquals(25, stockEnBase(produit, rayon));
        assertEquals(20, stockEnBase(produit, reserve));
        assertEquals(25, stockVirtuelEnBase(produit, rayon), "le stock virtuel suit le stock physique");

        assertEquals(0, quantiteSurEmplacement(proche, rayon), "le lot le plus proche de la péremption part le premier");
        assertEquals(10, quantiteSurEmplacement(proche, reserve));
        assertEquals(25, quantiteSurEmplacement(lointain, rayon), "le solde est pris sur le lot suivant");
        assertEquals(5, quantiteSurEmplacement(lointain, reserve));

        RepartitionStockProduit trace = repartitionPour(produit);
        assertEquals(TypeRepartition.MANUEL, trace.getTypeRepartition());
        assertEquals(15, trace.getQtyMvt());
        assertEquals(40, trace.getSourceInitStock());
        assertEquals(25, trace.getSourceFinalStock());
        assertEquals(5, trace.getDestInitStock());
        assertEquals(20, trace.getDestFinalStock());
        assertEquals(utilisateur.getId(), trace.getUser().getId());

        assertEquals(2, mouvementsJournalises(produit), "une sortie sur la source, une entrée sur la destination");
        assertEquals(
            1,
            compter(
                "SELECT COUNT(*) FROM inventory_transaction WHERE produit_id = %d AND storage_id = %d AND quantity = -15".formatted(
                        produit.getId(),
                        STORAGE_RAYON_ID
                    )
            )
        );
        assertEquals(
            1,
            compter(
                "SELECT COUNT(*) FROM inventory_transaction WHERE produit_id = %d AND storage_id = %d AND quantity = 15".formatted(
                        produit.getId(),
                        STORAGE_RESERVE_ID
                    )
            )
        );
    }

    @Test
    @DisplayName("Une répartition réserve → rayon reprend le chemin inverse")
    void repartitionManuelleVersLeRayon() {
        Produit produit = produit(unique("EFFERALGAN"));
        StockProduit stockRayon = stock(produit, rayon, 2);
        StockProduit stockReserve = stock(produit, reserve, 40);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(6), reserve, 40);
        viderLeCache();

        services.repartitionStockService.process(List.of(new RepartionQueryDto(stockReserve.getId(), stockRayon.getId(), 12, null, false)));
        viderLeCache();

        assertEquals(14, stockEnBase(produit, rayon));
        assertEquals(28, stockEnBase(produit, reserve));
        assertEquals(12, quantiteSurEmplacement(lot, rayon));
        assertEquals(28, quantiteSurEmplacement(lot, reserve));
    }

    @Test
    @DisplayName("Sans emplacement de réserve, la création à la volée en ouvre un et l'alimente")
    void creationDeLEmplacementDeReserve() {
        Produit produit = produit(unique("SANS RESERVE"));
        StockProduit stockRayon = stock(produit, rayon, 40);
        viderLeCache();

        services.repartitionStockService.process(List.of(new RepartionQueryDto(stockRayon.getId(), null, 15, null, true)));
        viderLeCache();

        StockProduit reserveCreee = services.stockProduitRepository
            .findStockProduitByStorageIdAndProduitId(STORAGE_RESERVE_ID, produit.getId())
            .orElseThrow();
        assertEquals(15, reserveCreee.getQtyStock());
        assertEquals(StorageType.SAFETY_STOCK, reserveCreee.getStorage().getStorageType());
        assertEquals(25, stockEnBase(produit, rayon));
    }

    @Test
    @DisplayName("Sans emplacement de réserve et sans demande de création, la répartition est refusée")
    void refusFauteDeDestination() {
        Produit produit = produit(unique("SANS RESERVE"));
        StockProduit stockRayon = stock(produit, rayon, 40);
        viderLeCache();

        List<RepartionQueryDto> demandes = List.of(new RepartionQueryDto(stockRayon.getId(), 999_999, 15, null, false));

        GenericError erreur = assertThrows(GenericError.class, () -> services.repartitionStockService.process(demandes));
        assertTrue(erreur.getMessage().contains("Cochez 'Créer la réserve'"));
        assertEquals(40, stockEnBase(produit, rayon), "le stock d'origine est intact");
    }

    @Test
    @DisplayName("Une quantité qui viderait l'emplacement source est passée sans rien écrire")
    void quantiteEgaleAuStockSource() {
        Produit produit = produit(unique("TOUT OU RIEN"));
        StockProduit stockRayon = stock(produit, rayon, 15);
        StockProduit stockReserve = stock(produit, reserve, 0);
        viderLeCache();

        services.repartitionStockService.process(List.of(new RepartionQueryDto(stockRayon.getId(), stockReserve.getId(), 15, null, false)));
        viderLeCache();

        assertEquals(15, stockEnBase(produit, rayon));
        assertEquals(0, stockEnBase(produit, reserve));
        assertEquals(0, mouvementsJournalises(produit));
    }

    @Test
    @DisplayName("Les unités gratuites restent gratuites après un transfert partiel")
    void lesUnitesGratuitesSurvivent() {
        Produit produit = produit(unique("AVEC UG"));
        StockProduit stockRayon = stock(produit, rayon, 20, 5);
        StockProduit stockReserve = stock(produit, reserve, 0);
        viderLeCache();

        services.repartitionStockService.process(List.of(new RepartionQueryDto(stockRayon.getId(), stockReserve.getId(), 10, null, false)));
        viderLeCache();

        assertEquals(10, stockEnBase(produit, rayon), "25 - 10 = 15 au total, dont 5 gratuites");
        assertEquals(15, stockVirtuelEnBase(produit, rayon));
        assertEquals(5, (int) compter("SELECT qty_ug FROM stock_produit WHERE id = " + stockRayon.getId()));
    }

    // ===== validation d'une suggestion =====

    @Test
    @DisplayName("Valider une suggestion rayon puise dans la réserve et clôt la suggestion")
    void validationDUneSuggestionRayon() {
        Produit produit = produit(unique("A RECOMPLETER"));
        StockProduit stockRayon = stock(produit, rayon, 3);
        stock(produit, reserve, 40);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(4), reserve, 40);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RAYON);
        ligne(suggestion, stockRayon, 20);
        viderLeCache();

        services.suggestionReassortService.validateSuggestionReassort(suggestion.getId());
        viderLeCache();

        assertEquals(23, stockEnBase(produit, rayon));
        assertEquals(20, stockEnBase(produit, reserve));
        assertEquals(20, quantiteSurEmplacement(lot, rayon));
        assertEquals(20, quantiteSurEmplacement(lot, reserve));
        assertEquals(2, mouvementsJournalises(produit));

        SuggestionReassort relue = services.suggestionReassortRepository.findById(suggestion.getId()).orElseThrow();
        assertEquals(StatutReassort.CLOSED, relue.getStatut());
        assertEquals(utilisateur.getId(), relue.getLastUserEdit().getId());
    }

    // ===== transferts automatiques =====

    @Test
    @DisplayName("La vente urgente rapatrie la réserve en rayon et trace un mouvement automatique")
    void transfertImplicitePourVenteUrgente() {
        Produit produit = produit(unique("VENTE URGENTE"));
        stock(produit, rayon, 0);
        stock(produit, reserve, 30);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(3), reserve, 30);
        viderLeCache();

        services.repartitionStockService.transfertImpliciteReserveVersRayon(
            produit.getId(),
            STORAGE_RAYON_ID,
            STORAGE_RESERVE_ID,
            6
        );
        viderLeCache();

        assertEquals(6, stockEnBase(produit, rayon));
        assertEquals(24, stockEnBase(produit, reserve));
        assertEquals(6, quantiteSurEmplacement(lot, rayon));
        assertEquals(24, quantiteSurEmplacement(lot, reserve));

        RepartitionStockProduit trace = repartitionPour(produit);
        assertEquals(TypeRepartition.AUTO, trace.getTypeRepartition(), "le mouvement n'est pas le fait d'une saisie");
        assertEquals(STORAGE_RESERVE_ID, trace.getStockProduitSource().getStorage().getId());
        assertEquals(STORAGE_RAYON_ID, trace.getStockProduitDestination().getStorage().getId());
    }

    @Test
    @DisplayName("Le surplus d'une réception descend en réserve en ordre FEFO")
    void versementDuSurplusEnReserve() {
        Produit produit = produit(unique("SURPLUS"));
        StockProduit stockRayon = stock(produit, rayon, 80);
        StockProduit stockReserve = stock(produit, reserve, 5);
        Lot proche = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(1), rayon, 20);
        Lot lointain = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusYears(3), rayon, 60);
        viderLeCache();

        services.repartitionStockService.autoPutawayRayonToReserve(
            services.stockProduitRepository.findById(stockRayon.getId()).orElseThrow(),
            services.stockProduitRepository.findById(stockReserve.getId()).orElseThrow(),
            30
        );
        viderLeCache();

        assertEquals(50, stockEnBase(produit, rayon));
        assertEquals(35, stockEnBase(produit, reserve));
        assertEquals(0, quantiteSurEmplacement(proche, rayon));
        assertEquals(20, quantiteSurEmplacement(proche, reserve));
        assertEquals(50, quantiteSurEmplacement(lointain, rayon));
        assertEquals(10, quantiteSurEmplacement(lointain, reserve));
    }

    @Test
    @DisplayName("Ouvrir un stock de réserve prélève d'autant le rayon")
    void ouvertureDunStockDeReserve() {
        Produit produit = produit(unique("NOUVELLE RESERVE"));
        stock(produit, rayon, 40);
        StockProduit stockReserve = stock(produit, reserve, 12);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(8), rayon, 40);
        viderLeCache();

        services.repartitionStockService.transferStockBetweenStorages(
            services.stockProduitRepository.findById(stockReserve.getId()).orElseThrow()
        );
        viderLeCache();

        assertEquals(28, stockEnBase(produit, rayon));
        assertEquals(12, stockEnBase(produit, reserve), "la réserve porte déjà la quantité saisie");
        assertEquals(28, quantiteSurEmplacement(lot, rayon));
        assertEquals(12, quantiteSurEmplacement(lot, reserve));
    }

    // ===== historique =====

    @Test
    @DisplayName("L'historique filtre par type, emplacement, libellé et période")
    void historiqueFiltre() {
        Produit produit = produit(unique("HISTORIQUE"));
        StockProduit stockRayon = stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 0);
        viderLeCache();

        services.repartitionStockService.process(List.of(new RepartionQueryDto(stockRayon.getId(), stockReserve.getId(), 15, null, false)));
        viderLeCache();

        LocalDate veille = LocalDate.now().minusDays(1);
        LocalDate lendemain = LocalDate.now().plusDays(1);
        Page<RepartitionStockProduitDto> parType = services.repartitionStockService.fetchRepartitionStockProduits(
            new RepartionSearchQueryDto(null, null, null, veille, lendemain, TypeRepartition.MANUEL, stockRayon.getId()),
            PageRequest.of(0, 20)
        );
        assertEquals(1, parType.getTotalElements());
        RepartitionStockProduitDto dto = parType.getContent().getFirst();
        assertEquals(15, dto.getMvtQty());
        assertEquals(60, dto.getSourceInitStock());
        assertEquals(45, dto.getSourceFinalStock());
        assertEquals(0, dto.getDestInitStock());
        assertEquals(15, dto.getDestFinalStock());
        assertEquals(produit.getLibelle(), dto.getProduitName());
        assertEquals(produit.getFournisseurProduitPrincipal().getCodeCip(), dto.getCodeCip());
        assertEquals(STORAGE_RAYON_ID, dto.getStockProduitSrc().getStorageId());
        assertEquals(STORAGE_RESERVE_ID, dto.getStockProduitDest().getStorageId());
        assertNotNull(dto.getCreated());

        assertTrue(
            services.repartitionStockService
                .fetchRepartitionStockProduits(
                    new RepartionSearchQueryDto(null, null, null, veille, lendemain, TypeRepartition.AUTO, stockRayon.getId()),
                    PageRequest.of(0, 20)
                )
                .isEmpty(),
            "le mouvement manuel ne remonte pas sous le filtre automatique"
        );

        assertEquals(
            1,
            services.repartitionStockService
                .fetchRepartitionStockProduits(
                    new RepartionSearchQueryDto(STORAGE_RESERVE_ID, null, produit.getLibelle(), veille, lendemain, null, null),
                    PageRequest.of(0, 20)
                )
                .getTotalElements(),
            "le libellé du produit et l'emplacement de destination sont des filtres valides"
        );

        assertTrue(
            services.repartitionStockService
                .fetchRepartitionStockProduits(
                    new RepartionSearchQueryDto(null, null, null, veille.minusDays(30), veille.minusDays(10), null, null),
                    PageRequest.of(0, 20)
                )
                .isEmpty(),
            "hors période, rien ne remonte"
        );
    }

    @Test
    @DisplayName("L'export reprend l'historique complet et le remet au rapport PDF")
    void exportDeLHistorique() {
        Produit produit = produit(unique("EXPORT"));
        StockProduit stockRayon = stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 0);
        viderLeCache();
        services.repartitionStockService.process(List.of(new RepartionQueryDto(stockRayon.getId(), stockReserve.getId(), 15, null, false)));
        viderLeCache();

        RepartionSearchQueryDto recherche = new RepartionSearchQueryDto(
            null,
            null,
            produit.getLibelle(),
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(1),
            null,
            null
        );
        when(services.repartitionStockPdfReportService.export(anyList(), eq(recherche))).thenReturn(new byte[] { 1, 2, 3 });

        assertEquals(3, services.repartitionStockService.exportRepartitionStockProduits(recherche).length);

        ArgumentCaptor<List<RepartitionStockProduitDto>> capteur = ArgumentCaptor.captor();
        verify(services.repartitionStockPdfReportService).export(capteur.capture(), eq(recherche));
        assertEquals(1, capteur.getValue().size());
        assertEquals(15, capteur.getValue().getFirst().getMvtQty());
    }
}
