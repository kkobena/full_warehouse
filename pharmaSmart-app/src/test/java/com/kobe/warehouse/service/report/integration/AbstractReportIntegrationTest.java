package com.kobe.warehouse.service.report.integration;

import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
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
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
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
 * Socle des tests d'intégration des rapports.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Les rapports ne lisent presque jamais les tables directement : ils interrogent des <b>vues et
 * vues matérialisées</b> — {@code v_abc_pareto_analysis}, {@code mv_supplier_performance},
 * {@code mv_stock_alerts}… — dont la définition vit dans les migrations, loin du code Java qui s'en
 * sert. Deux choses peuvent alors diverger sans que rien ne le signale : les <b>colonnes</b>
 * attendues par le service et celles que la vue expose, et les <b>filtres</b> de la vue, qu'un
 * jeu d'essai mal choisi croit franchir alors qu'il en est écarté — un produit {@code DETAIL} est
 * invisible pour l'analyse ABC, un fournisseur sans réception l'est pour la performance. Seule une
 * vraie base le montre.
 *
 * <p>Les vues matérialisées ne se mettent pas à jour toutes seules : {@link #rafraichir(String)}
 * les recalcule dans la transaction du test, et le rafraîchissement s'annule avec elle.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractReportIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;

    protected static EntityManager em;

    /** Suffixe unique par fixture : codes, libellés et numéros de lot sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected ReportServicesUnderTest services;
    protected AppUser utilisateur;
    protected Magasin magasin;
    protected Storage rayon;

    protected TransactionIdGeneratorService transactionIdGeneratorService;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        services = new ReportServicesUnderTest(em);
        transactionIdGeneratorService = new TransactionIdGeneratorService(em);

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);

        lenient().when(services.userService.getUser()).thenReturn(utilisateur);
    }

    @AfterEach
    void annulerLaTransaction() {
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    // ===== vues matérialisées =====

    /**
     * Recalcule une vue matérialisée. Sans {@code CONCURRENTLY}, interdit dans une transaction —
     * ce qui tombe bien : c'est ce qui permet au rafraîchissement de s'annuler avec le test.
     */
    protected void rafraichir(String vueMaterialisee) {
        em.flush();
        em.createNativeQuery("REFRESH MATERIALIZED VIEW " + vueMaterialisee).executeUpdate();
    }

    // ===== fabriques du jeu d'essai =====

    protected static String unique(String prefixe) {
        return prefixe + "-" + COMPTEUR.incrementAndGet();
    }

    protected Tva tva(int taux) {
        return em.createQuery("SELECT t FROM Tva t WHERE t.taux = :taux", Tva.class).setParameter("taux", taux).getSingleResult();
    }

    protected FamilleProduit premiereFamille() {
        return em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class).setMaxResults(1).getSingleResult();
    }

    // --- produits ---

    /**
     * Un produit vendable. Le <b>type</b> n'est pas décoratif : l'analyse ABC écarte les produits
     * {@code DETAIL}, si bien qu'un jeu d'essai bâti sur eux rendrait une vue vide sans rien dire.
     */
    protected Produit produit(String libelle, TypeProduit type, int prixVente, int seuilMini) {
        Produit produit = new Produit();
        produit.setLibelle(libelle);
        produit.setTypeProduit(type);
        produit.setStatus(Status.ENABLE);
        produit.setCostAmount(prixVente * 6 / 10);
        produit.setRegularUnitPrice(prixVente);
        produit.setNetUnitPrice(prixVente);
        produit.setItemCostAmount(prixVente * 6 / 10);
        produit.setItemRegularUnitPrice(prixVente);
        produit.setItemQty(1);
        produit.setPrixMnp(0);
        produit.setQtySeuilMini(seuilMini);
        produit.setDeconditionnable(false);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setTva(tva(0));
        produit.setFamille(premiereFamille());
        em.persist(produit);
        em.flush();
        return produit;
    }

    /** Un produit d'analyse : {@code PACKAGE}, donc visible de {@code v_abc_pareto_analysis}. */
    protected Produit produitAnalysable(String libelle, int prixVente) {
        return produit(libelle, TypeProduit.PACKAGE, prixVente, 5);
    }

    protected Fournisseur fournisseur(String libelle) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setLibelle(libelle);
        fournisseur.setCode(unique("FRS"));
        fournisseur.setPhone("0100000000");
        fournisseur.setMobile("0700000000");
        em.persist(fournisseur);
        em.flush();
        return fournisseur;
    }

    /** Le référencement fournisseur porte le code CIP que les rapports affichent. */
    protected FournisseurProduit referencement(Produit produit, Fournisseur fournisseur) {
        FournisseurProduit reference = new FournisseurProduit();
        reference.setProduit(produit);
        reference.setFournisseur(fournisseur);
        reference.setCodeCip(unique("CIP"));
        reference.setPrixAchat(produit.getCostAmount());
        reference.setPrixUni(produit.getRegularUnitPrice());
        em.persist(reference);
        produit.setFournisseurProduitPrincipal(reference);
        produit.getFournisseurProduits().add(reference);
        em.flush();
        return reference;
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

    /**
     * Range un produit dans un rayon. Sans ce rattachement, la valorisation par rayon le classe en
     * rayon 0 — « sans emplacement » — et le filtre par rayon ne le retrouve plus.
     */
    protected Rayon rangeAuRayon(Produit produit, String libelle) {
        Rayon nouveauRayon = new Rayon();
        nouveauRayon.setCode(unique("RAY"));
        nouveauRayon.setLibelle(libelle);
        nouveauRayon.setStorage(rayon);
        nouveauRayon.setExclude(false);
        em.persist(nouveauRayon);
        em.flush();
        return rattacheAuRayon(produit, nouveauRayon);
    }

    protected Rayon rattacheAuRayon(Produit produit, Rayon rayonCible) {
        RayonProduit rattachement = new RayonProduit();
        rattachement.setProduit(produit);
        rattachement.setRayon(rayonCible);
        em.persist(rattachement);
        em.flush();
        return rayonCible;
    }

    protected Lot lot(Produit produit, LocalDate peremption, int quantite) {
        Lot lot = new Lot();
        lot.setNumLot(unique("LOT"));
        lot.setProduit(produit);
        lot.setQuantity(quantite);
        lot.setCurrentQuantity(quantite);
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

    // --- ventes ---

    /** Un client nominatif : sans lui, une vente est anonyme et sort des analyses de fidélité. */
    protected UninsuredCustomer client(String nom, String prenom, String telephone) {
        UninsuredCustomer client = new UninsuredCustomer();
        client.setFirstName(prenom);
        client.setLastName(nom);
        client.setCode(unique("CLI"));
        client.setPhone(telephone);
        client.setTypeAssure(TypeAssure.PRINCIPAL);
        client.setStatus(Status.ENABLE);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        em.persist(client);
        em.flush();
        return client;
    }

    /** Une vente nominative d'un montant donné, sans ligne : les analyses client lisent l'en-tête. */
    protected CashSale achat(UninsuredCustomer client, LocalDate date, int montant) {
        CashSale vente = venteFermee(date, false, CategorieChiffreAffaire.CA, client);
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        em.flush();
        return vente;
    }

    protected CashRegister caisseOuverte() {
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
        caisse.setBeginTime(LocalDateTime.now());
        caisse.setCreated(LocalDateTime.now());
        caisse.setUpdated(LocalDateTime.now());
        caisse.setStatut(CashRegisterStatut.OPEN);
        em.persist(caisse);
        em.flush();
        return caisse;
    }

    /**
     * Une vente comptant clôturée et comptée en chiffre d'affaires. Les trois conditions —
     * {@code CLOSED}, non annulée, {@code ca = 'CA'} — sont celles que les vues d'analyse
     * exigent : en manquer une seule rend la vente invisible aux rapports.
     */
    protected CashSale venteFermee(LocalDate date) {
        return venteFermee(date, false, CategorieChiffreAffaire.CA);
    }

    protected CashSale venteFermee(LocalDate date, boolean annulee, CategorieChiffreAffaire categorie) {
        return venteFermee(date, annulee, categorie, null);
    }

    protected CashSale venteFermee(LocalDate date, boolean annulee, CategorieChiffreAffaire categorie, Customer client) {
        CashSale vente = new CashSale();
        vente.setCustomer(client);
        vente.setSaleDate(date);
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("IT" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCategorieChiffreAffaire(categorie);
        vente.setCanceled(annulee);
        vente.setCreatedAt(date.atTime(LocalTime.of(10, 0).plusSeconds(COMPTEUR.incrementAndGet())));
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(0);
        vente.setNetAmount(0);
        vente.setAmountToBePaid(0);
        vente.setPayrollAmount(0);
        vente.setRestToPay(0);
        vente.setAmountToBeTakenIntoAccount(0);
        vente.setUser(utilisateur);
        vente.setSeller(utilisateur);
        vente.setCaissier(utilisateur);
        vente.setMagasin(magasin);
        em.persist(vente);
        em.flush();
        return vente;
    }

    /**
     * Une vente dépôt clôturée. Elle porte le {@code dtype} {@code VenteDepot} et la catégorie
     * {@code CA_DEPOT} : c'est un transfert vers un dépôt, pas du chiffre d'affaires déclaré, et les
     * synthèses doivent la distinguer plutôt que l'additionner.
     */
    protected VenteDepot venteDepot(LocalDate date, int montant) {
        VenteDepot vente = new VenteDepot();
        vente.setDepot(magasin);
        vente.setSaleDate(date);
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("DEP" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCategorieChiffreAffaire(CategorieChiffreAffaire.CA_DEPOT);
        vente.setCreatedAt(date.atTime(LocalTime.of(10, 0).plusSeconds(COMPTEUR.incrementAndGet())));
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

    /** Vend un produit à une date donnée et remonte le montant sur l'en-tête de la vente. */
    protected SalesLine vendu(Produit produit, int quantite, LocalDate date) {
        return vendu(produit, quantite, date, false, CategorieChiffreAffaire.CA);
    }

    protected SalesLine vendu(Produit produit, int quantite, LocalDate date, boolean annulee, CategorieChiffreAffaire categorie) {
        Sales vente = venteFermee(date, annulee, categorie);
        SalesLine ligne = ligneDeVente(vente, produit, quantite);
        vente.setSalesAmount(ligne.getSalesAmount());
        vente.setNetAmount(ligne.getSalesAmount());
        vente.setAmountToBePaid(ligne.getSalesAmount());
        vente.setPayrollAmount(ligne.getSalesAmount());
        em.flush();
        return ligne;
    }

    protected SalesLine ligneDeVente(Sales vente, Produit produit, int quantite) {
        SalesLine ligne = new SalesLine();
        ligne.setId(services.saleLineIdGeneratorService.nextId());
        ligne.setSaleDate(vente.getSaleDate());
        ligne.setSales(vente);
        ligne.setProduit(produit);
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setQuantityAvoir(0);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        ligne.setNetUnitPrice(produit.getRegularUnitPrice());
        ligne.setCostAmount(produit.getCostAmount());
        ligne.setSalesAmount(quantite * produit.getRegularUnitPrice());
        ligne.setAmountToBeTakenIntoAccount(ligne.getSalesAmount());
        ligne.setCreatedAt(vente.getCreatedAt());
        ligne.setUpdatedAt(LocalDateTime.now());
        ligne.setEffectiveUpdateDate(LocalDateTime.now());
        em.persist(ligne);
        em.flush();
        return ligne;
    }

    // --- tiers payant ---

    protected GroupeTiersPayant groupeTiersPayant(String nom) {
        GroupeTiersPayant groupe = new GroupeTiersPayant();
        groupe.setName(unique(nom));
        em.persist(groupe);
        em.flush();
        return groupe;
    }

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
        em.persist(tiersPayant);
        em.flush();
        return tiersPayant;
    }

    /**
     * Une facture tiers payant et le dossier qu'elle porte. Le montant de la facture n'est pas une
     * colonne mais la <b>somme de ses dossiers</b> : c'est ce que les rapports de créances
     * recalculent, et une facture sans dossier vaut donc zéro.
     */
    protected FactureTiersPayant facture(TiersPayant tiersPayant, LocalDate dateEmission, int montant, int montantRegle, InvoiceStatut statut) {
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
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setSaleDate(dateEmission);
        vente.setNumberTransaction("FA" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.ASSURANCE);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(dateEmission.atTime(LocalTime.of(10, 0)));
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
        facture.setId(services.factureIdGeneratorService.nextId());
        facture.setNumFacture(LocalDate.now().getYear() + "_" + String.format("%04d", COMPTEUR.incrementAndGet()));
        facture.setInvoiceDate(dateEmission);
        facture.setCreated(LocalDateTime.now());
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
        dossier.setId(services.assuranceItemIdGeneratorService.nextId());
        dossier.setSaleDate(dateEmission);
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

    /**
     * Un encaissement rattaché à une vente et à une caisse.
     *
     * <p>Sans lui, aucun test ne pouvait éprouver la vue des modes de règlement : elle agrège
     * {@code payment_transaction}, que rien ici ne savait créer.
     */
    protected SalePayment reglement(Sales vente, CashRegister caisse, String codeMode, int montant) {
        SalePayment paiement = new SalePayment();
        // payment_transaction est partitionnée par date : son identifiant est composite.
        paiement.setId(transactionIdGeneratorService.nextId());
        paiement.setSale(vente);
        paiement.setCashRegister(caisse);
        paiement.setPaymentMode(em.find(PaymentMode.class, codeMode));
        paiement.setPaidAmount(montant);
        paiement.setExpectedAmount(montant);
        paiement.setMontantVerse(montant);
        paiement.setReelAmount(montant);
        paiement.setTransactionDate(vente.getSaleDate());
        paiement.setCreatedAt(vente.getCreatedAt());
        paiement.setCategorieChiffreAffaire(CategorieChiffreAffaire.CA);
        paiement.setTypeFinancialTransaction(TypeFinancialTransaction.CASH_SALE);
        paiement.setCredit(false);
        paiement.setPartAssure(montant);
        paiement.setPartTiersPayant(0);
        em.persist(paiement);
        em.flush();
        return paiement;
    }

    /**
     * Une facture de groupe : elle ne porte pas de lignes, elle totalise ses filles.
     *
     * <p>C'est la forme qui fait qu'une somme naïve sur {@code facture_tiers_payant} compte deux
     * fois les mêmes montants — d'où le filtre {@code groupe_facture_tiers_payant_id IS NULL}
     * que tous les écrans d'encours appliquent.
     */
    protected FactureTiersPayant factureDeGroupe(
        GroupeTiersPayant groupe,
        LocalDate dateEmission,
        FactureTiersPayant... filles
    ) {
        int total = 0;
        for (FactureTiersPayant fille : filles) {
            total += fille.getMontantTtc().intValue();
        }

        FactureTiersPayant groupee = new FactureTiersPayant();
        groupee.setId(services.factureIdGeneratorService.nextId());
        groupee.setNumFacture(unique("GRP"));
        groupee.setInvoiceDate(dateEmission);
        groupee.setCreated(LocalDateTime.now());
        groupee.setUpdated(LocalDateTime.now());
        groupee.setUser(utilisateur);
        groupee.setGroupeTiersPayant(groupe);
        groupee.setFactureProvisoire(false);
        groupee.setStatut(InvoiceStatut.NOT_PAID);
        groupee.setMontantRegle(0);
        groupee.setMontantTtc(BigDecimal.valueOf(total));
        groupee.setMontantTva(BigDecimal.ZERO);
        groupee.setMontantNet(BigDecimal.valueOf(total));
        groupee.setMontantHt(BigDecimal.valueOf(total));
        em.persist(groupee);

        for (FactureTiersPayant fille : filles) {
            fille.setGroupeFactureTiersPayant(groupee);
            em.merge(fille);
        }
        em.flush();
        return groupee;
    }

    // --- ajustements de stock ---

    protected MotifAjustement motif(String libelle) {
        MotifAjustement motif = new MotifAjustement();
        motif.setLibelle(libelle);
        em.persist(motif);
        em.flush();
        return motif;
    }

    /**
     * Un bon d'ajustement clôturé et la ligne qui le compose. Seuls les bons {@code CLOSED} et les
     * lignes {@code AJUSTEMENT_OUT} constituent de la démarque : un ajustement en brouillon n'a rien
     * retiré du stock, et un ajustement entrant est une correction à la hausse.
     */
    protected Ajust demarque(LocalDateTime date, Produit produit, int quantitePerdue, MotifAjustement motif) {
        return ajustement(date, produit, quantitePerdue, motif, AjustType.AJUSTEMENT_OUT, AjustementStatut.CLOSED);
    }

    protected Ajust ajustement(
        LocalDateTime date,
        Produit produit,
        int quantite,
        MotifAjustement motif,
        AjustType type,
        AjustementStatut statut
    ) {
        Ajust ajust = new Ajust();
        ajust.setCommentaire(unique("AJUST"));
        ajust.setUser(utilisateur);
        ajust.setDateMtv(date);
        ajust.setStatut(statut);
        em.persist(ajust);
        em.flush();

        StockProduit stockProduit = produit
            .getStockProduits()
            .stream()
            .findFirst()
            .orElseGet(() -> stock(produit, quantite));

        Ajustement ligne = new Ajustement();
        ligne.setAjust(ajust);
        ligne.setStockProduit(stockProduit);
        ligne.setMotifAjustement(motif);
        ligne.setType(type);
        // Une sortie se note en quantité négative ; le rapport en prend la valeur absolue.
        ligne.setQtyMvt(type == AjustType.AJUSTEMENT_OUT ? -quantite : quantite);
        ligne.setDateMtv(date);
        ligne.setStockBefore(stockProduit.getQtyStock());
        ligne.setStockAfter(stockProduit.getQtyStock() - quantite);
        em.persist(ligne);
        ajust.getAjustements().add(ligne);
        em.flush();
        return ajust;
    }

    // --- commandes et réceptions ---

    /**
     * Une commande réceptionnée. {@code receipt_date} moins {@code order_date} donne le délai de
     * livraison, et le rapport de conformité compare le reçu au demandé — c'est de ces deux écarts
     * que se déduit la note du fournisseur.
     */
    protected Commande reception(
        Fournisseur fournisseur,
        LocalDate dateCommande,
        LocalDate dateReception,
        int montant,
        int quantiteDemandee,
        int quantiteRecue
    ) {
        Commande commande = new Commande();
        commande.setId(services.commandeIdGeneratorService.getNextIdAsInt());
        commande.setOrderDate(dateCommande);
        commande.setReceiptDate(dateReception);
        commande.setOrderReference(unique("CMD"));
        commande.setOrderStatus(OrderStatut.RECEIVED);
        commande.setGrossAmount(montant);
        commande.setOrderAmount(montant);
        commande.setFinalAmount(montant);
        commande.setCreatedAt(dateCommande.atStartOfDay());
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setUser(utilisateur);
        commande.setFournisseur(fournisseur);
        em.persist(commande);
        em.flush();

        Produit produit = produit(unique("PRODUIT COMMANDE"), TypeProduit.PACKAGE, 10_000, 1);
        referencement(produit, fournisseur);

        OrderLine ligne = new OrderLine();
        ligne.setId(services.orderLineIdGeneratorService.getNextIdAsInt());
        ligne.setOrderDate(commande.getOrderDate());
        ligne.setCommande(commande);
        ligne.setFournisseurProduit(produit.getFournisseurProduitPrincipal());
        ligne.setQuantityRequested(quantiteDemandee);
        ligne.setQuantityReceived(quantiteRecue);
        ligne.setInitStock(0);
        ligne.setFinalStock(quantiteRecue);
        ligne.setOrderUnitPrice(10_000);
        ligne.setOrderCostAmount(6_000);
        ligne.setGrossAmount(montant);
        ligne.setOrderAmount(montant);
        ligne.setCreatedAt(LocalDateTime.now());
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        commande.getOrderLines().add(ligne);
        em.flush();
        return commande;
    }

    // ===== outils d'assertion =====

    /** Vide le contexte de persistance : la relecture qui suit vient bien de Postgres. */
    protected void viderLeCache() {
        em.flush();
        em.clear();
    }
}
