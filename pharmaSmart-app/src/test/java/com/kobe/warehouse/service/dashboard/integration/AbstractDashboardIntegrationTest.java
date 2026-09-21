package com.kobe.warehouse.service.dashboard.integration;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.DefaultPayment;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration des tableaux de bord.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Un tableau de bord ne modifie rien — il lit. Son risque n'est donc pas d'écrire de travers
 * mais de <b>compter faux</b>, et ce qui le fait compter faux tient dans la base : une colonne qui
 * ne porte pas le nom qu'on croit, une valeur d'énumération refusée par une contrainte
 * {@code CHECK}, une table partitionnée dont la requête n'atteint pas la partition du jour. Les
 * requêtes de ces écrans étant presque toutes natives, elles échappent à la vérification
 * d'Hibernate : seule leur exécution réelle les met à l'épreuve.
 *
 * <p>Le jeu d'essai est donc construit à la date du jour, puisque c'est ce que ces requêtes
 * interrogent : {@code CURRENT_DATE} pour la caisse, les ventes et les livraisons, une fenêtre
 * glissante de trente ou quatre-vingt-dix jours pour la rotation et le réassort. L'utilisateur
 * {@code system} et le magasin viennent des migrations.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractDashboardIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;
    protected static final String LOGIN = "system";

    protected static EntityManager em;

    /** Suffixe unique par fixture : codes, libellés et numéros de lot sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    /** Les paiements n'ont pas de séquence dédiée : on numérote hors de portée des données de référence. */
    private static final AtomicLong PAIEMENT_ID = new AtomicLong(1_000_000L);

    protected DashboardServicesUnderTest services;
    protected AppUser utilisateur;
    protected Magasin magasin;
    protected Storage rayon;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        services = new DashboardServicesUnderTest(em);

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);

        // Le tableau de bord du préparateur ne lit que la caisse de l'utilisateur connecté :
        // sans contexte de sécurité, toutes ses tuiles reviendraient vides.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(LOGIN, null, java.util.List.of()));
    }

    @AfterEach
    void annulerLaTransaction() {
        SecurityContextHolder.clearContext();
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    // ===== fabriques du jeu d'essai =====

    protected static String unique(String prefixe) {
        return prefixe + "-" + COMPTEUR.incrementAndGet();
    }

    protected Tva tva(int taux) {
        return em.createQuery("SELECT t FROM Tva t WHERE t.taux = :taux", Tva.class).setParameter("taux", taux).getSingleResult();
    }

    protected PaymentMode modePaiement(String code) {
        return em.find(PaymentMode.class, code);
    }

    // --- caisse ---

    /**
     * La date d'aujourd'hui telle que <b>la base</b> la voit.
     *
     * <p>Les horodatages sont stockés en UTC. Passé minuit dans un fuseau en avance sur UTC,
     * {@code LocalDate.now()} désigne déjà le lendemain pour PostgreSQL : les requêtes du tableau de
     * bord, qui filtrent sur {@code DATE(begin_time) = CURRENT_DATE}, ne retrouvent alors plus la
     * caisse qu'on vient d'ouvrir, et la classe entière échoue entre minuit et deux heures du matin.
     * Ancrer les fixtures sur la date de la base rend le résultat indépendant de l'heure d'exécution.
     */
    protected LocalDate aujourdHui() {
        Object date = em.createNativeQuery("SELECT CURRENT_DATE").getSingleResult();
        return date instanceof java.sql.Date jour ? jour.toLocalDate() : LocalDate.parse(date.toString());
    }

    protected CashRegister caisseOuverte(long fondDeCaisse) {
        return caisse(fondDeCaisse, aujourdHui().atTime(8, 0), CashRegisterStatut.OPEN, null);
    }

    protected CashRegister caisse(long fondDeCaisse, LocalDateTime ouverture, CashRegisterStatut statut, LocalDateTime fermeture) {
        CashFund fond = new CashFund();
        fond.setUser(utilisateur);
        fond.setAmount((int) fondDeCaisse);
        fond.setCreated(LocalDateTime.now());
        fond.setUpdated(LocalDateTime.now());
        fond.setCashFundType(CashFundType.AUTO);
        fond.setStatut(CashFundStatut.VALIDETED);
        em.persist(fond);

        CashRegister caisse = new CashRegister();
        caisse.setCashFund(fond);
        caisse.setUser(utilisateur);
        caisse.setInitAmount(fondDeCaisse);
        caisse.setBeginTime(ouverture);
        caisse.setEndTime(fermeture);
        caisse.setCreated(LocalDateTime.now());
        caisse.setUpdated(LocalDateTime.now());
        caisse.setStatut(statut);
        em.persist(caisse);
        em.flush();
        return caisse;
    }

    /**
     * Un encaissement rattaché à la caisse. Le {@code dtype} vaut {@code DefaultPayment} : c'est un
     * mouvement de caisse, donc jamais l'un des types que le tableau de bord exclut.
     */
    protected DefaultPayment encaissement(CashRegister caisse, String codeMode, TypeFinancialTransaction type, int montant) {
        DefaultPayment paiement = new DefaultPayment();
        paiement.setId(PAIEMENT_ID.incrementAndGet());
        paiement.setTransactionDate(LocalDate.now());
        paiement.setCreatedAt(LocalDateTime.now());
        paiement.setPaymentMode(modePaiement(codeMode));
        paiement.setCashRegister(caisse);
        paiement.setTypeFinancialTransaction(type);
        paiement.setExpectedAmount(montant);
        paiement.setPaidAmount(montant);
        paiement.setReelAmount(montant);
        paiement.setMontantVerse(montant);
        paiement.setAmountToBeTakenIntoAccount(montant);
        em.persist(paiement);
        em.flush();
        return paiement;
    }

    // --- ventes ---

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

    protected CashSale venteComptant(CashRegister caisse, int montant) {
        return vente(caisse, NatureVente.COMPTANT, montant, 0, false, null, LocalDate.now());
    }

    protected CashSale venteCarnet(CashRegister caisse, int montant) {
        return vente(caisse, NatureVente.CARNET, montant, 0, false, null, LocalDate.now());
    }

    protected CashSale venteDifferee(CashRegister caisse, int montant, int resteADevoir, UninsuredCustomer client, LocalDate date) {
        return vente(caisse, NatureVente.COMPTANT, montant, resteADevoir, true, client, date);
    }

    protected CashSale vente(
        CashRegister caisse,
        NatureVente nature,
        int montant,
        int resteADevoir,
        boolean differe,
        UninsuredCustomer client,
        LocalDate date
    ) {
        CashSale vente = new CashSale();
        vente.setSaleDate(date);
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("IT" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(resteADevoir > 0 ? PaymentStatus.IMPAYE : PaymentStatus.PAYE);
        vente.setNatureVente(nature);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(date.atTime(heureDansLaJournee()));
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        vente.setRestToPay(resteADevoir);
        vente.setAmountToBeTakenIntoAccount(montant);
        vente.setDiffere(differe);
        vente.setCustomer(client);
        vente.setUser(utilisateur);
        vente.setSeller(utilisateur);
        vente.setCaissier(utilisateur);
        vente.setCashRegister(caisse);
        vente.setMagasin(magasin);
        em.persist(vente);
        em.flush();
        return vente;
    }

    /** Une heure fixe dans la journée : l'ordre des ventes récentes se lit sur {@code created_at}. */
    private java.time.LocalTime heureDansLaJournee() {
        return java.time.LocalTime.of(10, 0).plusSeconds(COMPTEUR.incrementAndGet());
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

    // --- produits, stock et lots ---

    protected Produit produit(String libelle, int seuilMini) {
        Produit produit = new Produit();
        produit.setLibelle(libelle);
        produit.setTypeProduit(TypeProduit.DETAIL);
        produit.setStatus(Status.ENABLE);
        produit.setCostAmount(6_000);
        produit.setRegularUnitPrice(10_000);
        produit.setNetUnitPrice(10_000);
        produit.setItemCostAmount(6_000);
        produit.setItemRegularUnitPrice(10_000);
        produit.setItemQty(1);
        produit.setPrixMnp(0);
        produit.setQtySeuilMini(seuilMini);
        produit.setDeconditionnable(false);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setTva(tva(0));
        produit.setFamille(
            em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class).setMaxResults(1).getSingleResult()
        );
        em.persist(produit);
        em.flush();
        return produit;
    }

    protected Fournisseur fournisseur(String libelle) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setLibelle(libelle);
        fournisseur.setCode(unique("FRS"));
        em.persist(fournisseur);
        em.flush();
        return fournisseur;
    }

    protected FournisseurProduit referencement(Produit produit, Fournisseur fournisseur) {
        FournisseurProduit reference = new FournisseurProduit();
        reference.setProduit(produit);
        reference.setFournisseur(fournisseur);
        reference.setCodeCip(unique("CIP"));
        reference.setPrixAchat(6_000);
        reference.setPrixUni(10_000);
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

    /** Un lot disponible et daté : c'est la péremption qui décide de la tranche affichée. */
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

    // --- commandes ---

    /**
     * Une commande fournisseur du jour. {@code updated_at} au jour même est ce que la requête des
     * livraisons attendues interroge — {@code order_date} ne sert qu'à élaguer les partitions.
     */
    protected Commande commande(Fournisseur fournisseur, OrderStatut statut, int nombreDeLignes) {
        Commande commande = new Commande();
        commande.setId(services.commandeIdGeneratorService.getNextIdAsInt());
        commande.setOrderDate(aujourdHui());
        commande.setOrderReference(unique("CMD"));
        commande.setOrderStatus(statut);
        commande.setGrossAmount(100_000);
        commande.setOrderAmount(150_000);
        commande.setFinalAmount(150_000);
        // « Livraisons du jour » se lit sur updated_at rapporté à CURRENT_DATE : cet horodatage doit
        // donc tomber dans la journée de la base, non dans celle de la machine.
        commande.setCreatedAt(aujourdHui().atTime(9, 0));
        commande.setUpdatedAt(aujourdHui().atTime(12, 0));
        commande.setUser(utilisateur);
        commande.setFournisseur(fournisseur);
        em.persist(commande);
        em.flush();

        for (int i = 0; i < nombreDeLignes; i++) {
            ligneDeCommande(commande);
        }
        return commande;
    }

    protected OrderLine ligneDeCommande(Commande commande) {
        Produit produit = produit(unique("PRODUIT COMMANDE"), 1);
        referencement(produit, commande.getFournisseur());

        OrderLine ligne = new OrderLine();
        ligne.setId(services.orderLineIdGeneratorService.getNextIdAsInt());
        ligne.setOrderDate(commande.getOrderDate());
        ligne.setCommande(commande);
        ligne.setFournisseurProduit(produit.getFournisseurProduitPrincipal());
        ligne.setQuantityRequested(10);
        ligne.setQuantityReceived(0);
        ligne.setInitStock(0);
        ligne.setFinalStock(0);
        ligne.setOrderUnitPrice(10_000);
        ligne.setOrderCostAmount(6_000);
        ligne.setGrossAmount(60_000);
        ligne.setOrderAmount(100_000);
        ligne.setCreatedAt(LocalDateTime.now());
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        commande.getOrderLines().add(ligne);
        em.flush();
        return ligne;
    }

    // ===== outils d'assertion =====

    /** Vide le contexte de persistance : la relecture qui suit vient bien de Postgres. */
    protected void viderLeCache() {
        em.flush();
        em.clear();
    }
}
