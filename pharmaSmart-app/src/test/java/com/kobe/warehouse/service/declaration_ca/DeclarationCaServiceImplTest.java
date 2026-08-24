package com.kobe.warehouse.service.declaration_ca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.ExclusionMotif;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Le chiffre d'affaires déclaré et l'encaissement déclaré doivent bouger ensemble.
 *
 * <p>C'est l'exigence centrale : sur une vente de 1 000 F dont 200 F d'unités gratuites, réglée
 * 1 000 F en espèces, afficher un CA de 800 et un encaissement de 1 000 rendrait l'état
 * indéfendable — l'écart de 200 ne se rattacherait à rien.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Exclusions de CA — vente et règlements")
class DeclarationCaServiceImplTest {

    private static final int STORAGE_ID = 1;

    @Mock
    private LicenseService licenseService;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private RayonProduitRepository rayonProduitRepository;

    @Mock
    private StorageService storageService;

    private DeclarationCaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeclarationCaServiceImpl(
            licenseService,
            appConfigurationService,
            rayonProduitRepository,
            storageService
        );
        Storage storage = new Storage();
        storage.setId(STORAGE_ID);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(storage);
        when(licenseService.hasFeature(any(Feature.class))).thenReturn(true);
        when(appConfigurationService.excludeFreeUnit()).thenReturn(true);
        when(rayonProduitRepository.findByProduitIdAndStorageId(anyInt(), anyInt())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("1 000 F dont 200 F d'UG réglés en espèces : CA 800 et encaissement 800")
    void lExempleDeReference() {
        CashSale vente = vente(ligneAvecUg(1_000, 200));
        vente.setPayments(reglements(reglement("CASH", 1_000)));

        service.appliquerExclusions(vente);

        assertEquals(800, vente.getAmountToBeTakenIntoAccount(), "chiffre d'affaires déclaré");
        assertEquals(800, declareDuPremierReglement(vente), "encaissement déclaré");
    }

    @Test
    @DisplayName("Sans unité gratuite ni rayon exclu, rien n'est retraité")
    void venteIntacte() {
        CashSale vente = vente(ligne(1_000));
        vente.setPayments(reglements(reglement("CASH", 1_000)));

        service.appliquerExclusions(vente);

        assertNull(premiereLigne(vente).getExclusionMotif(), "aucun motif ne doit être posé");
        assertNull(declareDuPremierReglementBrut(vente), "le règlement reste au montant encaissé");
    }

    @Test
    @DisplayName("La réduction mord d'abord sur les espèces, puis sur le règlement suivant")
    void repartitionEspecesDabord() {
        // Vente 1 000, réduction 700 : 600 absorbés par les espèces, 100 par le mobile.
        CashSale vente = vente(ligneAvecUg(1_000, 700));
        vente.setPayments(reglements(reglement("OM", 400), reglement("CASH", 600)));

        service.appliquerExclusions(vente);

        assertEquals(300, vente.getAmountToBeTakenIntoAccount());
        assertEquals(0, declarePourMode(vente, "CASH"), "les espèces sont absorbées en premier");
        assertEquals(300, declarePourMode(vente, "OM"), "le reliquat de 100 mord sur le mobile");
    }

    @Test
    @DisplayName("Un produit d'un rayon exclu sort entièrement du CA déclaré")
    void rayonExclu() {
        CashSale vente = vente(ligne(1_000));
        SalesLine ligne = premiereLigne(vente);
        when(rayonProduitRepository.findByProduitIdAndStorageId(ligne.getProduit().getId(), STORAGE_ID))
            .thenReturn(Optional.of(rayonProduit(true)));
        vente.setPayments(reglements(reglement("CASH", 1_000)));

        service.appliquerExclusions(vente);

        assertEquals(0, vente.getAmountToBeTakenIntoAccount());
        assertEquals(ExclusionMotif.RAYON, ligne.getExclusionMotif());
        assertEquals(0, declarePourMode(vente, "CASH"), "l'encaissement déclaré tombe avec le CA");
    }

    @Test
    @DisplayName("Module non souscrit : la licence prime, aucun retraitement")
    void moduleNonSouscrit() {
        when(licenseService.hasFeature(Feature.EXCLUSION_UG)).thenReturn(false);
        when(licenseService.hasFeature(Feature.EXCLUSION_RAYON)).thenReturn(false);
        CashSale vente = vente(ligneAvecUg(1_000, 200));
        vente.setPayments(reglements(reglement("CASH", 1_000)));

        service.appliquerExclusions(vente);

        assertNull(premiereLigne(vente).getExclusionMotif());
        assertNull(declareDuPremierReglementBrut(vente));
    }

    @Test
    @DisplayName("Vente différée sans règlement : le CA baisse, l'encaissement reste vide")
    void venteDiffereeSansReglement() {
        CashSale vente = vente(ligneAvecUg(1_000, 200));
        vente.setPayments(new LinkedHashSet<>());

        service.appliquerExclusions(vente);

        assertEquals(800, vente.getAmountToBeTakenIntoAccount(), "le chiffre d'affaires est bien réduit");
        // Rien à réduire côté encaissement : un encaissement inférieur au CA est le propre du crédit.
    }

    @Test
    @DisplayName("Une vente absente ou vide est ignorée")
    void venteAbsenteOuVide() {
        assertDoesNotThrow(() -> service.appliquerExclusions(null));
        assertDoesNotThrow(() -> service.appliquerExclusions(new CashSale()));
    }

    @Test
    @DisplayName("Le rayon exclu prime sur les unités gratuites de la même ligne")
    void rayonExcluPrimeSurLesUg() {
        CashSale vente = vente(ligneAvecUg(1_000, 200));
        SalesLine ligne = premiereLigne(vente);
        when(rayonProduitRepository.findByProduitIdAndStorageId(ligne.getProduit().getId(), STORAGE_ID))
            .thenReturn(Optional.of(rayonProduit(true)));
        vente.setPayments(reglements(reglement("CASH", 1_000)));

        service.appliquerExclusions(vente);

        assertEquals(0, ligne.getAmountToBeTakenIntoAccount());
        assertEquals(ExclusionMotif.RAYON, ligne.getExclusionMotif());
        assertEquals(0, declarePourMode(vente, "CASH"));
    }

    @Test
    @DisplayName("Le montant de la vente cumule les lignes retraitées et intactes")
    void cumulDePlusieursLignes() {
        CashSale vente = vente(ligneAvecUg(1_000, 200), ligne(500));
        vente.setPayments(reglements(reglement("CASH", 1_500)));

        service.appliquerExclusions(vente);

        assertEquals(1_300, vente.getAmountToBeTakenIntoAccount());
        assertEquals(1_300, declarePourMode(vente, "CASH"));
    }

    @Test
    @DisplayName("Une réduction supérieure à l'encaissement ne rend jamais un règlement négatif")
    void encaissementPartielJamaisNegatif() {
        CashSale vente = vente(ligneAvecUg(1_000, 800));
        vente.setPayments(reglements(reglement("CASH", 300)));

        service.appliquerExclusions(vente);

        assertEquals(200, vente.getAmountToBeTakenIntoAccount());
        assertEquals(0, declarePourMode(vente, "CASH"));
    }

    @Test
    @DisplayName("Un seul tiers payant exclu suffit à écarter toute la vente")
    void plusieursTiersPayantsDontUnExclu() {
        ThirdPartySales vente = venteTiersPayant(false, true);

        service.appliquerExclusions(vente);

        assertEquals(0, vente.getAmountToBeTakenIntoAccount());
        assertEquals(ExclusionMotif.TIERS_PAYANT, vente.getSalesLines().iterator().next().getExclusionMotif());
        assertEquals(0, declarePourMode(vente, "CASH"));
    }

    @Test
    @DisplayName("Sans tiers payant exclu, la vente assurée reste intacte")
    void tiersPayantsNonExclus() {
        ThirdPartySales vente = venteTiersPayant(false, false);

        service.appliquerExclusions(vente);

        assertEquals(1_000, vente.getAmountToBeTakenIntoAccount());
        assertNull(vente.getSalesLines().iterator().next().getExclusionMotif());
        assertNull(declareDuPremierReglementBrut(vente));
    }

    // ===== Utilitaires =====

    private CashSale vente(SalesLine... lignes) {
        CashSale vente = new CashSale();
        LinkedHashSet<SalesLine> set = new LinkedHashSet<>();
        long id = 1;
        for (SalesLine ligne : lignes) {
            ligne.setId(id++);
            set.add(ligne);
        }
        vente.setSalesLines(set);
        return vente;
    }

    private ThirdPartySales venteTiersPayant(boolean... exclusions) {
        ThirdPartySales vente = new ThirdPartySales();
        vente.setSalesLines(new LinkedHashSet<>(List.of(ligne(1_000))));
        vente.setAmountToBeTakenIntoAccount(1_000);
        vente.setPayments(reglements(reglement("CASH", 1_000)));
        List<ThirdPartySaleLine> lignesTiersPayant = new ArrayList<>();
        for (boolean exclu : exclusions) {
            TiersPayant tiersPayant = new TiersPayant().setBeExclude(exclu);
            ClientTiersPayant client = new ClientTiersPayant().setTiersPayant(tiersPayant);
            lignesTiersPayant.add(new ThirdPartySaleLine().setClientTiersPayant(client));
        }
        vente.setThirdPartySaleLines(lignesTiersPayant);
        return vente;
    }

    private SalesLine ligne(int montantTtc) {
        return ligneAvecUg(montantTtc, 0);
    }

    /** Une ligne à prix unitaire 1, pour que quantité et montant se lisent directement. */
    private SalesLine ligneAvecUg(int montantTtc, int montantUg) {
        Produit produit = new Produit();
        produit.setId(42);
        SalesLine ligne = new SalesLine();
        ligne.setProduit(produit);
        ligne.setRegularUnitPrice(1);
        ligne.setQuantityRequested(montantTtc);
        ligne.setQuantitySold(montantTtc);
        ligne.setQuantityUg(montantUg);
        ligne.setAmountToBeTakenIntoAccount(montantTtc);
        ligne.setTaxValue(0);
        ligne.setCostAmount(0);
        return ligne;
    }

    private RayonProduit rayonProduit(boolean exclu) {
        Rayon rayon = new Rayon();
        rayon.setExclude(exclu);
        RayonProduit rayonProduit = new RayonProduit();
        rayonProduit.setRayon(rayon);
        return rayonProduit;
    }

    private LinkedHashSet<SalePayment> reglements(SalePayment... paiements) {
        LinkedHashSet<SalePayment> set = new LinkedHashSet<>();
        long id = 1;
        for (SalePayment paiement : paiements) {
            paiement.setId(id++);
            set.add(paiement);
        }
        return set;
    }

    private SalePayment reglement(String code, int montant) {
        PaymentMode mode = new PaymentMode();
        mode.setCode(code);
        SalePayment paiement = new SalePayment();
        paiement.setPaymentMode(mode);
        paiement.setPaidAmount(montant);
        paiement.setReelAmount(montant);
        return paiement;
    }

    private SalesLine premiereLigne(CashSale vente) {
        return vente.getSalesLines().iterator().next();
    }

    private int declareDuPremierReglement(CashSale vente) {
        return vente.getPayments().iterator().next().getAmountToBeTakenIntoAccount();
    }

    private Integer declareDuPremierReglementBrut(Sales vente) {
        return vente.getPayments().iterator().next().getAmountToBeTakenIntoAccount();
    }

    private int declarePourMode(Sales vente, String code) {
        return vente
            .getPayments()
            .stream()
            .filter(paiement -> code.equals(paiement.getPaymentMode().getCode()))
            .findFirst()
            .map(paiement -> paiement.getAmountToBeTakenIntoAccount())
            .orElseThrow(() -> new AssertionError("aucun règlement " + code));
    }
}
