package com.kobe.warehouse.service.declaration_ca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.service.declaration_ca.dto.JournalExclusionDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalExclusionParamDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalLigneDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalVenteDTO;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.CaPeriodeRepository;
import com.kobe.warehouse.repository.JournalExclusionRepository;
import com.kobe.warehouse.service.StorageService;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Les trois journaux d'exclusion, sur un PostgreSQL réel.
 *
 * <p>Tout est en SQL — jointures, agrégats, filtre de recherche — et c'est justement ce que le test
 * doit éprouver. Un repository simulé validerait la signature des méthodes sans jamais toucher la
 * seule chose qui puisse être fausse : la requête.
 *
 * <p>Le jeu d'essai contient délibérément une ligne <strong>non</strong> retraitée. Un journal qui
 * remonte tout est indiscernable d'un journal juste tant qu'on ne lui donne pas l'occasion de se
 * tromper.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Journaux d'exclusion — périmètre, mesures et filtres")
class JournalExclusionServiceTest {

    private static final String SCHEMA = "pharma_smart";
    private static final LocalDate DEBUT = LocalDate.of(2026, 5, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 5, 31);
    private static final LocalDate JOUR = LocalDate.of(2026, 5, 12);
    private static final int MAGASIN = 1;

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("journal_test");

    private static Connection connection;
    private static EntityManagerFactory entityManagerFactory;
    private static JpaRepositoryFactory fabriqueDeDepots;
    private static JournalExclusionService service;
    private static CaPeriodeRepository caPeriodeRepository;

    @BeforeAll
    static void preparer() throws SQLException {
        Flyway
            .configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas(SCHEMA)
            .defaultSchema(SCHEMA)
            .table("pharma_smart_history")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        try (Statement st = connection.createStatement()) {
            st.execute("SET search_path TO " + SCHEMA);
            jeuDEssai(st);
        }

        service = new JournalExclusionService(repository(dataSource()), storageService());
        caPeriodeRepository = fabriqueDeDepots.getRepository(CaPeriodeRepository.class);
    }

    private static DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        String url = POSTGRES.getJdbcUrl();
        // L'URL de Testcontainers porte déjà des paramètres : un second « ? » la rendrait invalide.
        dataSource.setUrl(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    /**
     * Un vrai {@code EntityManagerFactory} sur le domaine complet, sans contexte Spring.
     *
     * <p>C'est ce qui donne sa valeur au test : les requêtes du repository sont du JPQL, et le JPQL
     * n'existe qu'une fois les entités assemblées. Un dépôt simulé aurait validé des signatures
     * Java, jamais la requête — or c'est elle, et elle seule, qui peut être fausse.
     */
    private static JournalExclusionRepository repository(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean fabrique = new LocalContainerEntityManagerFactoryBean();
        fabrique.setDataSource(dataSource);
        fabrique.setPackagesToScan("com.kobe.warehouse.domain");
        fabrique.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        fabrique.setJpaPropertyMap(
            Map.of(
                "hibernate.hbm2ddl.auto", "none",
                "hibernate.default_schema", SCHEMA,
                "hibernate.jdbc.time_zone", "UTC",
                // Le cache de second niveau n'apporte rien ici et exigerait sa propre configuration.
                "hibernate.cache.use_second_level_cache", "false",
                "hibernate.cache.use_query_cache", "false"
            )
        );
        fabrique.afterPropertiesSet();
        entityManagerFactory = fabrique.getObject();
        fabriqueDeDepots = new JpaRepositoryFactory(entityManagerFactory.createEntityManager());
        return fabriqueDeDepots.getRepository(JournalExclusionRepository.class);
    }

    /** Le dépôt principal de l'utilisateur : il décide du rayon lu et borne les journaux à son magasin. */
    private static StorageService storageService() {
        Magasin magasin = new Magasin();
        magasin.setId(MAGASIN);
        Storage principal = new Storage();
        principal.setId(EntityConstant.DEFAULT_STORAGE);
        principal.setMagasin(magasin);
        StorageService storageService = Mockito.mock(StorageService.class);
        Mockito.when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(principal);
        return storageService;
    }

    @AfterAll
    static void fermer() throws SQLException {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Quatre lignes réparties sur trois ventes :
     *
     * <pre>
     *   vente 1 (comptant)  DOLIPRANE   10 × 100, dont 2 UG   → motif UG    : 200 retirés sur 1 000
     *   vente 1 (comptant)  SHAMPOING    5 × 200, rayon exclu → motif RAYON : 1 000 retirés sur 1 000
     *   vente 2 (comptant)  DOLIPRANE    3 × 100, intacte     → aucun motif : absente des journaux
     *   vente 3 (assurance) DOLIPRANE    4 × 100, TP exclu    → motif TIERS_PAYANT
     * </pre>
     *
     * Le coût unitaire vaut 60 pour DOLIPRANE et 150 pour SHAMPOING : la marge se distingue ainsi du
     * montant retiré, qu'un test aurait sinon toutes les chances de confondre.
     */
    private static void jeuDEssai(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS sales_2026 PARTITION OF sales FOR VALUES FROM ('2026-01-01') TO ('2027-01-01')");
        st.execute(
            "CREATE TABLE IF NOT EXISTS sales_line_2026 PARTITION OF sales_line FOR VALUES FROM ('2026-01-01') TO ('2027-01-01')"
        );
        st.execute(
            "CREATE TABLE IF NOT EXISTS third_party_sale_line_2026 PARTITION OF third_party_sale_line " +
            "FOR VALUES FROM ('2026-01-01') TO ('2027-01-01')"
        );

        produit(st, 960_001, "DOLIPRANE 1000", "3400930000011", 60);
        produit(st, 960_002, "SHAMPOING DOUX", "3400930000028", 150);

        // Le rayon exclu, et son rattachement au seul SHAMPOING.
        st.executeUpdate("INSERT INTO rayon (id, code, to_exclude, libelle, storage_id) VALUES (960001, 'PARA', true, 'PARAPHARMACIE', 1)");
        st.executeUpdate("INSERT INTO rayon_produit (produit_id, rayon_id) VALUES (960002, 960001)");

        // La chaîne du tiers-payant : assuré, tiers-payant exclu, rattachement.
        st.executeUpdate(
            """
            INSERT INTO customer (dtype, id, code, first_name, last_name, status, type_assure, created_at, updated_at)
            VALUES ('AssuredCustomer', 960001, 'CLI960001', 'AWA', 'KONE', 'ENABLE', 'PRINCIPAL', now(), now())
            """
        );
        st.executeUpdate(
            """
            INSERT INTO tiers_payant (id, categorie, created, updated, full_name, name, statut, to_be_exclude, user_id)
            VALUES (960001, 'ASSURANCE', now(), now(), 'MUTUELLE GENERALE', 'MUGEN', 'ACTIF', true, 1)
            """
        );
        st.executeUpdate(
            """
            INSERT INTO client_tiers_payant (id, created, updated, num, priorite, statut, taux,
                                             assured_customer_id, tierspayant_id)
            VALUES (960001, now(), now(), 'ADH-01', 'R0', 'ACTIF', 80, 960001, 960001)
            """
        );

        vente(st, 960_001, "CashSale", null);
        ligne(st, 9_600_011, 960_001, 960_001, 10, 100, 2, 800, "UG");
        ligne(st, 9_600_012, 960_001, 960_002, 5, 200, 0, 0, "RAYON");

        vente(st, 960_002, "CashSale", null);
        ligne(st, 9_600_021, 960_002, 960_001, 3, 100, 0, 300, null);

        vente(st, 960_003, "ThirdPartySales", 960_001);
        ligne(st, 9_600_031, 960_003, 960_001, 4, 100, 0, 0, "TIERS_PAYANT");
    }

    private static void produit(Statement st, int id, String libelle, String ean, int cout) throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO produit (id, libelle, code_ean_labo, status, type_produit, deconditionnable,
                                 cost_amount, regular_unit_price, net_unit_price, item_cost_amount,
                                 item_qty, item_regular_unit_price, created_at, updated_at, famille_id, tva_id)
            SELECT %d, '%s', '%s', 'ENABLE', 'DETAIL', false, %d, 100, 100, %d, 1, 100, now(), now(),
                   (SELECT min(id) FROM famille_produit), (SELECT id FROM tva WHERE taux = 0)
            """.formatted(id, libelle, ean, cout, cout)
        );
    }

    /** Une vente close du 12 mai. {@code clientTiersPayantId} non nul y ajoute le bon d'assurance. */
    private static void vente(Statement st, long id, String type, Integer clientTiersPayantId) throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO sales (dtype, id, sale_date, number_transaction, statut, ca, sales_amount,
                               amount_to_be_paid, amount_to_be_taken_into_account, discount_amount,
                               payroll_amount, rest_to_pay, monnaie, canceled, copy, imported, differe,
                               to_ignore, nature_vente, origine_vente, payment_status, type_prescription,
                               created_at, updated_at, effective_update_date, user_id, seller_id,
                               caissier_id, magasin_id, customer_id%s)
            VALUES ('%s', %d, DATE '2026-05-12', 'JRN-%d', 'CLOSED', 'CA', 0,
                    0, 0, 0, 0, 0, 0, false, false, false, false,
                    false, 'COMPTANT', 'DIRECT', 'PAYE', 'PRESCRIPTION',
                    now(), now(), now(), 1, 1, 1, 1, %s%s)
            """.formatted(
                    clientTiersPayantId == null ? "" : ", has_price_option",
                    type,
                    id,
                    id,
                    clientTiersPayantId == null ? "null" : "960001",
                    clientTiersPayantId == null ? "" : ", false"
                )
        );
        if (clientTiersPayantId != null) {
            st.executeUpdate(
                """
                INSERT INTO third_party_sale_line (id, sale_date, created_at, effective_update_date,
                                                   montant, statut, taux, updated_at,
                                                   client_tiers_payant_id, sale_id, sale_sale_date)
                VALUES (%d, DATE '2026-05-12', now(), now(), 320, 'ACTIF', 80, now(), %d, %d, DATE '2026-05-12')
                """.formatted(id, clientTiersPayantId, id)
            );
        }
    }

    private static void ligne(
        Statement st,
        long ligneId,
        long venteId,
        int produitId,
        int quantite,
        int prixUnitaire,
        int quantiteUg,
        int declarable,
        String motif
    ) throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO sales_line (id, sale_date, sales_id, sales_sale_date, produit_id,
                                    quantity_requested, quantity_sold, quantity_ug, regular_unit_price,
                                    net_unit_price, discount_unit_price, sales_amount, discount_amount,
                                    tax_value, cost_amount, amount_to_be_taken_into_account,
                                    exclusion_motif, to_ignore, created_at, updated_at, effective_update_date)
            SELECT %d, DATE '2026-05-12', %d, DATE '2026-05-12', %d,
                   %d, %d, %d, %d, %d, 0, %d, 0, 0, p.cost_amount, %d,
                   %s, false, now(), now(), now()
              FROM produit p WHERE p.id = %d
            """.formatted(
                    ligneId,
                    venteId,
                    produitId,
                    quantite,
                    quantite,
                    quantiteUg,
                    prixUnitaire,
                    prixUnitaire,
                    quantite * prixUnitaire,
                    declarable,
                    motif == null ? "null" : "'" + motif + "'",
                    produitId
                )
        );
    }

    private static JournalExclusionParamDTO filtres(String recherche, Integer tiersPayantId) {
        return new JournalExclusionParamDTO(DEBUT, FIN, recherche, tiersPayantId);
    }

    // ===== Unités gratuites =====

    @Test
    @DisplayName("Le journal des unités gratuites ne retient que la valeur des unités offertes")
    void journalUgNeRetientQueLesUnitesOffertes() {
        JournalExclusionDTO journal = service.unitesGratuites(filtres(null, null));

        assertEquals(1, journal.lignes().size(), "seule la ligne portant le motif UG doit remonter");
        JournalLigneDTO ligne = journal.lignes().getFirst();
        assertEquals(1_000, ligne.valeurTtc(), "10 × 100");
        assertEquals(200, ligne.montantExclu(), "2 unités gratuites à 100, et rien de plus");
        assertEquals(2, ligne.quantiteUg());
        // 1 000 vendus, 10 unités achetées 60 : la marge porte sur la vente réelle, pas sur le
        // montant déclarable — la marchandise a bien été payée par le client.
        assertEquals(400, ligne.marge());
    }

    @Test
    @DisplayName("Une ligne sans retraitement n'apparaît dans aucun journal")
    void ligneIntacteAbsente() {
        assertTrue(
            service.unitesGratuites(filtres(null, null)).lignes().stream().noneMatch(l -> l.quantite() == 3),
            "la vente 2, jamais retraitée, n'a rien à faire dans un journal d'exclusion"
        );
        assertEquals(0, service.rayonsExclus(filtres("DOLIPRANE", null)).lignes().size());
    }

    // ===== Rayons exclus =====

    @Test
    @DisplayName("Le journal des rayons exclut la ligne entière et nomme le rayon")
    void journalRayonExclutLaLigneEntiere() {
        JournalExclusionDTO journal = service.rayonsExclus(filtres(null, null));

        assertEquals(1, journal.lignes().size());
        JournalLigneDTO ligne = journal.lignes().getFirst();
        assertEquals("PARAPHARMACIE", ligne.rayon());
        assertEquals(1_000, ligne.valeurTtc(), "5 × 200");
        assertEquals(1_000, ligne.montantExclu(), "la ligne entière sort du CA déclaré");
        assertEquals(250, ligne.marge(), "1 000 vendus, 5 unités achetées 150");
    }

    @Test
    @DisplayName("Le journal des unités gratuites ne nomme aucun rayon")
    void journalUgSansRayon() {
        assertNull(service.unitesGratuites(filtres(null, null)).lignes().getFirst().rayon());
    }

    // ===== Recherche produit =====

    @Test
    @DisplayName("La recherche porte sur le libellé comme sur le code, sans ancrage ni casse")
    void rechercheParFragment() {
        assertEquals(1, service.unitesGratuites(filtres("doli", null)).lignes().size(), "fragment de libellé, en minuscules");
        assertEquals(1, service.rayonsExclus(filtres("0000028", null)).lignes().size(), "fragment de code EAN");
        assertEquals(0, service.unitesGratuites(filtres("ASPIRINE", null)).lignes().size());
    }

    // ===== Ventes tiers-payant =====

    @Test
    @DisplayName("Le journal tiers-payant se lit par vente et nomme l'organisme")
    void journalTiersPayantParVente() {
        JournalExclusionDTO journal = service.ventesTiersPayant(filtres(null, null));

        assertEquals(1, journal.ventes().size());
        assertTrue(journal.lignes().isEmpty(), "les lignes se chargent à la sélection, pas d'emblée");
        JournalVenteDTO vente = journal.ventes().getFirst();
        assertEquals("MUTUELLE GENERALE", vente.tiersPayants());
        assertEquals("AWA KONE", vente.client());
        assertEquals(400L, vente.valeurTtc(), "4 × 100");
        assertEquals(400L, vente.montantExclu(), "la vente entière sort du CA déclaré");
        assertEquals(1L, vente.nombreLignes());
    }

    @Test
    @DisplayName("Le détail d'une vente tiers-payant liste ses produits")
    void detailDuneVenteTiersPayant() {
        List<JournalLigneDTO> lignes = service.lignesDeLaVente(960_003L, JOUR);

        assertEquals(1, lignes.size());
        assertEquals("DOLIPRANE 1000", lignes.getFirst().libelleProduit());
        assertEquals(160, lignes.getFirst().marge(), "400 vendus, 4 unités achetées 60");
    }

    @Test
    @DisplayName("Le filtre tiers-payant retient l'organisme demandé et lui seul")
    void filtreParTiersPayant() {
        assertEquals(1, service.ventesTiersPayant(filtres(null, 960_001)).ventes().size());
        assertEquals(0, service.ventesTiersPayant(filtres(null, 999_999)).ventes().size());
    }

    // ===== Indicateurs =====

    @Test
    @DisplayName("Les indicateurs comptent les ventes distinctes et calculent le taux de marge")
    void indicateurs() {
        var kpi = service.unitesGratuites(filtres(null, null)).kpi();

        assertEquals(1L, kpi.nombreVentes());
        assertEquals(1L, kpi.nombreLignes());
        assertEquals(10L, kpi.quantite());
        assertEquals(2L, kpi.quantiteUg());
        assertEquals(1_000L, kpi.valeurTtc());
        assertEquals(200L, kpi.montantExclu());
        assertEquals(400L, kpi.marge());
        assertEquals(0, kpi.tauxMarge().compareTo(new java.math.BigDecimal("40.00")), "400 / 1 000");
    }

    @Test
    @DisplayName("Hors période, les indicateurs sont à zéro plutôt qu'absents")
    void indicateursHorsPeriode() {
        var kpi = service
            .unitesGratuites(new JournalExclusionParamDTO(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null, null))
            .kpi();

        // Un agrégat sans ligne renvoie null en SQL : sans coalesce, l'écran afficherait des trous
        // là où la réponse est « rien n'a été exclu ».
        assertEquals(0L, kpi.valeurTtc());
        assertEquals(0L, kpi.nombreLignes());
        assertEquals(0, kpi.tauxMarge().compareTo(java.math.BigDecimal.ZERO));
    }

    // ===== Chiffre d'affaires de la période =====

    @Test
    @DisplayName("Le CA réel de la période ne compte que les ventes comptant, avant retraitement")
    void caReelDeLaPeriode() {
        // 2 000 sur la vente 1 (1 000 + 1 000) et 300 sur la vente 2. La vente assurance est hors
        // périmètre : la ponction ne porte que sur le comptant (D8).
        assertEquals(2_300L, caPeriodeRepository.caReel(DEBUT, FIN, 1));
    }

    @Test
    @DisplayName("Le CA après exclusions retient ce que les lignes déclarent encore")
    void caApresExclusionsDeLaPeriode() {
        // 800 (unités gratuites retirées) + 0 (rayon exclu) + 300 (ligne intacte).
        assertEquals(1_100L, caPeriodeRepository.caApresExclusions(DEBUT, FIN, 1));
    }
}
