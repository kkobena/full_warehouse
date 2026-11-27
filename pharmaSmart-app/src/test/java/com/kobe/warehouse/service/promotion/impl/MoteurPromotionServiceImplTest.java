package com.kobe.warehouse.service.promotion.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.PromotionProduit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.repository.PromotionRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Moteur de promotions")
class MoteurPromotionServiceImplTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 11, 26);

    @Mock
    private PromotionRepository promotionRepository;

    private MoteurPromotionServiceImpl moteur;
    private Produit produit;
    private CashSale vente;
    private SalesLine ligne;

    @BeforeEach
    void setUp() {
        moteur = new MoteurPromotionServiceImpl(promotionRepository);
        produit = new Produit();
        produit.setId(10);
        Magasin magasin = new Magasin();
        magasin.setId(1);
        vente = new CashSale();
        vente.setMagasin(magasin);
        vente.setSaleDate(JOUR);
        ligne = ligne(produit, 1_000, 3);
        vente.setSalesLines(new HashSet<>(Set.of(ligne)));
    }

    private static SalesLine ligne(Produit produit, int prix, int quantite) {
        SalesLine ligne = new SalesLine();
        ligne.setProduit(produit);
        ligne.setRegularUnitPrice(prix);
        ligne.setQuantityRequested(quantite);
        ligne.setSalesAmount(prix * quantite);
        ligne.setDiscountAmount(0);
        return ligne;
    }

    private Promotion promo(long id, int taux, int priorite) {
        Promotion promotion = new Promotion()
                .setId(id)
                .setLibelle("Promo " + id)
                .setType(TypePromotion.PROMO_POURCENTAGE)
                .setValeur(taux)
                .setPriorite(priorite)
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(JOUR.minusDays(1))
                .setDateFin(JOUR.plusDays(1));
        promotion.getProduits().add(new PromotionProduit(produit, null));
        return promotion;
    }

    private void enVigueur(Promotion... promotions) {
        when(promotionRepository.findEnVigueur(1, JOUR, StatutPromotion.VALIDEE))
                .thenReturn(List.of(promotions));
    }

    @Test
    void posteLaPromotionAvecSonTauxPourLaFne() {
        enVigueur(promo(5, 10, 0));

        assertTrue(moteur.appliquer(vente));

        assertEquals(300, ligne.getDiscountAmount());
        assertEquals(0.10f, ligne.getTauxRemise(), 1e-6);
        assertEquals(5L, ligne.getPromotionId());
        assertEquals("Promo 5", ligne.getPromotionLibelle());
        assertEquals(3, ligne.getQuantitePromo());
    }

    @Test
    void retientLaPlusAvantageusePuisLaPrioritePuisLIdentifiant() {
        enVigueur(promo(1, 10, 9), promo(2, 20, 0));
        moteur.appliquer(vente);
        assertEquals(2L, ligne.getPromotionId());

        moteur.effacer(vente);
        enVigueur(promo(3, 10, 0), promo(4, 10, 5));
        moteur.appliquer(vente);
        assertEquals(4L, ligne.getPromotionId());

        moteur.effacer(vente);
        enVigueur(promo(8, 10, 0), promo(7, 10, 0));
        moteur.appliquer(vente);
        assertEquals(7L, ligne.getPromotionId());
    }

    @Test
    void lisLeMagasinEtLaDateDeLaVente() {
        vente.setSaleDate(JOUR.minusDays(3));
        when(promotionRepository.findEnVigueur(1, JOUR.minusDays(3), StatutPromotion.VALIDEE))
                .thenReturn(List.of());

        assertFalse(moteur.appliquer(vente));
        verify(promotionRepository).findEnVigueur(1, JOUR.minusDays(3), StatutPromotion.VALIDEE);
    }

    @Test
    void nEcrasePasUneRemiseHumaine() {
        ligne.setDiscountAmount(50);
        ligne.setTauxRemise(0.05f);
        enVigueur(promo(5, 10, 0));

        assertFalse(moteur.appliquer(vente));
        assertEquals(50, ligne.getDiscountAmount());
        assertNull(ligne.getPromotionId());
    }

    @Test
    void uneCumulableSAjouteALaRemiseDuCaissier() {
        // Remise produit de 150 sur 3 × 1 000 : la promotion cumulable porte sur les 2 850 restants.
        ligne.setDiscountAmount(150);
        ligne.setTauxRemise(0.05f);
        enVigueur(promo(5, 10, 0).setCumulable(true));

        assertTrue(moteur.appliquer(vente));

        assertEquals(435, ligne.getDiscountAmount());
        assertEquals(0.145f, ligne.getTauxRemise(), 1e-6);
        assertEquals(5L, ligne.getPromotionId());
        // Le journal des remises sépare les deux : 285 de promotion, 150 de remise produit.
        assertEquals(285, ligne.getRemisePromotion());
        assertEquals(150, ligne.getRemiseProduit());
    }

    @Test
    void effacerNeRetireQueCeQueLeMoteurAPose() {
        SalesLine humaine = ligne(new Produit(), 500, 1);
        humaine.setDiscountAmount(25);
        humaine.setTauxRemise(0.05f);
        vente.getSalesLines().add(humaine);
        enVigueur(promo(5, 10, 0));
        moteur.appliquer(vente);

        moteur.effacer(vente);

        assertEquals(0, ligne.getDiscountAmount());
        assertEquals(0f, ligne.getTauxRemise());
        assertNull(ligne.getPromotionId());
        assertNull(ligne.getPromotionLibelle());
        assertNull(ligne.getQuantitePromo());
        assertEquals(0, ligne.getRemisePromotion());
        assertEquals(25, humaine.getDiscountAmount());
        assertEquals(0, humaine.getRemisePromotion());
        assertEquals(25, humaine.getRemiseProduit());
    }

    @Test
    void reappliquerDonneLeMemeResultat() {
        enVigueur(promo(5, 10, 0));
        moteur.appliquer(vente);
        moteur.effacer(vente);
        moteur.appliquer(vente);
        assertEquals(300, ligne.getDiscountAmount());
    }

    private void dejaRemisees(long promotionId, long unites) {
        PromotionRepository.UnitesRemisees vendues = new PromotionRepository.UnitesRemisees() {
            @Override
            public Long getPromotionId() {
                return promotionId;
            }

            @Override
            public Long getUnites() {
                return unites;
            }
        };
        when(promotionRepository.sommerUnitesRemisees(List.of(promotionId))).thenReturn(List.of(vendues));
    }

    @Test
    void lePlafondTotalNeRemiseQueLesUnitesQuIlReste() {
        // 1 000 F × 3, -10 % ; plafond de 50 unités dont 48 déjà vendues : 2 unités remisées sur 3.
        Promotion promotion = promo(5, 10, 0).setQuantiteMaxTotale(50);
        enVigueur(promotion);
        dejaRemisees(5L, 48);

        assertTrue(moteur.appliquer(vente));

        assertEquals(200, ligne.getDiscountAmount());
        assertEquals(2, ligne.getQuantitePromo());
        assertEquals(5L, ligne.getPromotionId());
    }

    @Test
    void unePromotionEpuiseeNeSePosePlusEtCedeLaPlaceAUneAutre() {
        Promotion epuisee = promo(5, 30, 0).setQuantiteMaxTotale(50);
        enVigueur(epuisee, promo(6, 10, 0));
        dejaRemisees(5L, 50);

        assertTrue(moteur.appliquer(vente));

        assertEquals(6L, ligne.getPromotionId());
        assertEquals(300, ligne.getDiscountAmount());
    }

    @Test
    void lesPlafondsSePartagentEntreLesLignesDuTicket() {
        Produit autre = new Produit();
        autre.setId(11);
        ligne.setId(1L);
        SalesLine seconde = ligne(autre, 1_000, 3);
        seconde.setId(2L);
        vente.getSalesLines().add(seconde);
        // Deux par ticket au plus : la première ligne saisie les prend, la seconde reste au prix.
        Promotion promotion = promo(5, 10, 0).setQuantiteMaxParVente(2);
        promotion.getProduits().add(new PromotionProduit(autre, null));
        enVigueur(promotion);

        moteur.appliquer(vente);

        assertEquals(200, ligne.getDiscountAmount());
        assertEquals(2, ligne.getQuantitePromo());
        assertEquals(0, seconde.getDiscountAmount());
        assertNull(seconde.getPromotionId());
    }

    @Test
    void sansPlafondTotalLeCompteurNEstPasLu() {
        enVigueur(promo(5, 10, 0));

        moteur.appliquer(vente);

        verify(promotionRepository, never()).sommerUnitesRemisees(any());
    }

    @Test
    void ignoreLesLignesRembourseesDUneVenteAssurance() {
        // Sur une vente assurance, seule une ligne non remboursée se promeut : une ligne couverte, jamais.
        ThirdPartySales assurance = new ThirdPartySales();
        assurance.setNatureVente(com.kobe.warehouse.domain.enumeration.NatureVente.ASSURANCE);
        assurance.setMagasin(vente.getMagasin());
        assurance.setSalesLines(new HashSet<>(Set.of(ligne(produit, 1_000, 1))));
        assertFalse(moteur.appliquer(assurance));
    }

    // ── Offre N+M ────────────────────────────────────────────────────────────────────────────

    private Promotion offre(long id, int achetees, int offertes, Produit... produits) {
        Promotion promotion = new Promotion()
                .setId(id)
                .setLibelle("Offre " + id)
                .setType(TypePromotion.PROMO_OFFRE_NM)
                .setValeur(offertes)
                .setQuantiteMin(achetees)
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(JOUR.minusDays(1))
                .setDateFin(JOUR.plusDays(1));
        for (Produit p : produits) {
            promotion.getProduits().add(new PromotionProduit(p, null));
        }
        return promotion;
    }

    @Test
    void offreNMSurUneLigne() {
        // 3 unités à 1 000 en 2+1 : une offerte.
        enVigueur(offre(6, 2, 1, produit));

        assertTrue(moteur.appliquer(vente));

        assertEquals(1_000, ligne.getDiscountAmount());
        assertEquals(1_000f / 3_000, ligne.getTauxRemise(), 1e-6);
        assertEquals(6L, ligne.getPromotionId());
        assertEquals(3, ligne.getQuantitePromo());
        // L'unité offerte est toute de la promotion.
        assertEquals(1_000, ligne.getRemisePromotion());
        assertEquals(0, ligne.getRemiseProduit());
        // Les unités gratuites du fournisseur ne sont pas touchées (§7.5).
        assertEquals(0, ligne.getQuantityUg());
    }

    @Test
    void offreNMMeleDeuxArticlesEtRepartitLaRemise() {
        Produit autre = new Produit();
        autre.setId(11);
        ligne.setQuantityRequested(2);
        ligne.setSalesAmount(2_000);
        ligne.setId(1L);
        SalesLine seconde = ligne(autre, 400, 1);
        seconde.setId(2L);
        vente.getSalesLines().add(seconde);
        enVigueur(offre(6, 2, 1, produit, autre));

        moteur.appliquer(vente);

        // L'article à 400 est offert ; 400 F répartis au prorata de 2 000 et 400.
        assertEquals(333, ligne.getDiscountAmount());
        assertEquals(67, seconde.getDiscountAmount());
        assertEquals(333, ligne.getRemisePromotion());
        assertEquals(67, seconde.getRemisePromotion());
        assertEquals(6L, seconde.getPromotionId());
    }

    @Test
    void offreNMIncompleteLaisseLaPlaceAuxPromotionsDeLigne() {
        ligne.setQuantityRequested(2);
        ligne.setSalesAmount(2_000);
        enVigueur(offre(6, 2, 1, produit), promo(5, 10, 0));

        moteur.appliquer(vente);

        assertEquals(5L, ligne.getPromotionId());
        assertEquals(200, ligne.getDiscountAmount());
    }

    @Test
    void laPromotionDeLigneLEmporteQuandElleRemiseDavantage() {
        // 3 unités à 1 000 : 2+1 remise 1 000, -50 % remise 1 500.
        enVigueur(offre(6, 2, 1, produit), promo(5, 50, 0));

        moteur.appliquer(vente);

        assertEquals(5L, ligne.getPromotionId());
        assertEquals(1_500, ligne.getDiscountAmount());
    }

    @Test
    void offreNMRespecteLePlafondParTicketEnGroupesComplets() {
        ligne.setQuantityRequested(9);
        ligne.setSalesAmount(9_000);
        Promotion offre = offre(6, 2, 1, produit).setQuantiteMaxParVente(5);
        enVigueur(offre);

        moteur.appliquer(vente);

        assertEquals(1_000, ligne.getDiscountAmount());
        assertEquals(3, ligne.getQuantitePromo());
    }

    // ── Pack ─────────────────────────────────────────────────────────────────────────────────

    @Test
    void unPackCompletDansLePanierEstRemiseSurSesLignes() {
        Produit apres = new Produit();
        apres.setId(11);
        ligne.setQuantityRequested(1);
        ligne.setSalesAmount(1_000);
        ligne.setId(1L);
        SalesLine seconde = ligne(apres, 500, 1);
        seconde.setId(2L);
        vente.getSalesLines().add(seconde);
        Promotion pack = new Promotion()
                .setId(9L)
                .setLibelle("Duo")
                .setType(TypePromotion.PROMO_PACK)
                .setValeur(1_200)
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(JOUR.minusDays(1))
                .setDateFin(JOUR.plusDays(1));
        pack.getProduits().add(new PromotionProduit(produit, null, 1));
        pack.getProduits().add(new PromotionProduit(apres, null, 1));
        enVigueur(pack, promo(5, 10, 0));

        assertTrue(moteur.appliquer(vente));

        // 1 500 au lieu de 1 200 : 300 F au prorata de 1 000 et 500. Le pack (300) bat les -10 % (100).
        assertEquals(9L, ligne.getPromotionId());
        assertEquals(200, ligne.getDiscountAmount());
        assertEquals(100, seconde.getDiscountAmount());
        assertEquals(9L, seconde.getPromotionId());
        assertEquals(1, seconde.getQuantitePromo());
        assertEquals(200, ligne.getRemisePromotion());
        assertEquals(100, seconde.getRemisePromotion());
    }

    @Test
    void unPackIncompletLaisseLaLigneASaPromotion() {
        Produit apres = new Produit();
        apres.setId(11);
        Promotion pack = new Promotion()
                .setId(9L)
                .setLibelle("Duo")
                .setType(TypePromotion.PROMO_PACK)
                .setValeur(1_200)
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(JOUR.minusDays(1))
                .setDateFin(JOUR.plusDays(1));
        pack.getProduits().add(new PromotionProduit(produit, null, 1));
        pack.getProduits().add(new PromotionProduit(apres, null, 1));
        enVigueur(pack, promo(5, 10, 0));

        moteur.appliquer(vente);

        assertEquals(5L, ligne.getPromotionId());
        assertEquals(300, ligne.getDiscountAmount());
    }

    private Promotion seuil(long id, int taux, int montant, Produit... produits) {
        Promotion promotion = new Promotion()
                .setId(id)
                .setLibelle("Dès " + montant)
                .setType(TypePromotion.PROMO_SEUIL_MONTANT)
                .setValeur(taux)
                .setSeuilMontant(montant)
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(JOUR.minusDays(1))
                .setDateFin(JOUR.plusDays(1));
        for (Produit p : produits) {
            promotion.getProduits().add(new PromotionProduit(p, null));
        }
        return promotion;
    }

    @Test
    void leSeuilAtteintRemiseToutesLesLignesDeLaPromotionEtPasLesAutres() {
        Produit autre = new Produit();
        autre.setId(11);
        Produit horsPromotion = new Produit();
        horsPromotion.setId(12);
        ligne.setId(1L);
        SalesLine seconde = ligne(autre, 2_000, 1);
        seconde.setId(2L);
        SalesLine medicament = ligne(horsPromotion, 50_000, 1);
        medicament.setId(3L);
        vente.getSalesLines().add(seconde);
        vente.getSalesLines().add(medicament);
        // 3 000 + 2 000 = 5 000 : le seuil est atteint par les seuls articles de la promotion.
        enVigueur(seuil(7, 10, 5_000, produit, autre));

        assertTrue(moteur.appliquer(vente));

        assertEquals(300, ligne.getDiscountAmount());
        assertEquals(200, seconde.getDiscountAmount());
        assertEquals(7L, seconde.getPromotionId());
        assertEquals(1, seconde.getQuantitePromo());
        assertEquals(0, medicament.getDiscountAmount());
        assertEquals(null, medicament.getPromotionId());
    }

    @Test
    void sousLeSeuilLesLignesGardentLeurPromotion() {
        // 3 000 sur 5 000 : le seuil n'est pas atteint, la promotion de ligne s'applique.
        enVigueur(seuil(7, 20, 5_000, produit), promo(5, 10, 0));

        moteur.appliquer(vente);

        assertEquals(5L, ligne.getPromotionId());
        assertEquals(300, ligne.getDiscountAmount());
    }

    @Test
    void unArticleDejaMieuxRemiseNeCompteQueSiLePanierYGagne() {
        // Seuil atteint (3 000), -10 % de panier = 300 ; la promotion de ligne à -15 % fait 450 : elle garde la ligne.
        enVigueur(seuil(7, 10, 3_000, produit), promo(5, 15, 0));

        moteur.appliquer(vente);

        assertEquals(5L, ligne.getPromotionId());
        assertEquals(450, ligne.getDiscountAmount());
    }
}
