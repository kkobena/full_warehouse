package com.kobe.warehouse.service.sale.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.CodeGrilleRemise;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.PromotionRepository;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.promotion.ChoixPerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.EtatPromotion;
import com.kobe.warehouse.service.dto.promotion.PerimetreDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionArticleDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionFormDTO;
import com.kobe.warehouse.service.dto.promotion.PromotionProduitFormDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.promotion.impl.PromotionServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les promotions sur un vrai PostgreSQL, du formulaire au ticket : le service les crée et les
 * valide, le moteur les pose au comptoir à chaque mutation du panier, et le compteur du plafond
 * total relit les ventes clôturées.
 *
 * <p>Ce qu'un double mémoire laisserait passer : le JPQL des promotions en vigueur (magasin, dates,
 * statut), la somme des unités remisées (statut de la vente, drapeau {@code canceled}, contrepassation),
 * la contrainte de V2.1.60, et l'écriture de {@code promotion_id} / {@code quantite_promo} sur des
 * lignes partitionnées par date.
 */
@DisplayName("Promotions — du formulaire au comptoir, sur PostgreSQL")
class PromotionIntegrationTest extends AbstractSaleIntegrationTest {

    private PromotionServiceImpl promotionService;
    private PromotionRepository promotionRepository;
    private Magasin magasinDeVente;

    @BeforeEach
    void monterLeServiceDesPromotions() {
        promotionRepository = services.promotionRepository;
        // La vente est estampillée du magasin du caissier : la promotion doit viser le même.
        magasinDeVente = caissier.getMagasin();
        PorteeMagasinService portee = mock(PorteeMagasinService.class);
        lenient().when(portee.resoudreLeMagasinDExploitation()).thenReturn(magasinDeVente);
        promotionService = new PromotionServiceImpl(
                promotionRepository,
                IntegrationPostgresDatabase.bean(ProduitRepository.class),
                portee,
                services.userService);
    }

    // ===== fabriques =====

    /** Un article à prix libre (TVA 18 %), acheté 400 chez son fournisseur principal. */
    private Produit article(String libelle, int prix) {
        Produit produit = produitEnStock(libelle, prix, 400, 18, 100);
        fournisseurProduit(produit, "CIP" + produit.getId());
        return produit;
    }

    private PromotionDTO promotionValidee(int taux, Integer maxParVente, Integer maxTotale, Produit... produits) {
        LocalDate aujourdhui = LocalDate.now();
        List<PromotionProduitFormDTO> lignes = java.util.Arrays.stream(produits)
                .map(p -> new PromotionProduitFormDTO(p.getId(), null))
                .toList();
        PromotionDTO creee = promotionService.creer(new PromotionFormDTO(
                "Opération " + taux + " %",
                TypePromotion.PROMO_POURCENTAGE,
                taux,
                aujourdhui.minusDays(1),
                aujourdhui.plusDays(10),
                0,
                1,
                maxParVente,
                maxTotale,
                lignes));
        PromotionDTO validee = promotionService.valider(creee.id());
        em.flush();
        return validee;
    }

    /** Une offre N+M validée : {@code quantite_min} porte N, {@code valeur} M. */
    private PromotionDTO offreValidee(int achetees, int offertes, Produit... produits) {
        LocalDate aujourdhui = LocalDate.now();
        List<PromotionProduitFormDTO> lignes = java.util.Arrays.stream(produits)
                .map(p -> new PromotionProduitFormDTO(p.getId(), null))
                .toList();
        PromotionDTO creee = promotionService.creer(new PromotionFormDTO(
                achetees + " + " + offertes + " offert",
                TypePromotion.PROMO_OFFRE_NM,
                offertes,
                aujourdhui.minusDays(1),
                aujourdhui.plusDays(10),
                0,
                achetees,
                null,
                null,
                lignes));
        PromotionDTO validee = promotionService.valider(creee.id());
        em.flush();
        return validee;
    }

    private CashSaleDTO panier(Produit produit, int quantite) {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setNatureVente(NatureVente.COMPTANT);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        dto.setSalesLines(List.of(ligneDe(produit, quantite)));
        return services.saleService.createCashSale(dto);
    }

    private void ajouter(SaleId vente, Produit produit, int quantite) {
        SaleLineDTO ligne = ligneDe(produit, quantite);
        ligne.setSaleCompositeId(vente);
        services.saleService.addOrUpdateSaleLine(ligne);
    }

    private static SaleLineDTO ligneDe(Produit produit, int quantite) {
        SaleLineDTO ligne = new SaleLineDTO();
        ligne.setProduitId(produit.getId());
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        return ligne;
    }

    /** Clôture au montant exact que la vente demande, remise et arrondi de caisse compris. */
    private void cloturer(SaleId saleId) {
        em.flush();
        int montant = em.find(CashSale.class, saleId).getAmountToBePaid();
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(saleId);
        dto.setPayrollAmount(montant);
        dto.setAmountToBePaid(montant);
        dto.setRestToPay(0);
        dto.setMontantRendu(0);
        PaymentDTO reglement = new PaymentDTO();
        reglement.setPaidAmount(montant);
        reglement.setNetAmount(montant);
        reglement.setPaymentMode(new PaymentModeDTO().setCode("CASH"));
        dto.setPayments(List.of(reglement));
        services.saleService.save(dto);
        em.flush();
    }

    private SalesLine ligneDe(SaleId vente, Produit produit) {
        viderLeCache();
        return em.find(CashSale.class, vente).getSalesLines().stream()
                .filter(l -> l.getProduit().getId().equals(produit.getId()))
                .findFirst()
                .orElseThrow();
    }

    private long unitesRemisees(Long promotionId) {
        return promotionRepository.sommerUnitesRemisees(List.of(promotionId)).stream()
                .mapToLong(PromotionRepository.UnitesRemisees::getUnites)
                .sum();
    }

    // ===== cas =====

    @Nested
    @DisplayName("Cumul avec la remise du caissier")
    class Cumul {

        /** Une remise produit à 5 % sur le code de grille des articles du test (CODE_0 → grille 12 au comptant). */
        private RemiseProduit cinqPourCent() {
            RemiseProduit remise = new RemiseProduit();
            remise.setValeur("Remise essai 5 %");
            GrilleRemise grille = new GrilleRemise();
            grille.setCode(CodeGrilleRemise.CODE_12);
            grille.setTauxRemise(0.05f);
            grille.setRemiseValue(5f);
            grille.setRemiseProduit(remise);
            remise.getGrilles().add(grille);
            em.persist(remise);
            em.flush();
            return remise;
        }

        private PromotionDTO dixPourCent(boolean cumulable, Produit produit) {
            LocalDate aujourdhui = LocalDate.now();
            PromotionDTO creee = promotionService.creer(new PromotionFormDTO(
                    "-10 % " + (cumulable ? "cumulable" : "seule"),
                    TypePromotion.PROMO_POURCENTAGE,
                    10,
                    aujourdhui.minusDays(1),
                    aujourdhui.plusDays(10),
                    0,
                    1,
                    null,
                    null,
                    List.of(new PromotionProduitFormDTO(produit.getId(), null)),
                    null,
                    cumulable));
            PromotionDTO validee = promotionService.valider(creee.id());
            em.flush();
            return validee;
        }

        @Test
        @DisplayName("choisir la remise produit garde la promotion cumulable, posée après elle")
        void cumulable() {
            Produit creme = article("CREME CUMUL", 2_000);
            PromotionDTO promotion = dixPourCent(true, creme);
            SaleId vente = panier(creme, 2).getSaleId();
            assertThat(ligneDe(vente, creme).getDiscountAmount())
                    .as("promotion seule")
                    .isEqualTo(400);

            services.saleService.processDiscount(
                    new UpdateSaleInfo(vente, cinqPourCent().getId()));
            em.flush();

            // 4 000 : la remise produit 200, puis -10 % des 3 800 restants, 380.
            SalesLine ligne = ligneDe(vente, creme);
            assertThat(ligne.getDiscountAmount()).isEqualTo(580);
            assertThat(ligne.getPromotionId()).isEqualTo(promotion.id());
            assertThat(em.find(CashSale.class, vente).getDiscountAmount()).isEqualTo(580);
            // La part de chacune est enregistrée, pour le journal des remises.
            assertThat(ligne.getRemisePromotion()).isEqualTo(380);
            assertThat(ligne.getRemiseProduit()).isEqualTo(200);

            services.saleService.removeRemiseFromCashSale(vente);
            em.flush();
            SalesLine sansGrille = ligneDe(vente, creme);
            assertThat(sansGrille.getDiscountAmount())
                    .as("la remise produit retirée, la promotion reste")
                    .isEqualTo(400);
            assertThat(sansGrille.getRemisePromotion()).isEqualTo(400);
            assertThat(sansGrille.getRemiseProduit()).isZero();
        }

        @Test
        @DisplayName("une promotion non cumulable cède la ligne à la remise produit")
        void nonCumulable() {
            Produit creme = article("CREME SEULE", 2_000);
            dixPourCent(false, creme);
            SaleId vente = panier(creme, 2).getSaleId();

            services.saleService.processDiscount(
                    new UpdateSaleInfo(vente, cinqPourCent().getId()));
            em.flush();

            SalesLine ligne = ligneDe(vente, creme);
            assertThat(ligne.getDiscountAmount()).isEqualTo(200);
            assertThat(ligne.getPromotionId()).isNull();
        }

        @Test
        @DisplayName("la base refuse une offre, un pack ou une remise de panier cumulable")
        void contrainte() {
            assertThatThrownBy(() -> em.createNativeQuery(
                                    "INSERT INTO promotion (libelle, type, valeur, date_debut, date_fin, quantite_min, cumulable)"
                                            + " VALUES ('X', 'PROMO_OFFRE_NM', 1, CURRENT_DATE, CURRENT_DATE, 2, true)")
                            .executeUpdate())
                    .hasMessageContaining("promotion_cumulable_ck");
        }
    }

    @Nested
    @DisplayName("Périmètre par rayon ou par famille")
    class Perimetre {

        private Produit promouvable;
        private Produit administre;
        private Produit inactif;

        @BeforeEach
        void troisArticles() {
            promouvable = article("CREME SOLAIRE SPF50", 6_000);
            administre = produitEnStock("PARACETAMOL 500", 1_000, 600, 0, 100);
            inactif = article("CREME SOLAIRE ARRETEE", 5_000);
            inactif.setStatus(Status.DISABLE);
        }

        @Test
        @DisplayName("un rayon du magasin donne ses articles actifs, les prix administrés signalés")
        void rayon() {
            Rayon solaire = new Rayon();
            solaire.setCode("SOL");
            solaire.setLibelle("Solaires");
            solaire.setStorage(em.createQuery(
                            "SELECT s FROM Storage s WHERE s.magasin.id = :m ORDER BY s.id",
                            com.kobe.warehouse.domain.Storage.class)
                    .setParameter("m", magasinDeVente.getId())
                    .setMaxResults(1)
                    .getSingleResult());
            em.persist(solaire);
            for (Produit p : List.of(promouvable, administre, inactif)) {
                em.persist(new RayonProduit().setRayon(solaire).setProduit(p));
            }
            em.flush();

            PerimetreDTO perimetre = promotionService.perimetre(solaire.getId(), null);

            // Le médicament est proposé comme les autres : c'est au pharmacien de le garder ou non.
            assertThat(perimetre.articles())
                    .extracting(PerimetreDTO.Article::produitId, PerimetreDTO.Article::prixAdministre)
                    .containsExactly(
                            org.assertj.core.api.Assertions.tuple(promouvable.getId(), false),
                            org.assertj.core.api.Assertions.tuple(administre.getId(), true));
            PerimetreDTO.Article creme = perimetre.articles().getFirst();
            assertThat(creme.prix()).isEqualTo(6_000);
            assertThat(creme.prixAchat()).as("prix du fournisseur principal").isPositive();
            assertThat(perimetre.administres()).containsExactly("PARACETAMOL 500");
            assertThat(promotionService.choixPerimetre().rayons())
                    .filteredOn(o -> o.id().equals(solaire.getId()))
                    .extracting(ChoixPerimetreDTO.Option::articles)
                    .as("les articles actifs seulement")
                    .containsExactly(2L);
            assertThat(promotionService.perimetre(solaire.getId(), null).articles())
                    .isNotEmpty();
            assertThat(promotionRepository.produitsDuRayon(solaire.getId(), -1))
                    .as("un rayon d'un autre magasin ne livre rien")
                    .isEmpty();
        }

        @Test
        @DisplayName("une famille donne ses articles actifs, tous rayons confondus")
        void famille() {
            FamilleProduit soins = new FamilleProduit();
            soins.setCode("SOINS-PROMO");
            soins.setLibelle("Soins solaires (essai)");
            soins.setCategorie(promouvable.getFamille().getCategorie());
            em.persist(soins);
            List.of(promouvable, administre, inactif).forEach(p -> p.setFamille(soins));
            em.flush();

            PerimetreDTO perimetre = promotionService.perimetre(null, soins.getId());

            assertThat(perimetre.articles())
                    .extracting(PerimetreDTO.Article::libelle)
                    .containsExactly("CREME SOLAIRE SPF50", "PARACETAMOL 500");
            assertThat(perimetre.administres()).containsExactly("PARACETAMOL 500");
            assertThat(promotionService.choixPerimetre().familles())
                    .filteredOn(o -> o.id().equals(soins.getId()))
                    .extracting(ChoixPerimetreDTO.Option::articles)
                    .containsExactly(2L);
        }

        @Test
        @DisplayName("il faut un rayon ou une famille, pas les deux")
        void unSeulCritere() {
            assertThatThrownBy(() -> promotionService.perimetre(null, null))
                    .hasMessageContaining("rayon ou une famille");
            assertThatThrownBy(() -> promotionService.perimetre(1, 1)).hasMessageContaining("rayon ou une famille");
        }
    }

    @Nested
    @DisplayName("Au comptoir")
    class AuComptoir {

        @Test
        @DisplayName("le moteur pose la remise, le taux, la promotion et les unités remisées sur la ligne")
        void laLigneEstPromue() {
            Produit produit = article("CREME SOLAIRE", 1_000);
            PromotionDTO promo = promotionValidee(20, null, null, produit);

            SaleId vente = panier(produit, 3).getSaleId();

            SalesLine ligne = ligneDe(vente, produit);
            assertThat(ligne.getDiscountAmount()).isEqualTo(600);
            assertThat(ligne.getTauxRemise()).isEqualTo(0.20f);
            assertThat(ligne.getPromotionId()).isEqualTo(promo.id());
            assertThat(ligne.getPromotionLibelle()).isEqualTo("Opération 20 %");
            assertThat(ligne.getQuantitePromo()).isEqualTo(3);
            CashSale lue = em.find(CashSale.class, vente);
            assertThat(lue.getDiscountAmount()).isEqualTo(600);
            assertThat(lue.getAmountToBePaid()).isEqualTo(2_400);
            assertThat(compter("SELECT quantite_promo FROM sales_line WHERE promotion_id = " + promo.id()))
                    .as("écrit en base, pas seulement en mémoire")
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("une promotion suspendue ne s'applique plus à la mutation suivante du panier")
        void suspendueNeSAppliquePlus() {
            Produit produit = article("SHAMPOOING", 2_000);
            PromotionDTO promo = promotionValidee(10, null, null, produit);
            SaleId vente = panier(produit, 1).getSaleId();
            assertThat(ligneDe(vente, produit).getPromotionId()).isEqualTo(promo.id());

            promotionService.suspendre(promo.id());
            em.flush();
            ajouter(vente, produit, 1);

            SalesLine ligne = ligneDe(vente, produit);
            assertThat(ligne.getPromotionId()).isNull();
            assertThat(ligne.getDiscountAmount()).isZero();
            assertThat(ligne.getQuantitePromo()).isNull();
        }

        @Test
        @DisplayName("le plafond par vente vaut pour le ticket : deux articles de la promotion le partagent")
        void plafondParVentePartage() {
            Produit premier = article("GEL DOUCHE", 1_000);
            Produit second = article("DEODORANT", 1_000);
            promotionValidee(10, 2, null, premier, second);

            SaleId vente = panier(premier, 2).getSaleId();
            ajouter(vente, second, 2);

            assertThat(ligneDe(vente, premier).getQuantitePromo()).isEqualTo(2);
            assertThat(ligneDe(vente, second).getPromotionId())
                    .as("la première ligne saisie a pris les deux unités du ticket")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Offre N+M")
    class OffreNM {

        @Test
        @DisplayName("2 + 1 sur une gamme : l'unité la moins chère est offerte, la remise répartie sur le groupe")
        void groupeSurDeuxArticles() {
            Produit shampooing = article("SHAMPOOING DOUX", 2_000);
            Produit masque = article("MASQUE CHEVEUX", 1_000);
            PromotionDTO offre = offreValidee(2, 1, shampooing, masque);

            SaleId vente = panier(shampooing, 2).getSaleId();
            assertThat(ligneDe(vente, shampooing).getPromotionId())
                    .as("deux unités : le groupe n'est pas complet")
                    .isNull();

            ajouter(vente, masque, 1);

            // Le masque (1 000) est offert ; 1 000 F au prorata de 4 000 et 1 000.
            SalesLine ligneShampooing = ligneDe(vente, shampooing);
            SalesLine ligneMasque = ligneDe(vente, masque);
            assertThat(ligneShampooing.getDiscountAmount()).isEqualTo(800);
            assertThat(ligneMasque.getDiscountAmount()).isEqualTo(200);
            assertThat(ligneShampooing.getPromotionId()).isEqualTo(offre.id());
            assertThat(ligneMasque.getPromotionId()).isEqualTo(offre.id());
            assertThat(ligneShampooing.getQuantitePromo()).isEqualTo(2);
            assertThat(ligneMasque.getTauxRemise()).isEqualTo(0.20f);
            assertThat(ligneMasque.getQuantityUg())
                    .as("les unités gratuites du fournisseur ne financent pas l'offre")
                    .isZero();
            CashSale lue = em.find(CashSale.class, vente);
            assertThat(lue.getDiscountAmount()).isEqualTo(1_000);
            assertThat(lue.getAmountToBePaid()).isEqualTo(4_000);
        }

        @Test
        @DisplayName("un pack complet est remisé sur ses deux lignes, et se retrouve par son code-barres")
        void pack() {
            Produit shampooing = article("SHAMPOOING PACK", 5_000);
            Produit apres = article("APRES-SHAMPOOING PACK", 4_500);
            LocalDate aujourdhui = LocalDate.now();
            PromotionDTO creee = promotionService.creer(new PromotionFormDTO(
                    "Duo cheveux",
                    TypePromotion.PROMO_PACK,
                    8_000,
                    aujourdhui.minusDays(1),
                    aujourdhui.plusDays(10),
                    0,
                    1,
                    null,
                    null,
                    List.of(
                            new PromotionProduitFormDTO(shampooing.getId(), null, 1),
                            new PromotionProduitFormDTO(apres.getId(), null, 1))));
            promotionService.valider(creee.id());
            em.flush();
            assertThat(creee.codeBarrePack()).startsWith("PK");
            assertThat(promotionService.pack(creee.codeBarrePack()).composants())
                    .hasSize(2);

            SaleId vente = panier(shampooing, 1).getSaleId();
            assertThat(ligneDe(vente, shampooing).getPromotionId())
                    .as("pack incomplet")
                    .isNull();
            ajouter(vente, apres, 1);

            assertThat(ligneDe(vente, shampooing).getDiscountAmount()).isEqualTo(789);
            assertThat(ligneDe(vente, apres).getDiscountAmount()).isEqualTo(711);
            CashSale lue = em.find(CashSale.class, vente);
            assertThat(lue.getDiscountAmount()).isEqualTo(1_500);
            assertThat(lue.getAmountToBePaid()).isEqualTo(8_000);

            // Retirer un article défait le pack : l'autre revient au prix, et la vente avec lui.
            services.salesManager.deleteSaleLineById(ligneDe(vente, apres));
            em.flush();
            assertThat(ligneDe(vente, shampooing).getDiscountAmount()).isZero();
            assertThat(ligneDe(vente, shampooing).getPromotionId()).isNull();
            assertThat(em.find(CashSale.class, vente).getDiscountAmount()).isZero();
        }

        @Test
        @DisplayName("la base refuse une offre N+M sans unité offerte")
        void contrainteDeLaMigration() {
            assertThatThrownBy(() -> em.createNativeQuery(
                                    "INSERT INTO promotion (libelle, type, valeur, date_debut, date_fin, quantite_min)"
                                            + " VALUES ('X', 'PROMO_OFFRE_NM', 0, CURRENT_DATE, CURRENT_DATE, 2)")
                            .executeUpdate())
                    .hasMessageContaining("promotion_offre_nm_ck");
        }

        @Test
        @DisplayName("une remise de panier se déclenche au seuil et se répartit sur les lignes de la promotion")
        void seuilMontant() {
            Produit creme = article("CREME SEUIL", 12_000);
            Produit lait = article("LAIT SEUIL", 8_500);
            LocalDate aujourdhui = LocalDate.now();
            PromotionDTO creee = promotionService.creer(new PromotionFormDTO(
                    "-10 % dès 20 000",
                    TypePromotion.PROMO_SEUIL_MONTANT,
                    10,
                    aujourdhui.minusDays(1),
                    aujourdhui.plusDays(10),
                    0,
                    1,
                    null,
                    null,
                    List.of(
                            new PromotionProduitFormDTO(creme.getId(), null),
                            new PromotionProduitFormDTO(lait.getId(), null)),
                    20_000));
            promotionService.valider(creee.id());
            em.flush();

            SaleId vente = panier(creme, 1).getSaleId();
            assertThat(ligneDe(vente, creme).getPromotionId())
                    .as("12 000 sur 20 000")
                    .isNull();
            ajouter(vente, lait, 1);

            // 20 500 atteint : 2 050 répartis au prorata, 1 200 et 850.
            assertThat(ligneDe(vente, creme).getDiscountAmount()).isEqualTo(1_200);
            assertThat(ligneDe(vente, lait).getDiscountAmount()).isEqualTo(850);
            assertThat(ligneDe(vente, lait).getPromotionId()).isEqualTo(creee.id());
            assertThat(em.find(CashSale.class, vente).getDiscountAmount()).isEqualTo(2_050);
        }

        @Test
        @DisplayName("la base refuse une remise de panier sans seuil, ou plafonnée en unités")
        void contrainteDuSeuil() {
            assertThatThrownBy(() -> em.createNativeQuery(
                                    "INSERT INTO promotion (libelle, type, valeur, date_debut, date_fin, quantite_min)"
                                            + " VALUES ('X', 'PROMO_SEUIL_MONTANT', 10, CURRENT_DATE, CURRENT_DATE, 1)")
                            .executeUpdate())
                    .hasMessageContaining("promotion_seuil_montant_ck");
        }
    }

    @Nested
    @DisplayName("Plafond total")
    class PlafondTotal {

        @Test
        @DisplayName("un panier en cours ne compte pas ; une vente clôturée, si")
        void seulesLesVentesCloturreesComptent() {
            Produit produit = article("BAUME LEVRES", 1_000);
            PromotionDTO promo = promotionValidee(10, null, 5, produit);

            SaleId vente = panier(produit, 4).getSaleId();
            em.flush();
            assertThat(unitesRemisees(promo.id())).isZero();

            cloturer(vente);
            assertThat(unitesRemisees(promo.id())).isEqualTo(4);
        }

        @Test
        @DisplayName("au-delà du plafond, les unités restent au prix normal sur la même ligne")
        void lesDernieresUnites() {
            Produit produit = article("LAIT CORPS", 1_000);
            PromotionDTO promo = promotionValidee(10, null, 5, produit);
            cloturer(panier(produit, 4).getSaleId());

            SaleId suivante = panier(produit, 3).getSaleId();

            SalesLine ligne = ligneDe(suivante, produit);
            assertThat(ligne.getQuantitePromo()).isEqualTo(1);
            assertThat(ligne.getDiscountAmount()).as("10 % d'une seule unité").isEqualTo(100);
            assertThat(promotionService.lire(promo.id()).unitesRemisees()).isEqualTo(4);
        }

        @Test
        @DisplayName("atteint, la promotion est épuisée : plus de remise, plus de badge")
        void epuisee() {
            Produit produit = article("BRUME VISAGE", 1_000);
            PromotionDTO promo = promotionValidee(10, null, 5, produit);
            cloturer(panier(produit, 5).getSaleId());

            PromotionDTO lue = promotionService.lire(promo.id());
            assertThat(lue.etat()).isEqualTo(EtatPromotion.EPUISEE);
            assertThat(lue.unitesRemisees()).isEqualTo(5);
            assertThat(promotionService.articlesEnVigueur())
                    .extracting(PromotionArticleDTO::produitId)
                    .doesNotContain(produit.getId());

            SaleId suivante = panier(produit, 1).getSaleId();
            assertThat(ligneDe(suivante, produit).getPromotionId()).isNull();
        }

        @Test
        @DisplayName("une vente annulée rend ses unités : ni elle ni sa contrepassation ne comptent")
        void annulationRendLesUnites() {
            Produit produit = article("SAVON SURGRAS", 1_000);
            PromotionDTO promo = promotionValidee(10, null, 5, produit);
            SaleId vente = panier(produit, 5).getSaleId();
            cloturer(vente);
            assertThat(unitesRemisees(promo.id())).isEqualTo(5);

            services.saleService.cancelCashSale(vente, "erreur de saisie");
            em.flush();

            assertThat(unitesRemisees(promo.id())).isZero();
            assertThat(promotionService.lire(promo.id()).etat()).isEqualTo(EtatPromotion.EN_COURS);
            SaleId suivante = panier(produit, 2).getSaleId();
            assertThat(ligneDe(suivante, produit).getQuantitePromo()).isEqualTo(2);
        }

        @Test
        @DisplayName("la base refuse un plafond total inférieur à la quantité minimale")
        void contrainteDeLaMigration() {
            assertThatThrownBy(() -> em.createNativeQuery(
                                    "INSERT INTO promotion (libelle, type, valeur, date_debut, date_fin, quantite_min,"
                                            + " quantite_max_totale) VALUES ('X', 'PROMO_POURCENTAGE', 10,"
                                            + " CURRENT_DATE, CURRENT_DATE, 3, 2)")
                            .executeUpdate())
                    .hasMessageContaining("promotion_quantite_max_totale_ck");
        }
    }

    @Nested
    @DisplayName("Lectures")
    class Lectures {

        @Test
        @DisplayName("seules les promotions validées, du magasin et dans leurs dates, sont en vigueur")
        void enVigueur() {
            Produit produit = article("CREME MAINS", 1_000);
            PromotionDTO validee = promotionValidee(10, null, null, produit);
            Promotion brouillon = promotionRepository
                    .findById(promotionService
                            .creer(new PromotionFormDTO(
                                    "Brouillon",
                                    TypePromotion.PROMO_POURCENTAGE,
                                    30,
                                    LocalDate.now(),
                                    LocalDate.now().plusDays(3),
                                    0,
                                    1,
                                    null,
                                    null,
                                    List.of(new PromotionProduitFormDTO(produit.getId(), null))))
                            .id())
                    .orElseThrow();
            em.flush();
            LocalDate aujourdhui = LocalDate.now();

            assertThat(promotionRepository.findEnVigueur(magasinDeVente.getId(), aujourdhui, StatutPromotion.VALIDEE))
                    .extracting(Promotion::getId)
                    .contains(validee.id())
                    .doesNotContain(brouillon.getId());
            assertThat(promotionRepository.findEnVigueur(
                            magasinDeVente.getId(), aujourdhui.plusDays(30), StatutPromotion.VALIDEE))
                    .extracting(Promotion::getId)
                    .doesNotContain(validee.id());
            assertThat(promotionRepository.findEnVigueur(-1, aujourdhui, StatutPromotion.VALIDEE))
                    .isEmpty();
        }

        @Test
        @DisplayName("le badge retient, par article, la promotion la plus avantageuse")
        void articlesEnVigueur() {
            Produit produit = article("SERUM", 2_000);
            promotionValidee(10, null, null, produit);
            PromotionDTO meilleure = promotionValidee(25, null, null, produit);

            Map<Integer, PromotionArticleDTO> parProduit = promotionService.articlesEnVigueur().stream()
                    .collect(Collectors.toMap(PromotionArticleDTO::produitId, Function.identity()));

            PromotionArticleDTO badge = parProduit.get(produit.getId());
            assertThat(badge.promotionId()).isEqualTo(meilleure.id());
            assertThat(badge.prixReference()).isEqualTo(2_000);
            assertThat(badge.prixApresPromo()).isEqualTo(1_500);
        }
    }

    @Nested
    @DisplayName("Ventes à crédit : carnet en entier, assurance sur les lignes non remboursées")
    class VentesACredit {

        private com.kobe.warehouse.service.dto.ThirdPartySaleDTO venteACredit(
                NatureVente nature, com.kobe.warehouse.domain.ClientTiersPayant compte, Produit produit, String bon) {
            com.kobe.warehouse.service.dto.ThirdPartySaleDTO dto =
                    new com.kobe.warehouse.service.dto.ThirdPartySaleDTO();
            dto.setNatureVente(nature);
            dto.setTypePrescription(TypePrescription.PRESCRIPTION);
            dto.setCustomerId(compte.getAssuredCustomer().getId());
            dto.setSalesLines(List.of(ligneDe(produit, 1)));
            com.kobe.warehouse.service.dto.ClientTiersPayantDTO tp =
                    new com.kobe.warehouse.service.dto.ClientTiersPayantDTO();
            tp.setId(compte.getId());
            tp.setTaux(compte.getTaux());
            tp.setNumBon(bon);
            dto.setTiersPayants(new java.util.ArrayList<>(List.of(tp)));
            return services.thirdPartySaleService.createSale(dto);
        }

        @Test
        @DisplayName("une vente carnet reçoit la promotion sur sa ligne")
        void carnet() {
            Produit creme = article("CREME CARNET", 6_000);
            promotionValidee(10, null, null, creme);
            var compte = compte("ENTREPRISE", 100, com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant.R0);

            var cree = venteACredit(NatureVente.CARNET, compte, creme, "CARNET-1");
            em.flush();
            em.clear();

            SalesLine ligne = em.find(com.kobe.warehouse.domain.ThirdPartySales.class, cree.getSaleId())
                    .getSalesLines()
                    .iterator()
                    .next();
            assertThat(ligne.getPromotionId()).isNotNull();
            assertThat(ligne.getDiscountAmount()).isEqualTo(600);
            var vente = em.find(com.kobe.warehouse.domain.ThirdPartySales.class, cree.getSaleId());
            assertThat(vente.getPartTiersPayant())
                    .as("comptant à règlement différé : l'organisme est facturé au prix remisé")
                    .isEqualTo(5_400);
            assertThat(vente.getPartAssure()).isZero();
        }

        @Test
        @DisplayName("une vente assurance ne remise que la ligne passée en non remboursée, nette pour le patient")
        void assurance() {
            Produit creme = article("CREME ASSURANCE", 6_000);
            promotionValidee(10, null, null, creme);
            var compte = compte("MUGEF", 80, com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant.R0);

            var cree = venteACredit(NatureVente.ASSURANCE, compte, creme, "ASS-PROMO-1");
            em.flush();
            em.clear();
            var vente = em.find(com.kobe.warehouse.domain.ThirdPartySales.class, cree.getSaleId());
            SalesLine ligne = vente.getSalesLines().iterator().next();
            assertThat(ligne.getPromotionId())
                    .as("ligne couverte : jamais remisée")
                    .isNull();
            assertThat(vente.getPartTiersPayant()).isEqualTo(4_800);

            SaleLineDTO nonRembourse = new SaleLineDTO();
            nonRembourse.setSaleLineId(ligne.getId());
            nonRembourse.setSaleCompositeId(cree.getSaleId());
            nonRembourse.setNonRembourse(true);
            services.thirdPartySaleService.updateItemNonRembourse(nonRembourse);
            em.flush();
            em.clear();

            vente = em.find(com.kobe.warehouse.domain.ThirdPartySales.class, cree.getSaleId());
            ligne = vente.getSalesLines().iterator().next();
            assertThat(ligne.getPromotionId()).isNotNull();
            assertThat(ligne.getDiscountAmount()).isEqualTo(600);
            assertThat(vente.getPartTiersPayant()).isZero();
            assertThat(vente.getPartAssure())
                    .as("le patient paie la ligne, promotion déduite")
                    .isEqualTo(5_400);
            assertThat(vente.getMontantNonRembourse()).isEqualTo(6_000);
            assertThat(vente.getRemiseNonRembourse()).isEqualTo(600);
            assertThat(vente.getPartAssureRembourse())
                    .as("rien sur la partie remboursable")
                    .isZero();

            // Repassée en remboursée : la promotion s'efface.
            nonRembourse.setNonRembourse(false);
            services.thirdPartySaleService.updateItemNonRembourse(nonRembourse);
            em.flush();
            em.clear();
            ligne = em.find(com.kobe.warehouse.domain.ThirdPartySales.class, cree.getSaleId())
                    .getSalesLines()
                    .iterator()
                    .next();
            assertThat(ligne.getPromotionId()).isNull();
            assertThat(ligne.getDiscountAmount()).isZero();
        }
    }

    @Nested
    @DisplayName("Bilan : coût, ventes et comparaison avec la période précédente")
    class Bilan {

        @Test
        @DisplayName("ne compte que les ventes clôturées, et exporte une ligne par article")
        void bilan() throws Exception {
            Produit creme = article("CREME BILAN", 6_000);
            Produit savon = article("SAVON BILAN", 1_000);
            PromotionDTO promo = promotionValidee(10, null, null, creme, savon);
            SaleId close = panier(creme, 2).getSaleId();
            cloturer(close);
            panier(creme, 1); // en cours : ne compte pas
            em.flush();
            int cout = ligneDe(close, creme).getCostAmount();

            PorteeMagasinService portee = mock(PorteeMagasinService.class);
            lenient().when(portee.resoudreLeMagasinDExploitation()).thenReturn(magasinDeVente);
            var service = new com.kobe.warehouse.service.promotion.impl.BilanPromotionServiceImpl(
                    promotionRepository, portee, new com.kobe.warehouse.service.report.excel.CsvExportService());
            var bilan = service.bilan(promo.id());

            LocalDate hier = LocalDate.now().minusDays(1);
            assertThat(bilan.debut()).isEqualTo(hier);
            assertThat(bilan.fin())
                    .as("l'opération court encore : jusqu'à aujourd'hui")
                    .isEqualTo(LocalDate.now());
            assertThat(bilan.debutAvant()).as("même durée, juste avant").isEqualTo(hier.minusDays(2));
            assertThat(bilan.finAvant()).isEqualTo(hier.minusDays(1));
            assertThat(bilan.remise()).as("10 % de 2 × 6 000").isEqualTo(1_200);
            assertThat(bilan.unitesRemisees()).isEqualTo(2);
            assertThat(bilan.tickets()).isEqualTo(1);
            assertThat(bilan.pendant().unites()).isEqualTo(2);
            assertThat(bilan.pendant().chiffreAffaires()).isEqualTo(10_800);
            assertThat(bilan.pendant().marge()).isEqualTo(10_800 - 2L * cout);
            assertThat(bilan.avant().unites()).isZero();
            assertThat(bilan.articles())
                    .extracting(a -> a.libelle(), a -> a.unites())
                    .as("chaque article de l'opération, même sans vente")
                    .containsExactly(
                            org.assertj.core.api.Assertions.tuple("CREME BILAN", 2L),
                            org.assertj.core.api.Assertions.tuple("SAVON BILAN", 0L));

            String csv = new String(service.exporter(promo.id()), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(csv).contains("CREME BILAN;2;0;2;1200;10800").contains("SAVON BILAN;0;0;0;0;0");
        }
    }
}
