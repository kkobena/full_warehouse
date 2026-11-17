package com.kobe.warehouse.service.scheduler.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.SemoisSuggestionView;
import com.kobe.warehouse.domain.SemoisSuggestionViewId;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.SemoisSuggestionViewRepository;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * La dette client est retranchée du stock qui couvre la demande.
 *
 * <p>Le calcul comparait le stock objectif au stock <b>physique</b>. Or une partie de ce stock peut
 * être déjà promise : un produit servi partiellement au comptoir laisse un avoir client, et les
 * unités correspondantes sont dans le rayon mais réservées. Les compter comme disponibles
 * surestimait la couverture d'exactement la dette, et le réassort arrivait trop tard — pour un
 * client qui avait déjà payé.
 *
 * <p><b>Retrancher du stock, et non ajouter au résultat.</b> Les deux formes ne sont pas
 * équivalentes, et c'est ce que vérifie {@link #stockAuDessusDeLObjectif()} :
 *
 * <pre>
 *   stock 2,  objectif 10, dette 5  ->  retrancher : 13   ajouter : 13
 *   stock 12, objectif 10, dette 5  ->  retrancher :  3   ajouter :  5
 *   stock 30, objectif 10, dette 5  ->  retrancher :  0   ajouter :  5
 * </pre>
 *
 * <p>Elles coïncident tant que le stock est sous l'objectif, et l'addition surcommande partout
 * ailleurs. La dette ne crée pas un besoin supplémentaire : elle réduit ce dont on dispose.
 *
 * <p>Comme pour la quarantaine ({@code SemoisStockActuelIntegrationTest}), la règle vit à deux
 * endroits — la vue {@code v_semois_suggestion} et {@code SemoisCalculationService} — et les deux
 * doivent rendre le même nombre. La vue n'étant interrogeable que sur une vraie base, seul
 * PostgreSQL met son SQL à l'épreuve.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("SEMOIS — la dette client réduit le stock disponible")
class SemoisDetteClientIntegrationTest {

    private static final int MAGASIN_ID = 1;

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    private static EntityManager em;
    private static SemoisSuggestionViewRepository vueRepository;
    private static AvoirClientRepository avoirClientRepository;

    private TransactionStatus transaction;
    private AppUser utilisateur;
    private Magasin magasin;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(
            IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
        vueRepository = IntegrationPostgresDatabase.bean(SemoisSuggestionViewRepository.class);
        avoirClientRepository = IntegrationPostgresDatabase.bean(AvoirClientRepository.class);
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager()
            .getTransaction(new DefaultTransactionDefinition());
        magasin = em.find(Magasin.class, MAGASIN_ID);
        utilisateur = em.createQuery("SELECT u FROM AppUser u ORDER BY u.id", AppUser.class)
            .setMaxResults(1)
            .getSingleResult();
    }

    @AfterEach
    void annulerLaTransaction() {
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    @Test
    @DisplayName("sans dette, la quantité à commander est inchangée")
    void sansDette() {
        // Garde-fou de non-régression : le correctif ne doit rien changer là où rien n'est promis.
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        stock(produit, StorageType.PRINCIPAL, 12, 0);

        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        assertThat(vue.getStockActuel()).isEqualTo(12L);
        assertThat(vue.getDetteClient()).isZero();
        assertThat(vue.getQuantiteACommander()).isEqualTo(18L);
    }

    @Test
    @DisplayName("la dette s'ajoute à la quantité quand le stock est sous l'objectif")
    void detteSousLObjectif() {
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        stock(produit, StorageType.PRINCIPAL, 12, 0);
        avoir(produit, client(), 5, 0);

        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        // Disponible = 12 − 5 = 7. Objectif 30 → 23, soit 5 de plus que sans dette.
        assertThat(vue.getDetteClient()).isEqualTo(5L);
        assertThat(vue.getQuantiteACommander()).isEqualTo(23L);
    }

    @Test
    @DisplayName("stock au-dessus de l'objectif : la dette ne déclenche pas de commande inutile")
    void stockAuDessusDeLObjectif() {
        // LE cas qui départage les deux formules. Trente unités en rayon pour un objectif de dix,
        // cinq promises : il reste vingt-cinq libres, soit deux fois et demie l'objectif. Ajouter
        // la dette au résultat ferait commander 5 boîtes dont personne n'a besoin.
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 4, 2, 10);
        stock(produit, StorageType.PRINCIPAL, 30, 0);
        avoir(produit, client(), 5, 0);

        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        assertThat(vue.getStockActuel()).isEqualTo(30L);
        assertThat(vue.getDetteClient()).isEqualTo(5L);
        assertThat(vue.getQuantiteACommander()).isZero();
    }

    @Test
    @DisplayName("la dette fait franchir l'objectif sans le dépasser")
    void detteFaitBasculerAutourDeLObjectif() {
        // Stock 12, objectif 10, dette 5 : disponible 7, il manque 3 — pas 5.
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 4, 2, 10);
        stock(produit, StorageType.PRINCIPAL, 12, 0);
        avoir(produit, client(), 5, 0);

        assertThat(lireLaVue(produit).orElseThrow().getQuantiteACommander()).isEqualTo(3L);
    }

    @Test
    @DisplayName("un stock négatif et une dette se cumulent sans double compte")
    void stockNegatifEtDette() {
        // Une vente forcée sort la quantité demandée : le reste à servir vaut zéro et aucun avoir
        // n'est créé. Le négatif du stock et la dette d'avoir viennent donc toujours de ventes
        // DIFFÉRENTES, et s'additionnent légitimement.
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 2, 1, 4);
        stock(produit, StorageType.PRINCIPAL, -2, 0);
        avoir(produit, client(), 2, 0);

        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        // Disponible = −2 − 2 = −4. Objectif 4 → 8.
        assertThat(vue.getStockActuel()).isEqualTo(-2L);
        assertThat(vue.getQuantiteACommander()).isEqualTo(8L);
    }

    @Test
    @DisplayName("seul le solde restant dû compte, pas la quantité d'origine")
    void soldeRestantDu() {
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        stock(produit, StorageType.PRINCIPAL, 12, 0);
        avoir(produit, client(), 8, 6);

        // 8 promises, 6 déjà remises : il en reste 2.
        assertThat(lireLaVue(produit).orElseThrow().getDetteClient()).isEqualTo(2L);
        assertThat(lireLaVue(produit).orElseThrow().getQuantiteACommander()).isEqualTo(20L);
    }

    @Test
    @DisplayName("un avoir clôturé ne pèse plus sur le calcul")
    void avoirCloture() {
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        stock(produit, StorageType.PRINCIPAL, 12, 0);
        AvoirClient avoir = avoir(produit, client(), 5, 0);
        avoir.setStatut(AvoirClientStatut.CLOTURE);
        em.flush();

        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        assertThat(vue.getDetteClient()).isZero();
        assertThat(vue.getQuantiteACommander()).isEqualTo(18L);
    }

    @Test
    @DisplayName("un avoir au porteur pèse autant qu'un avoir nominatif")
    void avoirAuPorteur() {
        // La promesse a été faite ; que le client soit identifié ou non ne change rien au fait que
        // les unités sont réservées.
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        stock(produit, StorageType.PRINCIPAL, 12, 0);
        avoir(produit, null, 4, 0);

        assertThat(lireLaVue(produit).orElseThrow().getDetteClient()).isEqualTo(4L);
    }

    @Test
    @DisplayName("la vue et le calcul Java lisent la même dette")
    void vueEtCalculDAccord() {
        // La règle vit à deux endroits. Si l'un des deux l'oubliait, la liste des suggestions et
        // le détail d'un produit ne commanderaient pas la même chose, sans que rien ne le signale.
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        stock(produit, StorageType.PRINCIPAL, 12, 0);
        avoir(produit, client(), 3, 0);
        avoir(produit, client(), 4, 1);

        long detteJava = avoirClientRepository.lireDetteOuverte(produit.getId());
        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        assertThat(detteJava).isEqualTo(6L);
        assertThat(vue.getDetteClient()).isEqualTo(detteJava);
    }

    // ===== jeu d'essai =====

    private Optional<SemoisSuggestionView> lireLaVue(Produit produit) {
        em.flush();
        em.clear();
        return vueRepository.findById(new SemoisSuggestionViewId(produit.getId(), MAGASIN_ID));
    }

    private String unique(String prefixe) {
        return prefixe + "-" + COMPTEUR.incrementAndGet();
    }

    private UninsuredCustomer client() {
        UninsuredCustomer client = new UninsuredCustomer();
        client.setFirstName("Ama");
        client.setLastName("KOUAME");
        client.setCode(unique("CLI"));
        client.setTypeAssure(TypeAssure.PRINCIPAL);
        client.setStatus(Status.ENABLE);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        em.persist(client);
        em.flush();
        return client;
    }

    private AvoirClient avoir(Produit produit, UninsuredCustomer client, int quantite, int quantiteRemise) {
        AvoirClient avoir = new AvoirClient();
        avoir.setReference(unique("AV"));
        avoir.setProduit(produit);
        avoir.setCustomer(client);
        avoir.setQuantite(quantite);
        avoir.setQuantiteRemise(quantiteRemise);
        avoir.setMontant(quantite * 1_000);
        avoir.setStatut(AvoirClientStatut.OUVERT);
        avoir.setCreatedAt(LocalDateTime.now().minusDays(2));
        avoir.setCreatedBy(utilisateur);
        em.persist(avoir);
        em.flush();
        return avoir;
    }

    /** La vue écarte les {@code DETAIL} : ils relèvent du déconditionnement, pas du grossiste. */
    private Produit produitSuggerable() {
        Produit produit = new Produit();
        produit.setLibelle("PRODUIT " + unique("DETTE"));
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setStatus(Status.ENABLE);
        produit.setClasseCriticite(ClasseCriticite.B);
        produit.setCostAmount(6_000);
        produit.setRegularUnitPrice(10_000);
        produit.setNetUnitPrice(10_000);
        produit.setItemCostAmount(6_000);
        produit.setItemRegularUnitPrice(10_000);
        produit.setItemQty(1);
        produit.setPrixMnp(0);
        produit.setDeconditionnable(false);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setTva(em.createQuery("SELECT t FROM Tva t WHERE t.taux = 0", Tva.class).getSingleResult());
        produit.setFamille(
            em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class)
                .setMaxResults(1)
                .getSingleResult()
        );
        em.persist(produit);
        em.flush();

        Fournisseur fournisseur = new Fournisseur();
        String code = unique("FRS");
        fournisseur.setLibelle("FOURNISSEUR " + code);
        fournisseur.setCode(code);
        em.persist(fournisseur);

        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setProduit(produit);
        fournisseurProduit.setFournisseur(fournisseur);
        fournisseurProduit.setCodeCip(unique("CIP"));
        fournisseurProduit.setPrixAchat(6_000);
        fournisseurProduit.setPrixUni(10_000);
        em.persist(fournisseurProduit);
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        produit.getFournisseurProduits().add(fournisseurProduit);
        em.flush();
        return produit;
    }

    private void semoisConfiguration(Produit produit, int vmm, int margeSecurite, int stockObjectif) {
        SemoisConfiguration configuration = new SemoisConfiguration();
        configuration.setProduit(produit);
        configuration.setMagasin(magasin);
        configuration.setClasseCriticite(ClasseCriticite.B);
        configuration.updateCalculs(vmm, margeSecurite, stockObjectif);
        em.persist(configuration);
        em.flush();
    }

    private void stock(Produit produit, StorageType type, int quantite, int unitesGratuites) {
        Storage storage = em
            .createQuery("SELECT s FROM Storage s WHERE s.magasin.id = :magasin AND s.storageType = :type", Storage.class)
            .setParameter("magasin", MAGASIN_ID)
            .setParameter("type", type)
            .getSingleResult();

        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite + unitesGratuites);
        stockProduit.setQtyUG(unitesGratuites);
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        em.persist(stockProduit);
        produit.getStockProduits().add(stockProduit);
        em.flush();
    }
}
