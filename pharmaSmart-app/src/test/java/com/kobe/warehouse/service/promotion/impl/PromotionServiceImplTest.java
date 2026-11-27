package com.kobe.warehouse.service.promotion.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.PromotionProduit;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.PromotionRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.promotion.EtatPromotion;
import com.kobe.warehouse.service.dto.promotion.PromotionArticleDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionFormDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionProduitFormDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Promotions : invariants de création")
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private PorteeMagasinService porteeMagasinService;

    @Mock
    private UserService userService;

    private PromotionServiceImpl service;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new PromotionServiceImpl(promotionRepository, produitRepository, porteeMagasinService, userService);
        magasin = new Magasin();
        magasin.setId(1);
        lenient().when(porteeMagasinService.resoudreLeMagasinDExploitation()).thenReturn(magasin);
        lenient().when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> {
            Promotion p = i.getArgument(0);
            return p.setId(42L);
        });
    }

    private Produit produit(int id, int prix, int tva, int prixAchat) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setLibelle("Article " + id);
        produit.setRegularUnitPrice(prix);
        Tva taux = new Tva();
        taux.setTaux(tva);
        produit.setTva(taux);
        FournisseurProduit principal = new FournisseurProduit();
        principal.setPrixAchat(prixAchat);
        produit.setFournisseurProduitPrincipal(principal);
        when(produitRepository.findById(id)).thenReturn(Optional.of(produit));
        return produit;
    }

    private static PromotionFormDTO form(TypePromotion type, Integer valeur, List<PromotionProduitFormDTO> produits) {
        LocalDate today = LocalDate.now();
        return new PromotionFormDTO("Solaire été", type, valeur, today, today.plusDays(10), 0, 1, null, null, produits);
    }

    @Test
    void creeEnBrouillonDansLeMagasinDeLUtilisateur() {
        produit(1, 10_000, 18, 6_000);

        PromotionDTO cree =
                service.creer(form(TypePromotion.PROMO_POURCENTAGE, 20, List.of(new PromotionProduitFormDTO(1, null))));

        assertEquals(StatutPromotion.BROUILLON, cree.statut());
        assertEquals(EtatPromotion.BROUILLON, cree.etat());
        assertEquals("PROMO-00042", cree.code());
        assertEquals(8_000, cree.produits().getFirst().prixApresPromo());
    }

    @Test
    void lePlafondTotalNePeutPasEtreSousLaQuantiteMinimaleEtSeRelitDansLeDto() {
        produit(1, 10_000, 18, 6_000);
        LocalDate today = LocalDate.now();
        List<PromotionProduitFormDTO> un = List.of(new PromotionProduitFormDTO(1, null));

        BadRequestAlertException e = assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "X", TypePromotion.PROMO_POURCENTAGE, 10, today, today.plusDays(3), 0, 3, null, 2, un)));
        assertTrue(e.getMessage().contains("plafond total"));

        PromotionDTO cree = service.creer(new PromotionFormDTO(
                "X", TypePromotion.PROMO_POURCENTAGE, 10, today, today.plusDays(3), 0, 1, null, 50, un));
        assertEquals(50, cree.quantiteMaxTotale());
        assertEquals(0L, cree.unitesRemisees(), "une promotion qui vient d'être créée n'a rien remisé");
    }

    @Test
    void offreNMControleSesUnitesSonGroupeEtSonPlancher() {
        produit(1, 3_000, 18, 1_500);
        LocalDate today = LocalDate.now();
        List<PromotionProduitFormDTO> un = List.of(new PromotionProduitFormDTO(1, null));

        // Sans unité offerte, l'offre ne veut rien dire.
        BadRequestAlertException sansOffertes = assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "2+0", TypePromotion.PROMO_OFFRE_NM, 0, today, today.plusDays(3), 0, 2, null, null, un)));
        assertTrue(sansOffertes.getMessage().contains("au moins une unité"));

        // Un plafond plus petit qu'un groupe (2+1 = 3) ne laisserait jamais l'offre s'appliquer.
        BadRequestAlertException plafond = assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "2+1", TypePromotion.PROMO_OFFRE_NM, 1, today, today.plusDays(3), 0, 2, 2, null, un)));
        assertTrue(plafond.getMessage().contains("groupe de l'offre (3 unités)"));

        // 1+1 sur un article acheté 1 500 et vendu 3 000 : 1 500 l'unité, soit le prix d'achat. Signalé.
        PromotionDTO aPerte = service.creer(new PromotionFormDTO(
                "1+1", TypePromotion.PROMO_OFFRE_NM, 1, today, today.plusDays(3), 0, 1, null, null, un));
        assertTrue(aPerte.alertes().getFirst().contains("à perte"));

        PromotionDTO cree = service.creer(new PromotionFormDTO(
                "2+1", TypePromotion.PROMO_OFFRE_NM, 1, today, today.plusDays(3), 0, 2, null, null, un));
        assertEquals(2_000, cree.produits().getFirst().prixApresPromo());
    }

    @Test
    void uneRemiseDePanierExigeUnSeuilRefuseLesPlafondsEtGardeLePlancher() {
        produit(1, 3_000, 18, 2_500);
        LocalDate today = LocalDate.now();
        List<PromotionProduitFormDTO> un = List.of(new PromotionProduitFormDTO(1, null));

        BadRequestAlertException sansSeuil = assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "-10 %",
                        TypePromotion.PROMO_SEUIL_MONTANT, 10, today, today.plusDays(3), 0, 1, null, null, un, null)));
        assertTrue(sansSeuil.getMessage().contains("montant d'achat"));

        BadRequestAlertException plafonnee = assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "-10 %",
                        TypePromotion.PROMO_SEUIL_MONTANT, 10, today, today.plusDays(3), 0, 1, 5, null, un, 20_000)));
        assertTrue(plafonnee.getMessage().contains("plafonds"));

        // -20 % d'un article acheté 2 500 et vendu 3 000 : 2 400, sous le prix d'achat. Signalé.
        PromotionDTO aPerte = service.creer(new PromotionFormDTO(
                "-20 %",
                TypePromotion.PROMO_SEUIL_MONTANT, 20, today, today.plusDays(3), 0, 1, null, null, un, 20_000));
        assertTrue(aPerte.produits().getFirst().aPerte());

        PromotionDTO cree = service.creer(new PromotionFormDTO(
                "-10 %",
                TypePromotion.PROMO_SEUIL_MONTANT, 10, today, today.plusDays(3), 0, 4, null, null, un, 20_000));
        assertEquals(20_000, cree.seuilMontant());
        assertEquals(1, cree.quantiteMin(), "un seuil en francs, pas en unités");
        assertEquals(2_700, cree.produits().getFirst().prixApresPromo());
    }

    @Test
    void seuleUneMecaniqueDeLigneSeCumule() {
        produit(1, 3_000, 18, 1_000);
        LocalDate today = LocalDate.now();
        List<PromotionProduitFormDTO> un = List.of(new PromotionProduitFormDTO(1, null));

        BadRequestAlertException e = assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "2+1",
                        TypePromotion.PROMO_OFFRE_NM,
                        1,
                        today,
                        today.plusDays(3),
                        0,
                        2,
                        null,
                        null,
                        un,
                        null,
                        true)));
        assertTrue(e.getMessage().contains("se cumule"));

        PromotionDTO cree = service.creer(new PromotionFormDTO(
                "-10 %",
                TypePromotion.PROMO_POURCENTAGE, 10, today, today.plusDays(3), 0, 1, null, null, un, null, true));
        assertTrue(cree.cumulable());
        assertFalse(
                service.creer(form(TypePromotion.PROMO_POURCENTAGE, 10, un)).cumulable(), "non cumulable par défaut");
    }

    @Test
    void unPackSeControleEnEntierEtRecoitSonCodeBarre() {
        produit(1, 5_000, 18, 2_500);
        produit(2, 4_500, 18, 2_000);
        LocalDate today = LocalDate.now();
        List<PromotionProduitFormDTO> duo =
                List.of(new PromotionProduitFormDTO(1, null, 1), new PromotionProduitFormDTO(2, null, 1));

        // Un seul article : ce n'est pas un pack.
        assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "Solo",
                        TypePromotion.PROMO_PACK,
                        4_000,
                        today,
                        today.plusDays(3),
                        0,
                        1,
                        null,
                        null,
                        List.of(new PromotionProduitFormDTO(1, null, 2)))));
        // Plus cher que les articles seuls (9 500) : aucune remise.
        BadRequestAlertException sansRemise = assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(new PromotionFormDTO(
                        "Duo", TypePromotion.PROMO_PACK, 9_500, today, today.plusDays(3), 0, 1, null, null, duo)));
        assertTrue(sansRemise.getMessage().contains("9500 F"));
        // Au prix d'achat des deux (4 500) : à perte, jugé sur le pack entier. Signalé, pas refusé.
        PromotionDTO aPerte = service.creer(new PromotionFormDTO(
                "Duo", TypePromotion.PROMO_PACK, 4_500, today, today.plusDays(3), 0, 1, null, null, duo));
        assertEquals(List.of("Pack vendu à perte : 4500 F pour un achat de 4500 F."), aPerte.alertes());
        assertFalse(aPerte.produits().getFirst().aPerte(), "un article de pack ne se juge pas seul");

        PromotionDTO cree = service.creer(new PromotionFormDTO(
                "Duo", TypePromotion.PROMO_PACK, 8_000, today, today.plusDays(3), 0, 1, null, null, duo));
        assertEquals("PK00000042", cree.codeBarrePack());
        assertEquals(1, cree.produits().getFirst().quantite());
    }

    @Test
    void signaleUnArticleAPrixAdministreSansLeRefuser() {
        // C'est au pharmacien de choisir ce qu'il promeut : le médicament est accepté, et signalé.
        produit(1, 10_000, 0, 6_000);

        PromotionDTO cree =
                service.creer(form(TypePromotion.PROMO_POURCENTAGE, 10, List.of(new PromotionProduitFormDTO(1, null))));
        assertEquals(StatutPromotion.BROUILLON, cree.statut());
        assertTrue(cree.produits().getFirst().prixAdministre());
        assertTrue(cree.alertes().getFirst().contains("prix administré"));
    }

    @Test
    void signaleUneVenteAPerteSansLaRefuser() {
        produit(1, 10_000, 18, 9_000);

        PromotionDTO cree =
                service.creer(form(TypePromotion.PROMO_POURCENTAGE, 15, List.of(new PromotionProduitFormDTO(1, null))));
        assertTrue(cree.produits().getFirst().aPerte());
        assertEquals(List.of("Vendu à perte : Article 1 (8500 F pour un achat à 9000 F)."), cree.alertes());
    }

    @Test
    void prixFixeExigeUnPrixParArticle() {
        produit(1, 10_000, 18, 6_000);

        assertThrows(
                BadRequestAlertException.class,
                () -> service.creer(
                        form(TypePromotion.PROMO_PRIX_FIXE, null, List.of(new PromotionProduitFormDTO(1, null)))));
    }

    @Test
    void refuseUnTauxHorsBornesEtDesDatesInversees() {
        produit(1, 10_000, 18, 6_000);
        List<PromotionProduitFormDTO> un = List.of(new PromotionProduitFormDTO(1, null));
        assertThrows(
                BadRequestAlertException.class, () -> service.creer(form(TypePromotion.PROMO_POURCENTAGE, 100, un)));

        LocalDate today = LocalDate.now();
        PromotionFormDTO inversees = new PromotionFormDTO(
                "X", TypePromotion.PROMO_POURCENTAGE, 10, today.plusDays(5), today, 0, 1, null, null, un);
        assertThrows(BadRequestAlertException.class, () -> service.creer(inversees));
    }

    @Test
    void uneAnnulationEstDefinitiveEtUnePromotionValideeNeSeModifiePas() {
        Promotion validee = new Promotion()
                .setId(7L)
                .setStatut(StatutPromotion.VALIDEE)
                .setType(TypePromotion.PROMO_POURCENTAGE)
                .setDateDebut(LocalDate.now())
                .setDateFin(LocalDate.now().plusDays(3));
        validee.getMagasins().add(magasin);
        when(promotionRepository.findOneWithProduits(7L)).thenReturn(Optional.of(validee));

        assertThrows(
                BadRequestAlertException.class,
                () -> service.modifier(7L, form(TypePromotion.PROMO_POURCENTAGE, 10, List.of())));

        service.annuler(7L);
        assertEquals(StatutPromotion.ANNULEE, validee.getStatut());
        assertThrows(BadRequestAlertException.class, () -> service.valider(7L));
    }

    @Test
    void unePromotionDUnAutreMagasinEstIntrouvable() {
        Magasin autre = new Magasin();
        autre.setId(2);
        Promotion ailleurs = new Promotion().setId(8L);
        ailleurs.getMagasins().add(autre);
        when(promotionRepository.findOneWithProduits(8L)).thenReturn(Optional.of(ailleurs));

        assertThrows(BadRequestAlertException.class, () -> service.lire(8L));
    }

    private Promotion enVigueur(long id, int taux, Integer plafond, Produit... produits) {
        Promotion promotion = new Promotion()
                .setId(id)
                .setLibelle("Promo " + id)
                .setType(TypePromotion.PROMO_POURCENTAGE)
                .setValeur(taux)
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(LocalDate.now().minusDays(1))
                .setDateFin(LocalDate.now().plusDays(5))
                .setQuantiteMaxTotale(plafond);
        for (Produit produit : produits) {
            promotion.getProduits().add(new PromotionProduit(produit, null));
        }
        return promotion;
    }

    @Test
    void articlesEnVigueurRetientLaPlusAvantageuseEtIgnoreLesEpuisees() {
        Produit a = new Produit();
        a.setId(1);
        a.setRegularUnitPrice(1_000);
        Produit b = new Produit();
        b.setId(2);
        b.setRegularUnitPrice(2_000);
        Promotion dix = enVigueur(5, 10, null, a, b);
        Promotion vingt = enVigueur(6, 20, null, a);
        Promotion epuisee = enVigueur(7, 50, 10, b);
        when(promotionRepository.findEnVigueur(1, LocalDate.now(), StatutPromotion.VALIDEE))
                .thenReturn(List.of(dix, vingt, epuisee));
        PromotionRepository.UnitesRemisees vendues = new PromotionRepository.UnitesRemisees() {
            @Override
            public Long getPromotionId() {
                return 7L;
            }

            @Override
            public Long getUnites() {
                return 10L;
            }
        };
        when(promotionRepository.sommerUnitesRemisees(List.of(7L))).thenReturn(List.of(vendues));

        Map<Integer, PromotionArticleDTO> parProduit = service.articlesEnVigueur().stream()
                .collect(Collectors.toMap(PromotionArticleDTO::produitId, Function.identity()));

        assertEquals(6L, parProduit.get(1).promotionId());
        assertEquals(800, parProduit.get(1).prixApresPromo());
        assertEquals(5L, parProduit.get(2).promotionId());
        assertEquals(1_800, parProduit.get(2).prixApresPromo());
    }
}
