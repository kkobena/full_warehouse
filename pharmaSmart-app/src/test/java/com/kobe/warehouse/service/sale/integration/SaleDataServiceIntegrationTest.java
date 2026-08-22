package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.SaleDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * {@link com.kobe.warehouse.service.sale.SaleDataService} sur un vrai PostgreSQL.
 *
 * <p>Ce service est presque entièrement fait de requêtes : du SQL natif pour la liste des ventes
 * terminées, l'API Criteria pour les préventes et les ventes dépôt. Rien de tout cela n'a de sens
 * hors d'une base — la clause {@code EXISTS} sur les lignes, le {@code TO_CHAR(..., 'HH24:MI')} des
 * bornes horaires, le {@code cb.treat} sur la hiérarchie des ventes ne se vérifient qu'à
 * l'exécution.
 */
@DisplayName("SaleDataService — recherche et lecture des ventes sur PostgreSQL")
class SaleDataServiceIntegrationTest extends AbstractSaleIntegrationTest {

    private static final LocalDate AUJOURD_HUI = LocalDate.now();
    private static final LocalDate HIER = AUJOURD_HUI.minusDays(1);

    @Test
    @DisplayName("La liste des ventes terminées ne retient que les ventes clôturées du magasin")
    void listeDesVentesTerminees() {
        Produit produit = produitEnStock("DOLIPRANE DATA", 1_000, 600, 0, 100);
        venteCloturee(produit, 2, AUJOURD_HUI);
        venteCloturee(produit, 3, AUJOURD_HUI);
        venteEnCours(produit, 1);
        viderLeCache();

        var page = services.saleDataService.listVenteTerminees(
            null, AUJOURD_HUI, AUJOURD_HUI, null, null, false, null, null, null, null, null, null, PageRequest.of(0, 20)
        );

        assertEquals(2, page.getTotalElements(), "la vente encore active n'y figure pas");
        assertTrue(page.getContent().stream().allMatch(v -> v.getStatut() == SalesStatut.CLOSED));
    }

    @Test
    @DisplayName("Les bornes de date excluent ce qui tombe en dehors")
    void filtreParDate() {
        Produit produit = produitEnStock("EFFERALGAN DATA", 800, 500, 0, 100);
        venteCloturee(produit, 1, HIER);
        venteCloturee(produit, 1, AUJOURD_HUI);
        viderLeCache();

        var hier = services.saleDataService.listVenteTerminees(
            null, HIER, HIER, null, null, false, null, null, null, null, null, null, PageRequest.of(0, 20)
        );
        var lesDeux = services.saleDataService.listVenteTerminees(
            null, HIER, AUJOURD_HUI, null, null, false, null, null, null, null, null, null, PageRequest.of(0, 20)
        );

        assertEquals(1, hier.getTotalElements());
        assertEquals(2, lesDeux.getTotalElements());
    }

    @Test
    @DisplayName("Hors mode global, la recherche porte sur le début de la référence de vente")
    void rechercheParReference() {
        Produit produit = produitEnStock("SPASFON DATA", 1_200, 700, 0, 100);
        CashSale cible = venteCloturee(produit, 1, AUJOURD_HUI);
        venteCloturee(produit, 1, AUJOURD_HUI);
        viderLeCache();

        var page = services.saleDataService.listVenteTerminees(
            cible.getNumberTransaction(),
            AUJOURD_HUI,
            AUJOURD_HUI,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals(cible.getNumberTransaction(), page.getContent().getFirst().getNumberTransaction());
    }

    @Test
    @DisplayName("En mode global, la recherche descend dans les lignes jusqu'au libellé du produit")
    void rechercheGlobaleSurLeProduit() {
        Produit cherche = produitEnStock("ZYRTECSET", 3_000, 1_800, 0, 100);
        Produit autre = produitEnStock("XYZAL", 3_200, 1_900, 0, 100);
        venteCloturee(cherche, 1, AUJOURD_HUI);
        venteCloturee(autre, 1, AUJOURD_HUI);
        viderLeCache();

        var page = services.saleDataService.listVenteTerminees(
            "zyrtec", AUJOURD_HUI, AUJOURD_HUI, null, null, true, null, null, null, null, null, null, PageRequest.of(0, 20)
        );

        assertEquals(1, page.getTotalElements(), "la sous-requête EXISTS sur sales_line fait le tri");
    }

    @Test
    @DisplayName("Le total des ventes terminées somme les montants de la période")
    void totalDesVentes() {
        Produit produit = produitEnStock("ADVIL DATA", 1_500, 900, 0, 100);
        venteCloturee(produit, 2, AUJOURD_HUI);
        venteCloturee(produit, 4, AUJOURD_HUI);
        viderLeCache();

        long total = services.saleDataService.totalVenteTerminees(
            null, AUJOURD_HUI, AUJOURD_HUI, null, null, false, null, null, null, null
        );

        assertEquals(9_000, total, "3 000 + 6 000");
    }

    @Test
    @DisplayName("La pagination limite la page sans fausser le décompte total")
    void pagination() {
        Produit produit = produitEnStock("PAGINATION", 1_000, 600, 0, 100);
        venteCloturee(produit, 1, AUJOURD_HUI);
        venteCloturee(produit, 1, AUJOURD_HUI);
        venteCloturee(produit, 1, AUJOURD_HUI);
        viderLeCache();

        var page = services.saleDataService.listVenteTerminees(
            null, AUJOURD_HUI, AUJOURD_HUI, null, null, false, null, null, null, null, null, null, PageRequest.of(0, 2)
        );

        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    @DisplayName("Une période sans vente rend une page vide plutôt qu'une erreur")
    void periodeSansVente() {
        var page = services.saleDataService.listVenteTerminees(
            null,
            AUJOURD_HUI.minusYears(1),
            AUJOURD_HUI.minusYears(1),
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        );

        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Les achats d'un client ne remontent que ses ventes clôturées")
    void achatsDUnClient() {
        Produit produit = produitEnStock("CLIENT DATA", 2_000, 1_200, 0, 100);
        UninsuredCustomer client = client("DIALLO", "Fatou", "CLI-DATA-1");
        CashSale sienne = venteCloturee(produit, 2, AUJOURD_HUI);
        sienne.setCustomer(client);
        venteCloturee(produit, 1, AUJOURD_HUI);
        viderLeCache();

        List<SaleDTO> achats = services.saleDataService.customerPurchases(client.getId(), HIER, AUJOURD_HUI);

        assertEquals(1, achats.size());
        assertEquals(4_000, achats.getFirst().getSalesAmount());
    }

    @Test
    @DisplayName("Une vente est relue avec ses lignes par son identifiant composite")
    void lectureDUneVente() {
        Produit produit = produitEnStock("LECTURE", 900, 500, 0, 100);
        CashSale vente = venteCloturee(produit, 3, AUJOURD_HUI);
        viderLeCache();

        SaleDTO lue = services.saleDataService.fetchPurchaseBy(vente.getId().getId(), vente.getSaleDate());

        assertEquals(vente.getNumberTransaction(), lue.getNumberTransaction());
        assertEquals(2_700, lue.getSalesAmount());
        assertEquals(1, lue.getSalesLines().size());
    }

    @Test
    @DisplayName("Les préventes remontent par statut, les ventes clôturées restent dehors")
    void listeDesPreventes() {
        Produit produit = produitEnStock("PREVENTE", 700, 400, 0, 100);
        venteEnCours(produit, 2).setStatut(SalesStatut.PROCESSING);
        venteCloturee(produit, 1, AUJOURD_HUI);
        viderLeCache();

        List<SaleDTO> preventes = services.saleDataService.allPrevente(
            null,
            null,
            null,
            Set.of(SalesStatut.PROCESSING),
            HIER,
            AUJOURD_HUI,
            true
        );

        assertEquals(1, preventes.size());
        assertEquals(SalesStatut.PROCESSING, preventes.getFirst().getStatut());
    }

    @Test
    @DisplayName("Les ventes du jour encore ouvertes sont comptées, hors ventes dépôt")
    void comptageDesVentesEnCours() {
        Produit produit = produitEnStock("EN COURS", 1_000, 600, 0, 100);
        venteEnCours(produit, 1);
        venteEnCours(produit, 1);
        venteCloturee(produit, 1, AUJOURD_HUI);
        viderLeCache();

        assertEquals(2, services.saleDataService.countPendingSales(null));
    }

    @Test
    @DisplayName("Les ventes dépôt ont leur propre liste, filtrée par dépôt")
    void listeDesVentesDepot() {
        Produit produit = produitEnStock("DEPOT DATA", 1_000, 600, 0, 100);
        Magasin premier = depot("DEPOT DATA A");
        Magasin second = depot("DEPOT DATA B");
        venteDepotCloturee(premier, produit, 5);
        venteDepotCloturee(second, produit, 7);
        viderLeCache();

        var toutes = services.saleDataService.fetchVenteDepot(null, HIER, AUJOURD_HUI, null, null, null, PageRequest.of(0, 20));
        var duPremier = services.saleDataService.fetchVenteDepot(
            null,
            HIER,
            AUJOURD_HUI,
            null,
            null,
            premier.getId(),
            PageRequest.of(0, 20)
        );

        assertEquals(2, toutes.getTotalElements());
        assertEquals(1, duPremier.getTotalElements());
        assertEquals(5_000, duPremier.getContent().getFirst().getSalesAmount());
    }

    @Test
    @DisplayName("Une vente dépôt ne se mélange pas aux ventes terminées de l'officine")
    void venteDepotHorsListeOfficine() {
        Produit produit = produitEnStock("CLOISON", 1_000, 600, 0, 100);
        venteDepotCloturee(depot("DEPOT CLOISON"), produit, 5);
        viderLeCache();

        var page = services.saleDataService.listVenteTerminees(
            null, AUJOURD_HUI, AUJOURD_HUI, null, null, false, null, null, null, null, null, null, PageRequest.of(0, 20)
        );

        assertEquals(0, page.getTotalElements(), "CA_DEPOT n'entre pas dans les catégories par défaut");
    }

    @Test
    @DisplayName("Les lignes laissées en avoir sont retrouvables")
    void lignesEnAvoir() {
        Produit produit = produitEnStock("AVOIR DATA", 1_000, 600, 0, 100);
        CashSale vente = venteCloturee(produit, 2, AUJOURD_HUI);
        vente.getSalesLines().iterator().next().setQuantityAvoir(1);
        viderLeCache();

        List<SalesLine> avoirs = services.saleDataService.getAllAvoirs();

        assertFalse(avoirs.isEmpty());
        assertTrue(avoirs.stream().allMatch(l -> l.getQuantityAvoir() > 0));
    }

    // ===== outils =====

    private CashSale venteCloturee(Produit produit, int quantite, LocalDate date) {
        CashSale vente = new CashSale();
        remplir(vente, produit, quantite, date, SalesStatut.CLOSED, CategorieChiffreAffaire.CA);
        return vente;
    }

    private CashSale venteEnCours(Produit produit, int quantite) {
        CashSale vente = new CashSale();
        remplir(vente, produit, quantite, AUJOURD_HUI, SalesStatut.ACTIVE, CategorieChiffreAffaire.CA);
        return vente;
    }

    private VenteDepot venteDepotCloturee(Magasin depot, Produit produit, int quantite) {
        VenteDepot vente = new VenteDepot();
        vente.setDepot(depot);
        remplir(vente, produit, quantite, AUJOURD_HUI, SalesStatut.CLOSED, CategorieChiffreAffaire.CA_DEPOT);
        return vente;
    }

    private void remplir(
        Sales vente,
        Produit produit,
        int quantite,
        LocalDate date,
        SalesStatut statut,
        CategorieChiffreAffaire categorie
    ) {
        int montant = quantite * produit.getRegularUnitPrice();
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setSaleDate(date);
        vente.setNumberTransaction("DATA" + vente.getId().getId());
        vente.setStatut(statut);
        vente.setCategorieChiffreAffaire(categorie);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(date.atTime(10, 30));
        vente.setUpdatedAt(date.atTime(10, 30));
        vente.setEffectiveUpdateDate(date.atTime(10, 30));
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        vente.setAmountToBeTakenIntoAccount(montant);
        vente.setUser(caissier);
        vente.setSeller(caissier);
        vente.setCaissier(caissier);
        vente.setMagasin(magasin);
        em.persist(vente);

        SalesLine ligne = new SalesLine();
        ligne.setId(services.salesLineService.getNextId());
        ligne.setSaleDate(date);
        ligne.setSales(vente);
        ligne.setProduit(produit);
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        ligne.setNetUnitPrice(produit.getRegularUnitPrice());
        ligne.setCostAmount(produit.getCostAmount());
        ligne.setSalesAmount(montant);
        ligne.setAmountToBeTakenIntoAccount(montant);
        ligne.setCreatedAt(date.atTime(10, 30));
        ligne.setUpdatedAt(date.atTime(10, 30));
        ligne.setEffectiveUpdateDate(date.atTime(10, 30));
        em.persist(ligne);
        vente.getSalesLines().add(ligne);
        em.flush();
    }

    private Magasin depot(String nom) {
        Magasin depot = new Magasin();
        depot.setName(nom);
        depot.setFullName(nom);
        depot.setTypeMagasin(TypeMagasin.DEPOT);
        em.persist(depot);

        Storage stockage = new Storage();
        stockage.setName("Stock " + nom);
        stockage.setStorageType(StorageType.PRINCIPAL);
        stockage.setMagasin(depot);
        em.persist(stockage);

        depot.setPrimaryStorage(stockage);
        em.flush();
        return depot;
    }
}
