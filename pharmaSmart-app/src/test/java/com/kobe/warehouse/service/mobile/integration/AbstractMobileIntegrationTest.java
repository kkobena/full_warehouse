package com.kobe.warehouse.service.mobile.integration;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Ticketing;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration de l'application mobile.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Les repositories mobiles sont écrits en <b>SQL natif</b>, hors de portée de toute
 * vérification
 * à la compilation : un nom de colonne inexistant, une jointure qui multiplie les lignes ou un
 * {@code COUNT} qui ne compte pas la même chose que la liste qu'il annonce ne se voient qu'en
 * exécutant la requête sur un vrai PostgreSQL. C'est l'objet de ces tests.
 *
 * <p>Les compteurs de l'officine portent sur <b>toute</b> la base, jeu de référence compris : on
 * mesure donc systématiquement un écart par rapport à l'état initial plutôt qu'une valeur absolue,
 * ce que fait {@link #ecart(java.util.function.IntSupplier, Runnable)}.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractMobileIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;
    /**
     * Suffixe unique par fixture : codes, libellés et numéros de lot sont uniques en base.
     */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();
    protected static EntityManager em;
    protected AppUser utilisateur;
    protected Magasin magasin;
    protected Storage rayon;

    protected SaleIdGeneratorService saleIdGeneratorService;
    protected FactureIdGeneratorService factureIdGeneratorService;
    protected AssuranceItemIdGeneratorService assuranceItemIdGeneratorService;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(
            IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    /**
     * Mesure un compteur avant puis après l'ajout des fixtures, et rend l'écart. Le jeu de
     * référence livré par Flyway n'est pas vide : compter en absolu rendrait le test dépendant de
     * son contenu.
     */
    protected static int ecart(java.util.function.IntSupplier compteur, Runnable fixtures) {
        int avant = compteur.getAsInt();
        fixtures.run();
        em.flush();
        return compteur.getAsInt() - avant;
    }

    protected static String unique(String prefixe) {
        return prefixe + "-" + COMPTEUR.incrementAndGet();
    }

    // ===== mesure relative =====

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager()
            .getTransaction(new DefaultTransactionDefinition());

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);

        saleIdGeneratorService = new SaleIdGeneratorService(em);
        factureIdGeneratorService = new FactureIdGeneratorService(em);
        assuranceItemIdGeneratorService = new AssuranceItemIdGeneratorService(em);
    }

    @AfterEach
    void annulerLaTransaction() {
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    // ===== produits et stock =====

    protected Tva tva(int taux) {
        return em.createQuery("SELECT t FROM Tva t WHERE t.taux = :taux", Tva.class)
            .setParameter("taux", taux).getSingleResult();
    }

    protected FamilleProduit premiereFamille() {
        return em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class)
            .setMaxResults(1).getSingleResult();
    }

    protected Produit produit(String libelle) {
        Produit produit = new Produit();
        produit.setLibelle(libelle);
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setStatus(Status.ENABLE);
        produit.setCostAmount(600);
        produit.setRegularUnitPrice(1_000);
        produit.setNetUnitPrice(1_000);
        produit.setItemCostAmount(600);
        produit.setItemRegularUnitPrice(1_000);
        produit.setItemQty(1);
        produit.setPrixMnp(0);
        produit.setQtySeuilMini(5);
        produit.setDeconditionnable(false);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setTva(tva(0));
        produit.setFamille(premiereFamille());
        em.persist(produit);
        em.flush();
        return produit;
    }

    /**
     * Un produit et son stock. Sans ligne de stock, il sort du champ des ruptures.
     */
    protected Produit produitEnStock(String libelle, int quantite) {
        Produit produit = produit(libelle);
        stock(produit, quantite);
        return produit;
    }

    protected StockProduit stock(Produit produit, int quantite) {
        StockProduit stock = new StockProduit();
        stock.setProduit(produit);
        stock.setStorage(rayon);
        stock.setQtyStock(quantite);
        stock.setQtyVirtual(quantite);
        stock.setQtyUG(0);
        stock.setCreatedAt(LocalDateTime.now());
        stock.setUpdatedAt(LocalDateTime.now());
        em.persist(stock);
        produit.getStockProduits().add(stock);
        em.flush();
        return stock;
    }

    protected Lot lot(Produit produit, LocalDate peremption, int quantiteRestante) {
        Lot lot = new Lot();
        lot.setNumLot(unique("LOT"));
        lot.setProduit(produit);
        lot.setQuantity(Math.max(quantiteRestante, 1));
        lot.setCurrentQuantity(quantiteRestante);
        lot.setFreeQty(0);
        lot.setExpiryDate(peremption);
        lot.setCreatedDate(LocalDateTime.now());
        lot.setUpdated(LocalDateTime.now());
        lot.setPrixAchat(produit.getCostAmount());
        lot.setPrixUnit(produit.getRegularUnitPrice());
        lot.setStatut(StatutLot.AVAILABLE);
        em.persist(lot);
        em.flush();
        return lot;
    }

    // ===== ventes =====

    /**
     * Une vente comptant clôturée et comptée en chiffre d'affaires. Les trois conditions —
     * {@code CLOSED}, non annulée, {@code ca = 'CA'} — sont celles qu'exigent toutes les requêtes
     * mobiles : en manquer une seule rend la vente invisible.
     */
    protected CashSale venteFermee(LocalDate date, int montant) {
        return venteFermee(date, montant, false, CategorieChiffreAffaire.CA, null);
    }

    protected CashSale venteFermee(LocalDate date, int montant, boolean annulee,
        CategorieChiffreAffaire categorie, Customer client) {
        CashSale vente = new CashSale();
        vente.setCustomer(client);
        vente.setSaleDate(date);
        vente.setId(saleIdGeneratorService.nextId());
        vente.setNumberTransaction("MOB" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCategorieChiffreAffaire(categorie);
        vente.setCanceled(annulee);
        vente.setCreatedAt(
            date.atTime(LocalTime.of(10, 0).plusSeconds(COMPTEUR.incrementAndGet())));
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        vente.setRestToPay(0);
        vente.setAmountToBeTakenIntoAccount(montant);
        vente.setUser(utilisateur);
        vente.setSeller(utilisateur);
        vente.setCaissier(utilisateur);
        vente.setMagasin(magasin);
        em.persist(vente);
        em.flush();
        return vente;
    }

    /**
     * Un client nominatif : sans lui, la vente est anonyme et ne compte pas de client distinct.
     */
    protected UninsuredCustomer client(String nom) {
        UninsuredCustomer client = new UninsuredCustomer();
        client.setFirstName("Client");
        client.setLastName(unique(nom));
        client.setCode(unique("CLI"));
        client.setTypeAssure(TypeAssure.PRINCIPAL);
        client.setStatus(Status.ENABLE);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        em.persist(client);
        em.flush();
        return client;
    }

    // ===== tiers payant =====

    protected GroupeTiersPayant groupeTiersPayant(String nom) {
        GroupeTiersPayant groupe = new GroupeTiersPayant();
        groupe.setName(unique(nom));
        groupe.setTelephone("0102030405");
        em.persist(groupe);
        em.flush();
        return groupe;
    }

    /**
     * Un tiers payant rattaché à un groupe, ou isolé si {@code groupe} est nul.
     */
    protected TiersPayant tiersPayant(String nom, GroupeTiersPayant groupe) {
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setName(unique(nom));
        tiersPayant.setFullName(nom);
        tiersPayant.setStatut(TiersPayantStatut.ACTIF);
        tiersPayant.setCategorie(TiersPayantCategorie.ASSURANCE);
        tiersPayant.setCreated(LocalDateTime.now());
        tiersPayant.setUpdated(LocalDateTime.now());
        tiersPayant.setUser(utilisateur);
        tiersPayant.setGroupeTiersPayant(groupe);
        tiersPayant.setTelephone("0607080910");
        em.persist(tiersPayant);
        em.flush();
        return tiersPayant;
    }

    /**
     * Une facture tiers payant et le dossier qu'elle porte, émise il y a {@code anciennete} jours.
     *
     * <p>Le montant facturé n'est pas une colonne : c'est la somme des dossiers. Une facture sans
     * dossier vaut zéro et n'est jamais en retard de paiement.
     */
    protected FactureTiersPayant factureAgee(TiersPayant tiersPayant, int anciennete, int montant,
        int montantRegle, InvoiceStatut statut) {
        LocalDate emission = LocalDate.now().minusDays(anciennete);

        AssuredCustomer assure = new AssuredCustomer();
        assure.setFirstName("Assuré");
        assure.setLastName(unique("ASSURE"));
        assure.setCode(unique("ASS"));
        assure.setTypeAssure(TypeAssure.PRINCIPAL);
        assure.setStatus(Status.ENABLE);
        assure.setCreatedAt(LocalDateTime.now());
        assure.setUpdatedAt(LocalDateTime.now());
        em.persist(assure);

        ClientTiersPayant compte = new ClientTiersPayant();
        compte.setTiersPayant(tiersPayant);
        compte.setAssuredCustomer(assure);
        compte.setNum(unique("NUM"));
        compte.setTaux(80);
        compte.setPriorite(PrioriteTiersPayant.R0);
        compte.setStatut(TiersPayantStatut.ACTIF);
        compte.setCreated(LocalDateTime.now());
        compte.setUpdated(LocalDateTime.now());
        em.persist(compte);
        em.flush();

        ThirdPartySales vente = new ThirdPartySales();
        vente.setId(saleIdGeneratorService.nextId());
        vente.setSaleDate(emission);
        vente.setNumberTransaction("FA" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.ASSURANCE);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(emission.atTime(LocalTime.of(10, 0)));
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setAmountToBeTakenIntoAccount(montant);
        vente.setUser(utilisateur);
        vente.setSeller(utilisateur);
        vente.setCaissier(utilisateur);
        vente.setMagasin(magasin);
        vente.setCustomer(assure);
        em.persist(vente);
        em.flush();

        FactureTiersPayant facture = new FactureTiersPayant();
        facture.setId(factureIdGeneratorService.nextId());
        facture.setNumFacture(
            LocalDate.now().getYear() + "_" + String.format("%04d", COMPTEUR.incrementAndGet()));
        facture.setInvoiceDate(emission);
        facture.setCreated(emission.atTime(LocalTime.of(10, 0)));
        facture.setUpdated(LocalDateTime.now());
        facture.setUser(utilisateur);
        facture.setTiersPayant(tiersPayant);
        facture.setGroupeTiersPayant(tiersPayant.getGroupeTiersPayant());
        facture.setFactureProvisoire(false);
        facture.setStatut(statut);
        facture.setMontantRegle(montantRegle);
        facture.setMontantTtc(BigDecimal.valueOf(montant));
        facture.setMontantTva(BigDecimal.ZERO);
        facture.setMontantNet(BigDecimal.valueOf(montant));
        facture.setMontantHt(BigDecimal.valueOf(montant));
        em.persist(facture);
        em.flush();

        ThirdPartySaleLine dossier = new ThirdPartySaleLine();
        dossier.setId(assuranceItemIdGeneratorService.nextId());
        dossier.setSaleDate(emission);
        dossier.setSale(vente);
        dossier.setClientTiersPayant(compte);
        dossier.setNumBon(unique("BON"));
        dossier.setMontant(montant);
        dossier.setMontantRegle(montantRegle);
        dossier.setTaux((short) 80);
        dossier.setTauxVente((short) 100);
        dossier.setStatut(ThirdPartySaleStatut.ACTIF);
        dossier.setCreated(LocalDateTime.now());
        dossier.setUpdated(LocalDateTime.now());
        dossier.setEffectiveUpdateDate(LocalDateTime.now());
        dossier.setFactureTiersPayant(facture);
        em.persist(dossier);
        facture.getFacturesDetails().add(dossier);
        em.flush();
        return facture;
    }

    // ===== caisse =====

    /**
     * Une caisse fermée et son comptage. L'écart de caisse est la différence entre ce que le
     * caissier a compté ({@code ticketing.totalamount}) et ce que la caisse annonce
     * ({@code final_amount}).
     */
    protected CashRegister caisseFermee(LocalDate jour, long montantAnnonce, long montantCompte) {
        CashFund fond = new CashFund();
        fond.setUser(utilisateur);
        fond.setAmount(0);
        fond.setCreated(LocalDateTime.now());
        fond.setUpdated(LocalDateTime.now());
        fond.setCashFundType(CashFundType.AUTO);
        fond.setStatut(CashFundStatut.VALIDETED);
        em.persist(fond);

        CashRegister caisse = new CashRegister();
        caisse.setCashFund(fond);
        caisse.setUser(utilisateur);
        caisse.setInitAmount(0L);
        caisse.setBeginTime(jour.atTime(LocalTime.of(8, 0)));
        caisse.setEndTime(jour.atTime(LocalTime.of(19, 0)));
        caisse.setFinalAmount(montantAnnonce);
        caisse.setCreated(LocalDateTime.now());
        caisse.setUpdated(LocalDateTime.now());
        caisse.setStatut(CashRegisterStatut.CLOSED);
        em.persist(caisse);
        em.flush();

        Ticketing comptage = new Ticketing();
        comptage.setCashRegister(caisse);
        comptage.setTotalAmount(montantCompte);
        comptage.setCreated(jour.atTime(LocalTime.of(19, 0)));
        em.persist(comptage);
        em.flush();
        return caisse;
    }
}
