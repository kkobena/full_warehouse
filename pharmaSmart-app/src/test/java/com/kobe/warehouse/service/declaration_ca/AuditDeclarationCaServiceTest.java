package com.kobe.warehouse.service.declaration_ca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.declaration_ca.dto.AnomalieDTO;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Les contrôles de cohérence, éprouvés sur des données volontairement fausses.
 *
 * <p>Un contrôle qui ne détecte rien peut vouloir dire deux choses : que tout va bien, ou qu'il ne
 * regarde pas au bon endroit. Ce test tranche en <strong>fabriquant</strong> chaque incohérence, puis
 * en vérifiant qu'elle est vue — et qu'elle ne l'est plus une fois corrigée.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Audit — détection des incohérences du CA déclaré")
class AuditDeclarationCaServiceTest {

    private static final String SCHEMA = "pharma_smart";
    private static final LocalDate DEBUT = LocalDate.of(2026, 5, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 5, 31);
    private static final long VENTE = 960_001L;

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("audit_test");

    private static Connection connection;
    private static AuditDeclarationCaService service;

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

        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        String url = POSTGRES.getJdbcUrl();
        dataSource.setUrl(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        service = new AuditDeclarationCaService(JdbcClient.create(dataSource), storageService());
    }

    @AfterAll
    static void fermer() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /** Une vente comptant saine : 10 000 F exonérés, réglés en espèces, sans retraitement. */
    private static void jeuDEssai(Statement st) throws SQLException {
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
            INSERT INTO cash_register (id, begin_time, created, updated, init_amount, statut, user_id)
            VALUES (960001, now(), now(), now(), 0, 'OPEN', 1)
            """
        );
        st.executeUpdate(
            """
            INSERT INTO produit (id, libelle, status, type_produit, deconditionnable, cost_amount,
                                 regular_unit_price, net_unit_price, item_cost_amount, item_qty,
                                 item_regular_unit_price, created_at, updated_at, famille_id, tva_id)
            SELECT 960001, 'EXONERE', 'ENABLE', 'DETAIL', false, 0, 1, 1, 0, 1, 1, now(), now(),
                   (SELECT min(id) FROM famille_produit), (SELECT id FROM tva WHERE taux = 0)
            """
        );
        st.executeUpdate(
            """
            INSERT INTO sales (dtype, id, sale_date, number_transaction, statut, ca, sales_amount,
                               amount_to_be_paid, amount_to_be_taken_into_account, discount_amount,
                               payroll_amount, rest_to_pay, monnaie, canceled, copy, imported, differe,
                               to_ignore, nature_vente, origine_vente, payment_status, type_prescription,
                               created_at, updated_at, effective_update_date, user_id, seller_id,
                               caissier_id, magasin_id)
            VALUES ('CashSale', 960001, DATE '2026-05-12', 'AUD-1', 'CLOSED', 'CA', 10000,
                    10000, 10000, 0, 10000, 0, 0, false, false, false, false,
                    false, 'COMPTANT', 'DIRECT', 'PAYE', 'PRESCRIPTION',
                    now(), now(), now(), 1, 1, 1, 1)
            """
        );
        st.executeUpdate(
            """
            INSERT INTO sales_line (id, sale_date, sales_id, sales_sale_date, produit_id,
                                    quantity_requested, quantity_sold, quantity_ug, regular_unit_price,
                                    net_unit_price, discount_unit_price, sales_amount, discount_amount,
                                    tax_value, cost_amount, amount_to_be_taken_into_account, to_ignore,
                                    created_at, updated_at, effective_update_date)
            VALUES (9600011, DATE '2026-05-12', 960001, DATE '2026-05-12', 960001,
                    10000, 10000, 0, 1, 1, 0, 10000, 0, 0, 0, 10000, false, now(), now(), now())
            """
        );
        st.executeUpdate(
            """
            INSERT INTO payment_transaction (dtype, id, transaction_date, sale_id, sale_date,
                                             categorie_ca, created_at, credit, expected_amount,
                                             montant_verse, paid_amount, reel_amount, type_transaction,
                                             payment_mode_code, cash_register_id, part_assure, part_tiers_payant)
            VALUES ('SalePayment', 960001, DATE '2026-05-12', 960001, DATE '2026-05-12',
                    'CA', now(), false, 10000, 10000, 10000, 10000, 'CASH_SALE', 'CASH', 960001, 0, 0)
            """
        );
    }

    /** Les contrôles ne portent que sur le magasin de l'utilisateur connecté. */
    private static StorageService storageService() {
        Magasin magasin = new Magasin();
        magasin.setId(1);
        Storage principal = new Storage();
        principal.setId(1);
        principal.setMagasin(magasin);
        StorageService storageService = org.mockito.Mockito.mock(StorageService.class);
        org.mockito.Mockito.when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(principal);
        return storageService;
    }

    private AnomalieDTO controle(String code) {
        List<AnomalieDTO> resultats = service.controler(DEBUT, FIN);
        return resultats.stream().filter(a -> a.code().equals(code)).findFirst().orElseThrow();
    }

    private void executer(String sql) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    @Test
    @DisplayName("Sur un jeu sain, aucun invariant n'est rompu")
    void jeuSain() {
        List<AnomalieDTO> resultats = service.controler(DEBUT, FIN);

        assertFalse(resultats.isEmpty(), "les contrôles doivent tous s'exécuter");
        for (AnomalieDTO anomalie : resultats) {
            assertTrue(anomalie.estSain(), anomalie.code() + " : " + anomalie.exemples());
        }
    }

    @Test
    @DisplayName("V1 — la base refuse un montant déclarable supérieur au réel")
    void contrainteV1RefuseLeMontantHorsBornes() {
        assertThrows(
            SQLException.class,
            () -> executer("UPDATE sales_line SET amount_to_be_taken_into_account = 99999 WHERE id = 9600011"),
            "sales_line_declarable_ck doit rejeter l'écriture"
        );
    }

    @Test
    @DisplayName("V1 — le contrôle détecte une donnée écrite avant la contrainte")
    void montantHorsBornes() throws SQLException {
        // La contrainte est posée NOT VALID : elle protège les écritures à venir, pas les lignes
        // déjà en base. C'est ce cas que le contrôle couvre, et pour le reproduire il faut écrire
        // comme on écrivait avant elle.
        executer("ALTER TABLE sales_line DROP CONSTRAINT sales_line_declarable_ck");
        try {
            executer("UPDATE sales_line SET amount_to_be_taken_into_account = 99999 WHERE id = 9600011");
            assertEquals(1, controle("V1").nombreAnomalies());
        } finally {
            executer("UPDATE sales_line SET amount_to_be_taken_into_account = 10000 WHERE id = 9600011");
            executer(
                """
                ALTER TABLE sales_line ADD CONSTRAINT sales_line_declarable_ck
                CHECK (amount_to_be_taken_into_account IS NULL
                       OR (amount_to_be_taken_into_account >= 0
                           AND amount_to_be_taken_into_account <= quantity_requested * regular_unit_price))
                NOT VALID
                """
            );
        }
        assertTrue(controle("V1").estSain(), "l'anomalie doit disparaître une fois la donnée réparée");
    }

    @Test
    @DisplayName("V9 — un écart de TVA sans motif est détecté, et disparaît dès qu'il est motivé")
    void tvaNonRapprochee() throws SQLException {
        // Un montant déclarable réduit sans motif : l'écart existe, rien ne l'explique.
        executer("UPDATE sales_line SET amount_to_be_taken_into_account = 8000 WHERE id = 9600011");
        AnomalieDTO anomalie = controle("V9");

        assertEquals(1, anomalie.nombreAnomalies());
        assertTrue(anomalie.exemples().getFirst().contains("2000 inexpliqué"), anomalie.exemples().getFirst());

        // Le même écart, cette fois porté par une ligne exclue : il se rapproche exactement.
        executer("UPDATE sales_line SET exclusion_motif = 'UG' WHERE id = 9600011");
        assertTrue(controle("V9").estSain(), "un écart motivé se rattache à sa ligne");

        executer("UPDATE sales_line SET amount_to_be_taken_into_account = 10000, exclusion_motif = NULL WHERE id = 9600011");
    }

    @Test
    @DisplayName("V2 — la divergence entre la vente et ses lignes est détectée")
    void divergenceVenteLignes() throws SQLException {
        executer("UPDATE sales SET amount_to_be_taken_into_account = 7000 WHERE id = 960001");
        AnomalieDTO anomalie = controle("V2");

        assertEquals(1, anomalie.nombreAnomalies());
        assertTrue(anomalie.exemples().getFirst().contains("7000"), "l'exemple doit citer les deux montants");

        executer("UPDATE sales SET amount_to_be_taken_into_account = 10000 WHERE id = 960001");
        assertTrue(controle("V2").estSain());
    }

    @Test
    @DisplayName("V2b — un encaissement déclaré supérieur au CA déclaré est détecté")
    void encaissementSuperieurAuChiffre() throws SQLException {
        executer("UPDATE sales SET amount_to_be_taken_into_account = 8000 WHERE id = 960001");
        executer("UPDATE sales_line SET amount_to_be_taken_into_account = 8000 WHERE id = 9600011");
        // Le règlement reste à 10 000 : c'est exactement le cas « CA 800, encaissement 1 000 ».
        assertEquals(1, controle("V2b").nombreAnomalies());

        executer("UPDATE payment_transaction SET amount_to_be_taken_into_account = 8000 WHERE id = 960001");
        assertTrue(controle("V2b").estSain(), "le règlement suit le chiffre déclaré");

        executer("UPDATE sales SET amount_to_be_taken_into_account = 10000 WHERE id = 960001");
        executer("UPDATE sales_line SET amount_to_be_taken_into_account = 10000 WHERE id = 9600011");
        executer("UPDATE payment_transaction SET amount_to_be_taken_into_account = NULL WHERE id = 960001");
    }

    @Test
    @DisplayName("V8 — une ponction posée sur une ligne taxée est détectée")
    void ponctionSurLigneTaxee() throws SQLException {
        executer("UPDATE sales_line SET exclusion_motif = 'PONCTION', tax_value = 18 WHERE id = 9600011");
        assertEquals(1, controle("V8").nombreAnomalies());

        executer("UPDATE sales_line SET exclusion_motif = NULL, tax_value = 0 WHERE id = 9600011");
        assertTrue(controle("V8").estSain());
    }

    @Test
    @DisplayName("Chaque contrôle explique ce qu'il garantit et ce qu'on risque")
    void chaqueControleSExplique() {
        for (AnomalieDTO anomalie : service.controler(DEBUT, FIN)) {
            assertFalse(anomalie.libelle().isBlank(), anomalie.code() + " sans libellé");
            assertFalse(anomalie.consequence().isBlank(), anomalie.code() + " sans conséquence");
        }
    }
}
