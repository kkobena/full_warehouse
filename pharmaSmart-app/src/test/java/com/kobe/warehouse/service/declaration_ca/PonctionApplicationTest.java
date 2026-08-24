package com.kobe.warehouse.service.declaration_ca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * L'écriture d'une ponction : application, annulation, et ce qu'elles ne doivent pas toucher.
 *
 * <p>Distinct de {@link PonctionCalculatorTest}, qui n'éprouve que le calcul. Ici chaque test
 * modifie les ventes puis les rétablit : le jeu d'essai est reconstruit avant chacun d'eux, sans
 * quoi l'ordre d'exécution deviendrait un paramètre du résultat.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Ponction — application, annulation et périmètre")
class PonctionApplicationTest {

    private static final String SCHEMA = "pharma_smart";
    private static final LocalDate DEBUT = LocalDate.of(2026, 7, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 7, 31);
    private static final LocalDate JOUR = LocalDate.of(2026, 7, 15);
    private static final int MAGASIN = 1;
    private static final BigDecimal PLAFOND = new BigDecimal("35.00");
    private static final String[] CASH = { "CASH" };

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("ponction_application_test");

    private static Connection connection;
    private static PonctionCalculator calculator;

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
            socle(st);
        }

        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        String url = POSTGRES.getJdbcUrl();
        dataSource.setUrl(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        calculator = new PonctionCalculator(JdbcClient.create(dataSource));
    }

    @AfterAll
    static void fermer() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @BeforeEach
    void reconstruireLeJeuDEssai() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM ca_ponction WHERE id >= 970000");
            st.executeUpdate("DELETE FROM payment_transaction WHERE id >= 970000");
            st.executeUpdate("DELETE FROM sales_line WHERE id >= 970000");
            st.executeUpdate("DELETE FROM sales WHERE id >= 970000");
            ventes(st);
        }
    }

    /** Partitions, produits et caisse : ce qui ne change pas d'un test à l'autre. */
    private static void socle(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS sales_2026 PARTITION OF sales FOR VALUES FROM ('2026-01-01') TO ('2027-01-01')");
        st.execute(
            "CREATE TABLE IF NOT EXISTS sales_line_2026 PARTITION OF sales_line FOR VALUES FROM ('2026-01-01') TO ('2027-01-01')"
        );
        st.execute(
            "CREATE TABLE IF NOT EXISTS payment_transaction_2026 PARTITION OF payment_transaction " +
            "FOR VALUES FROM ('2026-01-01') TO ('2027-01-01')"
        );
        st.executeUpdate(
            """
            INSERT INTO produit (id, libelle, status, type_produit, deconditionnable, cost_amount,
                                 regular_unit_price, net_unit_price, item_cost_amount, item_qty,
                                 item_regular_unit_price, created_at, updated_at, famille_id, tva_id)
            SELECT 970001, 'EXONERE', 'ENABLE', 'DETAIL', false, 0, 1, 1, 0, 1, 1, now(), now(),
                   (SELECT min(id) FROM famille_produit), (SELECT id FROM tva WHERE taux = 0)
            ON CONFLICT (id) DO NOTHING
            """
        );
        // sales_line est unique sur (produit_id, sales_id, sale_date) : la vente à 25 lignes
        // exige autant de produits distincts.
        for (int i = 0; i < 25; i++) {
            st.executeUpdate(
                """
                INSERT INTO produit (id, libelle, status, type_produit, deconditionnable, cost_amount,
                                     regular_unit_price, net_unit_price, item_cost_amount, item_qty,
                                     item_regular_unit_price, created_at, updated_at, famille_id, tva_id)
                SELECT %d, 'EXONERE %d', 'ENABLE', 'DETAIL', false, 0, 1, 1, 0, 1, 1, now(), now(),
                       (SELECT min(id) FROM famille_produit), (SELECT id FROM tva WHERE taux = 0)
                ON CONFLICT (id) DO NOTHING
                """.formatted(970_100 + i, i)
            );
        }
        st.executeUpdate(
            """
            INSERT INTO cash_register (id, begin_time, created, updated, init_amount, statut, user_id)
            VALUES (970001, now(), now(), now(), 0, 'OPEN', 1)
            ON CONFLICT (id) DO NOTHING
            """
        );
    }

    /**
     * Trois ventes, chacune pour éprouver un point précis :
     *
     * <pre>
     *   970001  10 000 exonérés sur UNE ligne, réglés 6 000 espèces + 4 000 Orange Money
     *   970002  10 000 exonérés répartis sur 25 lignes de 400 : le cas des arrondis
     *   970003  10 000 exonérés, espèces, annulée en cours de route par un test
     * </pre>
     */
    private static void ventes(Statement st) throws SQLException {
        vente(st, 970_001, 10_000);
        ligne(st, 970_011, 970_001, 10_000);
        reglement(st, 970_001, 970_001, 6_000, "CASH");
        reglement(st, 970_101, 970_001, 4_000, "OM");

        vente(st, 970_002, 10_000);
        for (int i = 0; i < 25; i++) {
            ligne(st, 970_200 + i, 970_002, 400, 970_100 + i);
        }
        reglement(st, 970_002, 970_002, 10_000, "CASH");

        vente(st, 970_003, 10_000);
        ligne(st, 970_031, 970_003, 10_000);
        reglement(st, 970_003, 970_003, 10_000, "CASH");
    }

    private static void vente(Statement st, long id, int montant) throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO sales (dtype, id, sale_date, number_transaction, statut, ca, sales_amount,
                               amount_to_be_paid, amount_to_be_taken_into_account, discount_amount,
                               payroll_amount, rest_to_pay, monnaie, canceled, copy, imported, differe,
                               to_ignore, nature_vente, origine_vente, payment_status, type_prescription,
                               created_at, updated_at, effective_update_date, user_id, seller_id,
                               caissier_id, magasin_id)
            VALUES ('CashSale', %d, DATE '2026-07-15', 'APP-%d', 'CLOSED', 'CA', %d,
                    %d, %d, 0, %d, 0, 0, false, false, false, false,
                    false, 'COMPTANT', 'DIRECT', 'PAYE', 'PRESCRIPTION',
                    now(), now(), now(), 1, 1, 1, %d)
            """.formatted(id, id, montant, montant, montant, montant, MAGASIN)
        );
    }

    private static void ligne(Statement st, long ligneId, long venteId, int montant) throws SQLException {
        ligne(st, ligneId, venteId, montant, 970_001);
    }

    private static void ligne(Statement st, long ligneId, long venteId, int montant, int produitId) throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO sales_line (id, sale_date, sales_id, sales_sale_date, produit_id,
                                    quantity_requested, quantity_sold, quantity_ug, regular_unit_price,
                                    net_unit_price, discount_unit_price, sales_amount, discount_amount,
                                    tax_value, cost_amount, amount_to_be_taken_into_account, to_ignore,
                                    created_at, updated_at, effective_update_date)
            VALUES (%d, DATE '2026-07-15', %d, DATE '2026-07-15', %d,
                    %d, %d, 0, 1, 1, 0, %d, 0, 0, 0, %d, false, now(), now(), now())
            """.formatted(ligneId, venteId, produitId, montant, montant, montant, montant)
        );
    }

    private static void reglement(Statement st, long id, long venteId, int montant, String mode) throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO payment_transaction (dtype, id, transaction_date, sale_id, sale_date,
                                             categorie_ca, created_at, credit, expected_amount,
                                             montant_verse, paid_amount, reel_amount, type_transaction,
                                             payment_mode_code, cash_register_id, part_assure, part_tiers_payant)
            VALUES ('SalePayment', %d, DATE '2026-07-15', %d, DATE '2026-07-15',
                    'CA', now(), false, %d, %d, %d, %d, 'CASH_SALE', '%s', 970001, 0, 0)
            """.formatted(id, venteId, montant, montant, montant, montant, mode)
        );
    }

    /** Crée l'en-tête, écrit le détail et applique. Retourne le nombre de ventes retenues. */
    private int ponctionner(int ponctionId, long objectif) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                """
                INSERT INTO ca_ponction (id, magasin_id, date_debut, date_fin, mode_calcul, valeur_saisie,
                                         ca_apres_exclusions, statut, created_by, created_at, validated_at)
                VALUES (%d, %d, DATE '2026-07-01', DATE '2026-07-31', 'MONTANT_FIXE', %d,
                        30000, 'VALIDEE', 1, now(), now())
                """.formatted(ponctionId, MAGASIN, objectif)
            );
        }
        int ventes = calculator.enregistrerDetail(ponctionId, DEBUT, FIN, MAGASIN, PLAFOND, CASH, objectif);
        calculator.appliquer(ponctionId);
        return ventes;
    }

    private long valeur(String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getLong(1);
        }
    }

    // ===== Règlements =====

    @Test
    @DisplayName("Vente réglée en espèces et en mobile : seule la part espèces est réduite")
    void seuleLaPartEspecesEstReduite() throws SQLException {
        // 3 500 pris sur la vente 970001, dont les 6 000 F d'espèces suffisent à absorber la prise.
        ponctionner(970_001, 3_500);

        assertEquals(
            2_500,
            valeur("SELECT amount_to_be_taken_into_account FROM payment_transaction WHERE id = 970001"),
            "les espèces portent la totalité de la prise"
        );
        assertEquals(
            4_000,
            valeur("SELECT amount_to_be_taken_into_account FROM payment_transaction WHERE id = 970101"),
            "le règlement Orange Money n'est pas touché : il est tracé chez l'opérateur"
        );
    }

    // ===== Arrondis =====

    @Test
    @DisplayName("Le reliquat d'arrondi se répartit à un franc par ligne, sans faire passer aucune sous zéro")
    void reliquatRepartiLigneParLigne() throws SQLException {
        // Les autres ventes sont écartées : à assiette égale elles passeraient devant, et la prise
        // ne toucherait celle-ci que pour son reliquat.
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM payment_transaction WHERE sale_id IN (970001, 970003)");
            st.executeUpdate("DELETE FROM sales_line WHERE sales_id IN (970001, 970003)");
            st.executeUpdate("DELETE FROM sales WHERE id IN (970001, 970003)");
        }

        // 25 lignes à 400 pour 10 000. Le plafond borne la prise à 3 500 ; chaque prise étant ramenée
        // au multiple de 5 inférieur, un objectif de 3 487 donne une prise de 3 485, qui se divise
        // en 139 par ligne au prorata, soit 3 475, et laisse 10 francs à placer — un par ligne, aux
        // plus fortes décimales.
        ponctionner(970_002, 3_487);

        assertEquals(
            10_000 - 3_485,
            valeur("SELECT sum(amount_to_be_taken_into_account) FROM sales_line WHERE sales_id = 970002"),
            "la somme des lignes égale exactement la prise"
        );
        assertEquals(
            0,
            valeur("SELECT count(*) FROM sales_line WHERE sales_id = 970002 AND amount_to_be_taken_into_account < 0"),
            "aucune ligne ne passe sous zéro"
        );
        assertEquals(
            10,
            valeur("SELECT count(*) FROM sales_line WHERE sales_id = 970002 AND amount_to_be_taken_into_account = 400 - 140"),
            "dix lignes portent le franc supplémentaire, pas une seule les dix"
        );
        assertEquals(
            15,
            valeur("SELECT count(*) FROM sales_line WHERE sales_id = 970002 AND amount_to_be_taken_into_account = 400 - 139"),
            "les quinze autres s'en tiennent à leur part entière"
        );
    }

    // ===== Aller-retour =====

    @Test
    @DisplayName("Appliquer puis annuler rétablit exactement les montants d'origine")
    void applicationPuisAnnulationExacte() throws SQLException {
        long lignesAvant = valeur("SELECT sum(amount_to_be_taken_into_account) FROM sales_line WHERE sales_id >= 970000");
        long ventesAvant = valeur("SELECT sum(amount_to_be_taken_into_account) FROM sales WHERE id >= 970000");

        ponctionner(970_003, 5_000);
        assertTrue(
            valeur("SELECT sum(amount_to_be_taken_into_account) FROM sales_line WHERE sales_id >= 970000") < lignesAvant,
            "l'application doit avoir réduit quelque chose"
        );

        calculator.annuler(970_003);

        assertEquals(
            lignesAvant,
            valeur("SELECT sum(amount_to_be_taken_into_account) FROM sales_line WHERE sales_id >= 970000"),
            "les lignes retrouvent leur montant réel"
        );
        assertEquals(
            ventesAvant,
            valeur("SELECT sum(amount_to_be_taken_into_account) FROM sales WHERE id >= 970000"),
            "les ventes aussi"
        );
        assertEquals(
            0,
            valeur("SELECT count(*) FROM sales_line WHERE exclusion_motif = 'PONCTION'"),
            "plus aucune ligne ne porte le motif"
        );
        assertEquals(0, valeur("SELECT count(*) FROM sales WHERE ponction_id IS NOT NULL"), "ni aucune vente le rattachement");
        assertEquals(
            0,
            valeur("SELECT count(*) FROM payment_transaction WHERE amount_to_be_taken_into_account IS NOT NULL"),
            "les règlements reviennent à leur montant encaissé"
        );
    }

    // ===== Périmètre =====

    @Test
    @DisplayName("Les périodes validées d'un même magasin ne peuvent pas se chevaucher")
    void chevauchementRefuseParLaBase() throws SQLException {
        ponctionner(970_004, 1_000);

        // Deux appels concurrents passent le contrôle applicatif en même temps ; c'est la contrainte
        // d'exclusion qui tranche, et elle ne peut pas être contournée par un enchaînement malheureux.
        assertThrows(
            SQLException.class,
            () -> {
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate(
                        """
                        INSERT INTO ca_ponction (id, magasin_id, date_debut, date_fin, mode_calcul, valeur_saisie,
                                                 ca_apres_exclusions, statut, created_by, created_at)
                        VALUES (970005, 1, DATE '2026-07-15', DATE '2026-08-15', 'MONTANT_FIXE', 1000,
                                30000, 'VALIDEE', 1, now())
                        """
                    );
                }
            },
            "la période 15/07 → 15/08 recouvre celle déjà validée"
        );
    }

    @Test
    @DisplayName("Une même période reste possible dans un autre magasin")
    void memePeriodeAutreMagasinAcceptee() throws SQLException {
        ponctionner(970_006, 1_000);

        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                """
                INSERT INTO magasin (id, address, full_name, name, phone, registre, type_magasin)
                SELECT 970001, 'ailleurs', 'OFFICINE B', 'B', '0000', 'RC-B', m.type_magasin
                  FROM magasin m WHERE m.id = 1
                ON CONFLICT (id) DO NOTHING
                """
            );
            st.executeUpdate(
                """
                INSERT INTO ca_ponction (id, magasin_id, date_debut, date_fin, mode_calcul, valeur_saisie,
                                         ca_apres_exclusions, statut, created_by, created_at)
                VALUES (970007, 970001, DATE '2026-07-01', DATE '2026-07-31', 'MONTANT_FIXE', 1000,
                        30000, 'VALIDEE', 1, now())
                """
            );
        }

        assertEquals(1, valeur("SELECT count(*) FROM ca_ponction WHERE id = 970007"), "la contrainte porte sur le magasin");
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM ca_ponction WHERE id = 970007");
            st.executeUpdate("DELETE FROM magasin WHERE id = 970001");
        }
    }

    @Test
    @DisplayName("Une vente annulée après coup se repère dans le détail de la ponction")
    void venteAnnuleeApresPonction() throws SQLException {
        ponctionner(970_008, 3_500);

        assertEquals(
            0,
            valeur(
                """
                SELECT count(*) FROM ca_ponction_detail d
                  JOIN sales s ON s.id = d.sale_id AND s.sale_date = d.sale_date
                 WHERE d.ponction_id = 970008 AND s.canceled = true
                """
            ),
            "aucune vente annulée au départ"
        );

        try (Statement st = connection.createStatement()) {
            st.executeUpdate("UPDATE sales SET canceled = true WHERE id = 970001");
        }

        // C'est ce décompte que le service interroge pour lever le délai d'annulation : une vente
        // contrepassée a déjà défait son montant déclarable, laisser la ponction en place figerait
        // un écart que plus rien n'explique.
        assertEquals(
            1,
            valeur(
                """
                SELECT count(*) FROM ca_ponction_detail d
                  JOIN sales s ON s.id = d.sale_id AND s.sale_date = d.sale_date
                 WHERE d.ponction_id = 970008 AND s.canceled = true
                """
            ),
            "la vente annulée est retrouvée par son détail"
        );
    }
}
