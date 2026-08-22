package com.kobe.warehouse.service.financiel_transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.service.utils.ServiceUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Vérifie le calcul du montant hors taxe des fonctions de rapport, sur un PostgreSQL réel.
 *
 * <p><strong>Pourquoi une base réelle.</strong> Le défaut corrigé par
 * {@code V1.9.0__fix_tva_division_entiere.sql} était une division entière <em>dans le SQL</em> :
 * {@code sales_line.tax_value} étant de type {@code integer}, {@code 18 / 100} valait 0, le diviseur
 * valait 1 pour tous les taux, et le montant HT était donc égal au montant TTC — la TVA collectée
 * ressortait à zéro dans tous les rapports. Un test unitaire avec un repository simulé n'aurait rien
 * vu : il faut exécuter les fonctions.
 *
 * <p>Le test rejoue <strong>toute</strong> la chaîne Flyway, et pas seulement la migration
 * corrective : c'est la seule façon de vérifier que la redéfinition s'applique bien par-dessus les
 * définitions de {@code V1.0.5__procedure.sql} et qu'aucune surcharge parasite n'est créée.
 *
 * <p>{@code disabledWithoutDocker} ignore le test là où Docker est absent plutôt que de faire
 * échouer la construction : les autres tests du dépôt sont des tests unitaires purs et ne l'exigent
 * pas.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Calcul du HT et de la TVA dans les fonctions de rapport")
class TvaCalculationMigrationTest {

    private static final String SCHEMA = "pharma_smart";

    /** Le jeu d'essai : un TTC par taux, choisi pour que le HT tombe juste sur 10 000. */
    private static final int TTC_TAUX_0 = 10_000;
    private static final int TTC_TAUX_9 = 10_900;
    private static final int TTC_TAUX_18 = 11_800;
    private static final int HT_ATTENDU_PAR_LIGNE = 10_000;

    private static final int PRODUIT_TAUX_0 = 900_001;
    private static final int PRODUIT_TAUX_9 = 900_002;
    private static final int PRODUIT_TAUX_18 = 900_003;
    private static final long VENTE_ID = 900_001L;
    private static final String DATE_VENTE = "2026-03-15";

    /**
     * Démarré et arrêté par l'extension {@code @Testcontainers}.
     *
     * <p>{@code @SuppressWarnings("resource")} est délibéré : {@link PostgreSQLContainer} est
     * {@code AutoCloseable}, mais un {@code try}-with-resources le fermerait à la sortie du bloc,
     * donc avant la première assertion. Le conteneur doit vivre le temps de toute la classe — c'est
     * exactement le contrat de {@link Container}, qui le démarre avant les {@code @BeforeAll} et
     * l'arrête après les {@code @AfterAll}.
     */
    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("pharma_smart_test");

    private static Connection connection;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void preparerBase() throws SQLException {
        Flyway.configure()
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
        }
        insererJeuDEssai();
    }

    @AfterAll
    static void fermerConnexion() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Une vente comptant close, trois lignes — une par taux de TVA.
     *
     * <p>S'appuie sur les référentiels posés par {@code V1.0.2__referentiels.sql} (magasin 1,
     * utilisateur 1, taux 0/9/18) ; seuls les produits et la vente sont créés ici.
     */
    private static void insererJeuDEssai() throws SQLException {
        try (Statement st = connection.createStatement()) {
            creerPartitionsAnnuelles(st);
            int famille = valeurEntiere("SELECT min(id) FROM famille_produit");

            creerProduit(st, PRODUIT_TAUX_0, "PRODUIT EXONERE", TTC_TAUX_0, 0, famille);
            creerProduit(st, PRODUIT_TAUX_9, "PRODUIT TAUX 9", TTC_TAUX_9, 9, famille);
            creerProduit(st, PRODUIT_TAUX_18, "PRODUIT TAUX 18", TTC_TAUX_18, 18, famille);

            int totalTtc = TTC_TAUX_0 + TTC_TAUX_9 + TTC_TAUX_18;
            st.executeUpdate(
                """
                INSERT INTO sales (dtype, id, sale_date, number_transaction, statut, ca,
                                   sales_amount, amount_to_be_paid, amount_to_be_taken_into_account,
                                   discount_amount, payroll_amount, rest_to_pay, monnaie,
                                   canceled, copy, imported, differe, to_ignore,
                                   nature_vente, origine_vente, payment_status, type_prescription,
                                   created_at, updated_at, effective_update_date,
                                   user_id, seller_id, caissier_id, magasin_id)
                VALUES ('CashSale', %d, DATE '%s', 'T-TVA-001', 'CLOSED', 'CA',
                        %d, %d, %d,
                        0, %d, 0, 0,
                        false, false, false, false, false,
                        'COMPTANT', 'DIRECT', 'PAYE', 'PRESCRIPTION',
                        now(), now(), now(),
                        1, 1, 1, 1)
                """.formatted(VENTE_ID, DATE_VENTE, totalTtc, totalTtc, totalTtc, totalTtc)
            );

            creerLigne(st, 900_101L, PRODUIT_TAUX_0, TTC_TAUX_0, 0);
            creerLigne(st, 900_102L, PRODUIT_TAUX_9, TTC_TAUX_9, 9);
            creerLigne(st, 900_103L, PRODUIT_TAUX_18, TTC_TAUX_18, 18);
        }
    }

    /**
     * {@code sales} et {@code sales_line} sont partitionnées par année sur {@code sale_date}, et les
     * partitions ne sont pas créées par les migrations : c'est
     * {@link com.kobe.warehouse.config.DatabasePartitionService} qui s'en charge au démarrage de
     * l'application. Le test le fait donc lui-même, en reprenant la même convention de nommage.
     */
    private static void creerPartitionsAnnuelles(Statement st) throws SQLException {
        int annee = Integer.parseInt(DATE_VENTE.substring(0, 4));
        for (String table : new String[] { "sales", "sales_line" }) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS %s_%d PARTITION OF %s FOR VALUES FROM ('%d-01-01') TO ('%d-01-01')"
                    .formatted(table, annee, table, annee, annee + 1)
            );
        }
    }

    private static void creerProduit(Statement st, int id, String libelle, int prix, int taux, int famille)
        throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO produit (id, libelle, status, type_produit, deconditionnable,
                                 cost_amount, regular_unit_price, net_unit_price,
                                 item_cost_amount, item_qty, item_regular_unit_price,
                                 created_at, updated_at, famille_id, tva_id)
            VALUES (%d, '%s', 'ENABLE', 'DETAIL', false,
                    0, %d, %d,
                    0, 1, %d,
                    now(), now(), %d, (SELECT id FROM tva WHERE taux = %d))
            """.formatted(id, libelle, prix, prix, prix, famille, taux)
        );
    }

    private static void creerLigne(Statement st, long id, int produitId, int prix, int taux) throws SQLException {
        st.executeUpdate(
            """
            INSERT INTO sales_line (id, sale_date, sales_id, sales_sale_date, produit_id,
                                    quantity_requested, quantity_sold, quantity_ug,
                                    regular_unit_price, net_unit_price, discount_unit_price,
                                    sales_amount, discount_amount, tax_value, cost_amount,
                                    amount_to_be_taken_into_account, to_ignore,
                                    created_at, updated_at, effective_update_date)
            VALUES (%d, DATE '%s', %d, DATE '%s', %d,
                    1, 1, 0,
                    %d, %d, 0,
                    %d, 0, %d, 0,
                    %d, false,
                    now(), now(), now())
            """.formatted(id, DATE_VENTE, VENTE_ID, DATE_VENTE, produitId, prix, prix, prix, taux, prix)
        );
    }

    // ===== Le diviseur lui-même =====

    @Test
    @DisplayName("tva_divisor rend 1 + taux/100 et non 1 : c'est la régression à empêcher")
    void tvaDivisorNestPlusToujoursUn() throws SQLException {
        assertEquals(new BigDecimal("1.00"), diviseur(0).setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("1.09"), diviseur(9).setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("1.18"), diviseur(18).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("ht_from_ttc rend la même valeur que son pendant Java ServiceUtil#htFromTtc")
    void htSqlEtHtJavaConcordent() throws SQLException {
        for (int taux : new int[] { 0, 9, 18 }) {
            for (int ttc : new int[] { 1, 999, 10_000, 11_800, 1_234_567 }) {
                BigDecimal sql = valeurDecimale("SELECT round(ht_from_ttc(%d, %d))".formatted(ttc, taux));
                long java = ServiceUtil.htFromTtc(ttc, taux);
                assertEquals(
                    sql.longValueExact(),
                    java,
                    "divergence SQL/Java pour ttc=%d taux=%d — le rapport TVA et la FNE cesseraient de se recouper"
                        .formatted(ttc, taux)
                );
            }
        }
    }

    // ===== Le rapport TVA =====

    @Test
    @DisplayName("sales_tva_report ventile un HT correct par taux, et une TVA non nulle")
    void rapportTvaVentileParTaux() throws SQLException {
        JsonNode lignes = rapportTva();
        assertEquals(3, lignes.size(), "une ligne par taux");

        verifierLigne(lignes, 0, TTC_TAUX_0, HT_ATTENDU_PAR_LIGNE, 0);
        verifierLigne(lignes, 9, TTC_TAUX_9, HT_ATTENDU_PAR_LIGNE, 900);
        verifierLigne(lignes, 18, TTC_TAUX_18, HT_ATTENDU_PAR_LIGNE, 1_800);
    }

    @Test
    @DisplayName("Les lignes exonérées ne sont pas touchées : HT = TTC au taux 0")
    void tauxZeroInchange() throws SQLException {
        JsonNode ligne = ligneDuTaux(rapportTva(), 0);
        assertEquals(ligne.get("montantTtc").asLong(), ligne.get("montantHt").asLong());
    }

    @Test
    @DisplayName("La TVA totale du rapport n'est plus nulle")
    void tvaTotaleNonNulle() throws SQLException {
        JsonNode lignes = rapportTva();
        long tva = 0;
        for (JsonNode l : lignes) {
            tva += l.get("montantTtc").asLong() - l.get("montantHt").asLong();
        }
        assertEquals(2_700, tva, "900 au taux 9 + 1 800 au taux 18");
    }

    // ===== Cohérence entre les trois écrans de comptabilité =====

    @Test
    @DisplayName("Le rapport TVA, la balance de caisse et le tableau pharmacien annoncent le même HT")
    void lesTroisRapportsSeRecoupent() throws SQLException {
        long htTva = 0;
        for (JsonNode l : rapportTva()) {
            htTva += l.get("montantHt").asLong();
        }

        long htBalance = 0;
        for (JsonNode l : appelJson(
            "SELECT sales_balance(DATE '%s', DATE '%s', ARRAY['CLOSED'], ARRAY['CA'], false, 'DECLARE')"
                .formatted(DATE_VENTE, DATE_VENTE)
        )) {
            htBalance += l.get("montantHt").asLong();
        }

        long htTableau = 0;
        for (JsonNode l : appelJson(
            "SELECT tableau_pharmacien_report(DATE '%s', DATE '%s', ARRAY['CLOSED'], ARRAY['CA'], false, 'DECLARE')"
                .formatted(DATE_VENTE, DATE_VENTE)
        )) {
            htTableau += l.get("montantHt").asLong();
        }

        assertEquals(3 * HT_ATTENDU_PAR_LIGNE, htTva, "rapport TVA");
        assertEquals(htTva, htBalance, "balance de caisse");
        assertEquals(htTva, htTableau, "tableau pharmacien");
    }

    // ===== Modes de lecture REEL / DECLARE =====



    @Test
    @DisplayName("Le mode par défaut est REEL : un paramètre oublié ne sous-déclare pas en silence")
    void modeParDefautEstReel() throws SQLException {
        long defaut = valeurEntiere(
            """
            SELECT coalesce(sum((l ->> 'montantTtc')::bigint), 0)
              FROM jsonb_array_elements(
                     sales_tva_report(DATE '%s', DATE '%s', ARRAY['CLOSED'], ARRAY['CA'], false)
                   ) AS l
            """.formatted(DATE_VENTE, DATE_VENTE)
        );
        assertEquals(totalTtc(rapportTva("REEL")), defaut, "l'état neutre est la donnée non retraitée");
    }

    @Test
    @DisplayName("Les 5 fonctions des écrans de comptabilité acceptent le mode")
    void lesCinqFonctionsAcceptentLeMode() throws SQLException {
        String sql =
            """
            SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = '%s'
               AND p.proname IN ('sales_balance', 'sales_tva_report', 'sales_tva_report_journalier',
                                 'tableau_pharmacien_report', 'tableau_pharmacien_month_report')
               AND pg_get_function_identity_arguments(p.oid) LIKE '%%p_mode text%%'
            """.formatted(SCHEMA);
        assertEquals(5, valeurEntiere(sql));
    }

    @Test
    @DisplayName("L'ancienne signature à six arguments a bien été supprimée")
    void ancienneSignatureSupprimee() throws SQLException {
        String sql =
            """
            SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = '%s' AND p.proname = 'sales_balance'
            """.formatted(SCHEMA);
        assertEquals(1, valeurEntiere(sql), "deux surcharges rendraient tout appel à six arguments ambigu");
    }

    // ===== Non-régression structurelle =====

    @Test
    @DisplayName("Aucune fonction de rapport ne calcule encore le diviseur à la main")
    void aucuneDivisionEntiereResiduelle() throws SQLException {
        String sql =
            """
            SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = '%s'
               AND p.prokind = 'f'          -- pg_get_functiondef ne sait pas décrire un agrégat
               AND p.prosrc LIKE '%%tax_value / 100%%'
            """.formatted(SCHEMA);
        assertEquals(
            0,
            valeurEntiere(sql),
            "une fonction réintroduit la division entière : passer par tva_divisor(tax_value)"
        );
    }

    @Test
    @DisplayName("La redéfinition n'a pas créé de surcharge parasite")
    void pasDeSurchargeDupliquee() throws SQLException {
        String sql =
            """
            SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = '%s' AND p.proname = 'sales_tva_report'
            """.formatted(SCHEMA);
        assertEquals(1, valeurEntiere(sql), "sales_tva_report doit exister en un seul exemplaire");
    }

    // ===== Utilitaires =====

    private void verifierLigne(JsonNode lignes, int taux, long ttcAttendu, long htAttendu, long tvaAttendue) {
        JsonNode ligne = ligneDuTaux(lignes, taux);
        assertEquals(ttcAttendu, ligne.get("montantTtc").asLong(), "TTC au taux " + taux);
        assertEquals(htAttendu, ligne.get("montantHt").asLong(), "HT au taux " + taux);
        assertEquals(
            tvaAttendue,
            ligne.get("montantTtc").asLong() - ligne.get("montantHt").asLong(),
            "TVA au taux " + taux
        );
    }

    private JsonNode ligneDuTaux(JsonNode lignes, int taux) {
        for (JsonNode ligne : lignes) {
            if (ligne.get("codeTva").asInt() == taux) {
                return ligne;
            }
        }
        throw new AssertionError("aucune ligne au taux " + taux);
    }

    private JsonNode rapportTva() throws SQLException {
        return rapportTva("DECLARE");
    }

    private JsonNode rapportTva(String mode) throws SQLException {
        return appelJson(
            "SELECT sales_tva_report(DATE '%s', DATE '%s', ARRAY['CLOSED'], ARRAY['CA'], false, '%s')"
                .formatted(DATE_VENTE, DATE_VENTE, mode)
        );
    }

    private long totalTtc(JsonNode lignes) {
        long total = 0;
        for (JsonNode ligne : lignes) {
            total += ligne.get("montantTtc").asLong();
        }
        return total;
    }

    private JsonNode appelJson(String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next(), "la fonction n'a rien renvoyé");
            String json = rs.getString(1);
            assertNotNull(json, "la fonction a renvoyé NULL : le jeu d'essai n'a pas été vu");
            return MAPPER.readTree(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new AssertionError("JSON illisible", e);
        }
    }

    private BigDecimal diviseur(int taux) throws SQLException {
        return valeurDecimale("SELECT tva_divisor(%d)".formatted(taux));
    }

    private BigDecimal valeurDecimale(String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getBigDecimal(1);
        }
    }

    private static int valeurEntiere(String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}
