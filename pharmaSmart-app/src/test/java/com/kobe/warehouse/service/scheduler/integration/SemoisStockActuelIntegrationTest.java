package com.kobe.warehouse.service.scheduler.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.SemoisSuggestionViewRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
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
 * Le stock retranché du stock objectif décide de ce qu'on commande, et il est calculé à deux
 * endroits : par la vue {@code v_semois_suggestion} pour la liste des suggestions, et par
 * {@code SemoisCalculationService.getStockActuel} pour le détail et le repli. Les deux doivent
 * rendre le même nombre, sans quoi l'écran et le calcul ne parlent pas du même stock.
 *
 * <p>Ce qui s'y joue n'est pas de l'arithmétique mais une règle de gestion : <b>la quarantaine ne
 * couvre aucune demande</b>. Son stock attend un retour fournisseur, une destruction ou un
 * contrôle ; le compter ferait croire à une couverture qui n'existe pas et retarderait la commande
 * d'autant. Le rayon, la réserve et l'armoire à toxiques comptent — cette dernière est vendable et
 * sert les ordonnances de stupéfiants et de liste I/II, l'exclure ferait surcommander précisément
 * les produits qu'on ne veut pas en excédent.
 *
 * <p>Le test vaut par son jeu d'essai : une quarantaine <b>plus grosse que tout le reste</b>. Si
 * l'un des deux chemins la comptait, la quantité à commander tomberait à zéro et l'écart serait
 * visible plutôt que silencieux.
 *
 * <p>La vue n'étant interrogeable que sur une vraie base, Hibernate ne peut rien dire de son SQL :
 * seul PostgreSQL le met à l'épreuve.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("SEMOIS — le stock actuel exclut la quarantaine, vue et calcul d'accord")
class SemoisStockActuelIntegrationTest {

    private static final int MAGASIN_ID = 1;

    /** Suffixe unique par fixture : codes CIP et libellés sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    private static EntityManager em;
    private static StockProduitRepository stockProduitRepository;
    private static SemoisSuggestionViewRepository vueRepository;

    private TransactionStatus transaction;
    private Magasin magasin;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
        stockProduitRepository = IntegrationPostgresDatabase.bean(StockProduitRepository.class);
        vueRepository = IntegrationPostgresDatabase.bean(SemoisSuggestionViewRepository.class);
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        magasin = em.find(Magasin.class, MAGASIN_ID);
    }

    @AfterEach
    void annulerLaTransaction() {
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    @Test
    @DisplayName("la vue et le calcul Java rendent le même stock, quarantaine exclue")
    void vueEtCalculDAccord() {
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        poserLeStockDEssai(produit);

        // 4 + 1 UG (rayon) + 6 (réserve) + 2 (toxiques) = 13. Les 100 unités en quarantaine
        // n'entrent pas : elles ne serviront aucune ordonnance.
        int attendu = 13;

        Integer calculJava = stockProduitRepository.findTotalQuantityCouvrantLaDemande(
            MAGASIN_ID,
            produit.getId(),
            StorageType.listerTypesCouvrantLaDemande()
        );
        Optional<SemoisSuggestionView> vue = lireLaVue(produit);

        assertThat(calculJava).isEqualTo(attendu);
        assertThat(vue).isPresent();
        assertThat(vue.orElseThrow().getStockActuel()).isEqualTo(attendu);
        assertThat(vue.orElseThrow().getStockActuel()).isEqualTo(calculJava.longValue());
    }

    /** Le stock objectif est à 30 : il reste 17 unités à commander, pas zéro. */
    @Test
    @DisplayName("la quarantaine ne fait pas tomber la quantité à commander à zéro")
    void quarantaineNEteintPasLaCommande() {
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        poserLeStockDEssai(produit);

        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        assertThat(vue.getQuantiteACommander()).isEqualTo(17L);
    }

    /**
     * La lecture « tous emplacements » reste nécessaire ailleurs — l'inventaire la veut pour
     * compter ce qui est physiquement là, et la clôture d'un avoir client pour savoir si la dette
     * est couverte. Le correctif ne devait pas la déplacer.
     */
    @Test
    @DisplayName("la lecture tous emplacements continue de compter la quarantaine")
    void lectureTousEmplacementsInchangee() {
        Produit produit = produitSuggerable();
        poserLeStockDEssai(produit);

        Integer tousEmplacements = stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(MAGASIN_ID, produit.getId());

        assertThat(tousEmplacements).isEqualTo(113);
    }

    /**
     * Le stock d'un dépôt est bien réel, mais il est ailleurs : il ne sert pas le comptoir de
     * l'officine et n'a donc rien à faire dans son réassort. La vue le comptait pourtant, faute de
     * filtrer sur le magasin, là où le chemin Java filtrait — le désaccord valait exactement le
     * stock des autres magasins.
     *
     * <p>Comme pour la quarantaine, le jeu d'essai est volontairement écrasant : 500 unités au
     * dépôt contre 13 à l'officine. Si l'un des deux chemins les comptait, la quantité à commander
     * tomberait à zéro.
     */
    @Test
    @DisplayName("le stock d'un dépôt ne compte pas dans le réassort de l'officine")
    void stockDUnDepotNeCompte() {
        Produit produit = produitSuggerable();
        semoisConfiguration(produit, 10, 5, 30);
        poserLeStockDEssai(produit);
        stock(produit, emplacementDUnDepot(), 500, 0);
        em.flush();

        Integer calculJava = stockProduitRepository.findTotalQuantityCouvrantLaDemande(
            MAGASIN_ID,
            produit.getId(),
            StorageType.listerTypesCouvrantLaDemande()
        );
        SemoisSuggestionView vue = lireLaVue(produit).orElseThrow();

        assertThat(calculJava).isEqualTo(13);
        assertThat(vue.getStockActuel()).isEqualTo(13L);
        assertThat(vue.getStockActuel()).isEqualTo(calculJava.longValue());
        assertThat(vue.getQuantiteACommander()).isEqualTo(17L);
    }

    @Test
    @DisplayName("seule la quarantaine est écartée : rayon, réserve et toxiques couvrent la demande")
    void regleDesEmplacements() {
        assertThat(StorageType.listerTypesCouvrantLaDemande())
            .containsExactlyInAnyOrder(StorageType.PRINCIPAL, StorageType.SAFETY_STOCK, StorageType.TOXIQUE)
            .doesNotContain(StorageType.QUARANTAINE);
    }

    // ===== jeu d'essai =====

    /** Rayon 4 (+1 UG), réserve 6, toxiques 2, quarantaine 100. */
    private void poserLeStockDEssai(Produit produit) {
        stock(produit, emplacement(StorageType.PRINCIPAL), 4, 1);
        stock(produit, emplacement(StorageType.SAFETY_STOCK), 6, 0);
        stock(produit, emplacement(StorageType.TOXIQUE), 2, 0);
        stock(produit, emplacement(StorageType.QUARANTAINE), 100, 0);
        em.flush();
    }

    /**
     * La vue n'est pas transactionnelle du point de vue d'Hibernate : sans vidage du contexte,
     * elle lirait un état antérieur aux insertions du test.
     */
    private Optional<SemoisSuggestionView> lireLaVue(Produit produit) {
        em.flush();
        em.clear();
        return vueRepository.findById(new SemoisSuggestionViewId(produit.getId(), MAGASIN_ID));
    }

    private String unique(String prefixe) {
        return prefixe + "-" + COMPTEUR.incrementAndGet();
    }

    private Storage emplacement(StorageType type) {
        return em
            .createQuery("SELECT s FROM Storage s WHERE s.magasin.id = :magasin AND s.storageType = :type", Storage.class)
            .setParameter("magasin", MAGASIN_ID)
            .setParameter("type", type)
            .getSingleResult();
    }

    /**
     * Un produit que la vue accepte de rendre : actif, et de type {@code PACKAGE} — la vue écarte
     * les {@code DETAIL}, qui relèvent du déconditionnement et non de la commande au grossiste.
     */
    private Produit produitSuggerable() {
        Produit produit = new Produit();
        produit.setLibelle("PRODUIT " + unique("SEMOIS"));
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
            em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class).setMaxResults(1).getSingleResult()
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

    /** La vue ne rend un produit que si sa VMM calculée est strictement positive. */
    private void semoisConfiguration(Produit produit, int vmm, int margeSecurite, int stockObjectif) {
        SemoisConfiguration configuration = new SemoisConfiguration();
        configuration.setProduit(produit);
        configuration.setMagasin(magasin);
        configuration.setClasseCriticite(ClasseCriticite.B);
        configuration.updateCalculs(vmm, margeSecurite, stockObjectif);
        em.persist(configuration);
        em.flush();
    }

    /** Un dépôt et son emplacement principal, pour y poser du stock qui ne doit pas compter. */
    private Storage emplacementDUnDepot() {
        String nom = unique("DEPOT");
        Magasin depot = new Magasin();
        depot.setName(nom);
        depot.setFullName("DEPOT " + nom);
        depot.setTypeMagasin(TypeMagasin.DEPOT);
        em.persist(depot);

        Storage storage = new Storage();
        storage.setName(unique("RAYON DEPOT"));
        storage.setStorageType(StorageType.PRINCIPAL);
        storage.setMagasin(depot);
        em.persist(storage);
        em.flush();
        return storage;
    }

    private void stock(Produit produit, Storage storage, int quantite, int unitesGratuites) {
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
    }
}
