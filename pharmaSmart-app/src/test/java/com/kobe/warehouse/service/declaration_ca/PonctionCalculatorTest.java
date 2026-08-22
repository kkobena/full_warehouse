package com.kobe.warehouse.service.declaration_ca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionLigneDTO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * L'algorithme de ponction, exécuté sur un PostgreSQL réel.
 *
 * <p>Tout le calcul vit dans le SQL — plafond par vente, tri, point de coupure — parce qu'une
 * période de deux mois porte couramment 20 000 ventes qu'on ne peut pas trier en mémoire. Le vérifier
 * avec un repository simulé ne prouverait donc rien : c'est la requête qu'il faut exécuter.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Ponction — plafond, ordre et point de coupure")
class PonctionCalculatorTest {

    private static final String SCHEMA = "pharma_smart";
    private static final LocalDate DEBUT = LocalDate.of(2026, 4, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 4, 30);
    private static final int MAGASIN = 1;
    private static final BigDecimal PLAFOND = new BigDecimal("35.00");
    private static final String[] CASH = { "CASH" };

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("ponction_test");

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            jeuDEssai(st);
        }

        // Le calculateur ne fait que du SQL : un JdbcClient suffit, sans bootstrap JPA.
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        String url = POSTGRES.getJdbcUrl();
        // L'URL de Testcontainers porte déjà des paramètres : un second « ? » la rendrait invalide.
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

    /**
     * Six ventes, choisies pour éprouver chaque restriction d'assiette :
     *
     * <pre>
     *   1  10 000 exonérée, espèces          → éligible, plafond 3 500
     *   2   8 000 exonérée, espèces          → éligible, plafond 2 800
     *   3   5 000 exonérée, espèces          → éligible, plafond 1 750
     *   4  10 000 dont 4 000 exonérés        → éligible, plafond min(3 500, 4 000) = 3 500
     *   5  10 000 entièrement taxée          → écartée : aucune part exonérée
     *   6  10 000 exonérée mais différée     → écartée : règlements à venir
     * </pre>
     */
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
            INSERT INTO produit (id, libelle, status, type_produit, deconditionnable, cost_amount,
                                 regular_unit_price, net_unit_price, item_cost_amount, item_qty,
                                 item_regular_unit_price, created_at, updated_at, famille_id, tva_id)
            SELECT 950001, 'EXONERE', 'ENABLE', 'DETAIL', false, 0, 1, 1, 0, 1, 1, now(), now(),
                   (SELECT min(id) FROM famille_produit), (SELECT id FROM tva WHERE taux = 0)
            """
        );
        st.executeUpdate(
            """
            INSERT INTO produit (id, libelle, status, type_produit, deconditionnable, cost_amount,
                                 regular_unit_price, net_unit_price, item_cost_amount, item_qty,
                                 item_regular_unit_price, created_at, updated_at, famille_id, tva_id)
            SELECT 950002, 'TAXE 18', 'ENABLE', 'DETAIL', false, 0, 1, 1, 0, 1, 1, now(), now(),
                   (SELECT min(id) FROM famille_produit), (SELECT id FROM tva WHERE taux = 18)
            """
        );

        // Les règlements exigent une caisse ouverte : la ponction vise la part espèces.
        st.executeUpdate(
            """
            INSERT INTO cash_register (id, begin_time, created, updated, init_amount, statut, user_id)
            VALUES (950001, now(), now(), now(), 0, 'OPEN', 1)
            """
        );

        vente(st, 1, 10_000, 0, false);
        vente(st, 2, 8_000, 0, false);
        vente(st, 3, 5_000, 0, false);
        vente(st, 4, 4_000, 6_000, false);
        vente(st, 5, 0, 10_000, false);
        vente(st, 6, 10_000, 0, true);
    }

    private static void vente(Statement st, int numero, int montantExonere, int montantTaxe, boolean differe)
        throws SQLException {
        long id = 950_000L + numero;
        int total = montantExonere + montantTaxe;
        st.executeUpdate(
            """
            INSERT INTO sales (dtype, id, sale_date, number_transaction, statut, ca, sales_amount,
                               amount_to_be_paid, amount_to_be_taken_into_account, discount_amount,
                               payroll_amount, rest_to_pay, monnaie, canceled, copy, imported, differe,
                               to_ignore, nature_vente, origine_vente, payment_status, type_prescription,
                               created_at, updated_at, effective_update_date, user_id, seller_id,
                               caissier_id, magasin_id)
            VALUES ('CashSale', %d, DATE '2026-04-10', 'PON-%d', 'CLOSED', 'CA', %d,
                    %d, %d, 0, %d, 0, 0, false, false, false, %b,
                    false, 'COMPTANT', 'DIRECT', 'PAYE', 'PRESCRIPTION',
                    now(), now(), now(), 1, 1, 1, 1)
            """.formatted(id, numero, total, total, total, total, differe)
        );
        if (montantExonere > 0) {
            ligne(st, id * 10, id, 950_001, montantExonere, 0);
        }
        if (montantTaxe > 0) {
            ligne(st, id * 10 + 1, id, 950_002, montantTaxe, 18);
        }
        st.executeUpdate(
            """
            INSERT INTO payment_transaction (dtype, id, transaction_date, sale_id, sale_date,
                                             categorie_ca, created_at, credit, expected_amount,
                                             montant_verse, paid_amount, reel_amount, type_transaction,
                                             payment_mode_code, cash_register_id, part_assure, part_tiers_payant)
            VALUES ('SalePayment', %d, DATE '2026-04-10', %d, DATE '2026-04-10',
                    'CA', now(), false, %d, %d, %d, %d, 'CASH_SALE', 'CASH',
                    (SELECT id FROM cash_register LIMIT 1), 0, 0)
            """.formatted(id, id, total, total, total, total)
        );
    }

    private static void ligne(Statement st, long ligneId, long venteId, int produitId, int montant, int taux)
        throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO sales_line (id, sale_date, sales_id, sales_sale_date, produit_id,
                                    quantity_requested, quantity_sold, quantity_ug, regular_unit_price,
                                    net_unit_price, discount_unit_price, sales_amount, discount_amount,
                                    tax_value, cost_amount, amount_to_be_taken_into_account, to_ignore,
                                    created_at, updated_at, effective_update_date)
            VALUES (%d, DATE '2026-04-10', %d, DATE '2026-04-10', %d,
                    %d, %d, 0, 1, 1, 0, %d, 0, %d, 0, %d, false, now(), now(), now())
            """.formatted(ligneId, venteId, produitId, montant, montant, montant, taux, montant)
        );
    }

    // ===== Assiette =====

    @Test
    @DisplayName("L'assiette écarte la vente taxée et la vente différée")
    void assietteEcarteCeQuiNestPasEligible() {
        PonctionCalculator.Assiette assiette = calculator.calculerAssiette(DEBUT, FIN, MAGASIN, PLAFOND, CASH);

        assertEquals(4, assiette.nombreVentes(), "seules 4 ventes sur 6 sont éligibles");
        assertEquals(27_000, assiette.assietteTva0(), "10 000 + 8 000 + 5 000 + 4 000 exonérés");
    }

    @Test
    @DisplayName("Le montant ponctionnable est borné par le plafond ET par la part exonérée")
    void montantPonctionnable() {
        PonctionCalculator.Assiette assiette = calculator.calculerAssiette(DEBUT, FIN, MAGASIN, PLAFOND, CASH);

        // 3 500 + 2 800 + 1 750 + min(3 500, 4 000) = 11 550
        assertEquals(11_550, assiette.montantPonctionnable());
        assertTrue(
            assiette.montantPonctionnable() < assiette.assietteTva0() * 35 / 100 + assiette.assietteTva0(),
            "le plafond borne bien en deçà de l'assiette"
        );
    }

    // ===== Répartition =====

    @Test
    @DisplayName("La prise s'arrête exactement à l'objectif, sur les plus grosses ventes d'abord")
    void repartitionSarreteALobjectif() {
        List<PonctionLigneDTO> lignes = calculator.repartir(DEBUT, FIN, MAGASIN, PLAFOND, CASH, 5_000);

        assertEquals(5_000, lignes.stream().mapToLong(PonctionLigneDTO::montantPonctionne).sum());
        assertEquals(2, lignes.size(), "3 500 sur la première, 1 500 sur la deuxième");
        assertEquals(3_500, lignes.getFirst().montantPonctionne());
        assertEquals(1_500, lignes.get(1).montantPonctionne());
    }

    @Test
    @DisplayName("Aucune vente ne cède plus que son plafond")
    void plafondJamaisDepasse() {
        List<PonctionLigneDTO> lignes = calculator.repartir(DEBUT, FIN, MAGASIN, PLAFOND, CASH, 11_550);

        for (PonctionLigneDTO ligne : lignes) {
            long plafondVente = ligne.montantVente() * 35 / 100;
            assertTrue(
                ligne.montantPonctionne() <= plafondVente,
                "vente " + ligne.saleId() + " : " + ligne.montantPonctionne() + " > " + plafondVente
            );
            assertTrue(ligne.montantPonctionne() <= ligne.montantBase(), "jamais plus que la part exonérée");
        }
    }

    @Test
    @DisplayName("Une vente mixte est plafonnée par sa part exonérée quand celle-ci est la plus basse")
    void venteMixtePlafonneeParLaPartExoneree() {
        List<PonctionLigneDTO> lignes = calculator.repartir(DEBUT, FIN, MAGASIN, PLAFOND, CASH, 11_550);

        PonctionLigneDTO mixte = lignes.stream().filter(l -> l.saleId() == 950_004L).findFirst().orElseThrow();
        assertEquals(10_000, mixte.montantVente());
        assertEquals(4_000, mixte.montantBase(), "seuls 4 000 sont exonérés");
        assertEquals(3_500, mixte.montantPonctionne(), "ici c'est le plafond de 35 % qui borne");
    }

    @Test
    @DisplayName("L'ordre suit l'assiette exonérée décroissante, pas le montant total")
    void ordreSurLassietteExoneree() {
        List<PonctionLigneDTO> lignes = calculator.repartir(DEBUT, FIN, MAGASIN, PLAFOND, CASH, 11_550);

        // La vente mixte totalise 10 000 comme la première, mais n'a que 4 000 exonérés : elle passe
        // après la vente à 5 000 entièrement exonérée.
        List<Long> ordre = lignes.stream().map(PonctionLigneDTO::saleId).toList();
        assertEquals(List.of(950_001L, 950_002L, 950_003L, 950_004L), ordre);
    }

    @Test
    @DisplayName("Le détail écrit en base totalise exactement l'objectif")
    void detailEcritTotaliseLobjectif() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                """
                INSERT INTO ca_ponction (id, magasin_id, date_debut, date_fin, mode_calcul, valeur_saisie,
                                         ca_apres_exclusions, statut, created_by, created_at)
                VALUES (960101, 1, DATE '2026-04-01', DATE '2026-04-30', 'MONTANT_FIXE', 5000,
                        27000, 'SIMULATION', 1, now())
                """
            );
        }

        int ventes = calculator.enregistrerDetail(960101, DEBUT, FIN, MAGASIN, PLAFOND, CASH, 5_000);

        assertEquals(2, ventes, "3 500 sur la première, 1 500 sur la deuxième");
        try (Statement st = connection.createStatement()) {
            var rs = st.executeQuery(
                "SELECT sum(montant_ponctionne), count(*) FILTER (WHERE numero_transaction IS NULL) " +
                "FROM ca_ponction_detail WHERE ponction_id = 960101"
            );
            assertTrue(rs.next());
            assertEquals(5_000, rs.getLong(1), "le détail en base totalise l'objectif, pas seulement le calcul en mémoire");
            assertEquals(0, rs.getLong(2), "chaque ligne porte sa référence de vente : le justificatif est autonome");
        }
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM ca_ponction WHERE id = 960101");
        }
    }

    @Test
    @DisplayName("Un objectif nul ne retient aucune vente")
    void objectifNul() {
        assertTrue(calculator.repartir(DEBUT, FIN, MAGASIN, PLAFOND, CASH, 0).isEmpty());
    }

    @Test
    @DisplayName("Deux exécutions identiques donnent le même résultat : la simulation vaut engagement")
    void calculDeterministe() {
        List<PonctionLigneDTO> premiere = calculator.repartir(DEBUT, FIN, MAGASIN, PLAFOND, CASH, 7_000);
        List<PonctionLigneDTO> seconde = calculator.repartir(DEBUT, FIN, MAGASIN, PLAFOND, CASH, 7_000);

        assertEquals(premiere, seconde);
    }

    // ===== V7 : la ponction est neutre en TVA =====

    /**
     * V7 — la ponction ne déplace pas un franc de TVA.
     *
     * <p>C'est la contrepartie de D7 : la ponction ne mord que sur des lignes à TVA 0, donc le
     * rapport TVA doit sortir identique taux par taux, avant et après. Seul le TTC du taux exonéré
     * baisse, et exactement du montant ponctionné.
     *
     * <p>La comparaison se fait à exclusions constantes, et non entre {@code REEL} et
     * {@code DECLARE} : entre ces deux modes la TVA baisse légitimement, du fait des exclusions
     * rayon, tiers-payant et unités gratuites.
     */
    @Test
    @DisplayName("V7 — après ponction, la TVA est inchangée taux par taux ; seul le TTC exonéré baisse")
    void ponctionNeutreEnTva() throws SQLException {
        Map<Integer, long[]> avant = rapportTva();

        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                """
                INSERT INTO ca_ponction (id, magasin_id, date_debut, date_fin, mode_calcul, valeur_saisie,
                                         ca_apres_exclusions, statut, created_by, created_at)
                VALUES (960201, 1, DATE '2026-04-01', DATE '2026-04-30', 'MONTANT_FIXE', 5000,
                        27000, 'VALIDEE', 1, now())
                """
            );
        }
        calculator.enregistrerDetail(960201, DEBUT, FIN, MAGASIN, PLAFOND, CASH, 5_000);
        calculator.appliquer(960201);

        try {
            Map<Integer, long[]> apres = rapportTva();

            assertEquals(avant.keySet(), apres.keySet(), "aucun taux n'apparaît ni ne disparaît");
            for (Integer taux : avant.keySet()) {
                assertEquals(avant.get(taux)[1], apres.get(taux)[1], "TVA au taux " + taux);
            }
            assertEquals(5_000, avant.get(0)[0] - apres.get(0)[0], "le TTC exonéré baisse du montant ponctionné");
            assertEquals(avant.get(18)[0], apres.get(18)[0], "le TTC taxé ne bouge pas");
        } finally {
            calculator.annuler(960201);
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("DELETE FROM ca_ponction WHERE id = 960201");
            }
        }

        // L'annulation rend le rapport à son état d'origine : c'est ce qui rend la ponction réversible.
        assertEquals(avant.get(0)[0], rapportTva().get(0)[0], "après annulation, le TTC exonéré est rétabli");
    }

    /** Le rapport TVA déclaré, indexé par taux : {@code [montantTtc, montantTva]}. */
    private Map<Integer, long[]> rapportTva() throws SQLException {
        Map<Integer, long[]> parTaux = new HashMap<>();
        try (Statement st = connection.createStatement()) {
            var rs = st.executeQuery(
                "SELECT sales_tva_report(DATE '2026-04-01', DATE '2026-04-30', ARRAY['CLOSED'], ARRAY['CA'], false, 'DECLARE')"
            );
            assertTrue(rs.next());
            try {
                JsonNode lignes = MAPPER.readTree(rs.getString(1));
                for (JsonNode ligne : lignes) {
                    long ttc = ligne.get("montantTtc").asLong();
                    long ht = ligne.get("montantHt").asLong();
                    parTaux.put(ligne.get("codeTva").asInt(), new long[] { ttc, ttc - ht });
                }
            } catch (Exception e) {
                throw new AssertionError("rapport TVA illisible", e);
            }
        }
        return parTaux;
    }
}
