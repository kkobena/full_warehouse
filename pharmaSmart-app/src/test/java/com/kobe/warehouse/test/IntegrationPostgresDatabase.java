package com.kobe.warehouse.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Properties;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.AvailableSettings;
import org.postgresql.Driver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Le PostgreSQL de tous les tests d'intégration : un conteneur unique, migré par Flyway, sur lequel
 * on monte les vrais repositories Spring Data.
 *
 * <p>Un seul conteneur pour l'ensemble des paquets d'intégration — ventes, stock, règlement,
 * facturation, inventaire, ajustement — et non un par domaine. Le montage était strictement
 * identique dans les six copies ; seuls le nom de la base et celui de l'unité de persistance
 * changeaient. Les multiplier ne séparait donc rien : chacune rejouait la même centaine de
 * migrations, et les démarrages concurrents se disputaient la machine au point de faire tomber au
 * hasard une classe sur son délai d'attente.
 *
 * <p>Aucun contexte Spring Boot complet ici : sécurité, licence et imprimante n'ont rien à y faire.
 * Ce que les tests d'intégration veulent éprouver — le SQL réellement envoyé, les identifiants
 * composites, le partitionnement par date, les contraintes et les vues — tient dans un
 * {@code EntityManagerFactory} et les repositories qui vont avec.
 *
 * <p><b>L'isolement entre tests repose entièrement sur l'annulation de transaction</b> que pose
 * chaque socle de paquet : rien n'est jamais validé, la base reste donc à l'état laissé par
 * Flyway. C'est ce qui rend le partage sûr, et c'est aussi ce qui l'exige — un test qui
 * committerait polluerait désormais les autres paquets, et non plus seulement le sien.
 */
public final class IntegrationPostgresDatabase {

    public static final String SCHEMA = "pharma_smart";

    private static PostgreSQLContainer<?> postgres;
    private static AnnotationConfigApplicationContext context;

    private IntegrationPostgresDatabase() {}

    public static synchronized AnnotationConfigApplicationContext context() {
        if (context == null) {
            demarrer();
        }
        return context;
    }

    public static <T> T bean(Class<T> type) {
        return context().getBean(type);
    }

    public static PlatformTransactionManager transactionManager() {
        return bean(PlatformTransactionManager.class);
    }

    @SuppressWarnings("resource")
    private static void demarrer() {
        postgres = new PostgreSQLContainer<>("postgres:18-alpine").withDatabaseName("pharma_smart_integration_test");
        postgres.start();

        Flyway
            .configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .schemas(SCHEMA)
            .defaultSchema(SCHEMA)
            .table("pharma_smart_history")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        creerLesPartitions();

        context = new AnnotationConfigApplicationContext(JpaTestConfiguration.class);
        Runtime.getRuntime().addShutdownHook(new Thread(IntegrationPostgresDatabase::fermer));
    }

    private static void fermer() {
        if (context != null) {
            context.close();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    /**
     * Les tables datées sont partitionnées : sans partition couvrant la date visée, l'insertion
     * échoue au lieu de créer la ligne. Les domaines qui écrivent dedans le font rarement au jour
     * même — un règlement s'antidate, une facture se prépare sur la période écoulée, une réception
     * se saisit sur une commande de l'an passé, un inventaire journalise à la date de comptage. On
     * couvre donc l'année précédente, l'année en cours et la suivante.
     */
    private static void creerLesPartitions() {
        var tables = new String[] {
            "sales",
            "sales_line",
            "third_party_sale_line",
            "payment_transaction",
            "inventory_transaction",
            "facture_tiers_payant",
            "invoice_payment_item",
            "commande",
            "order_line",
        };
        int annee = LocalDate.now().getYear();
        try (
            Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
            Statement st = connection.createStatement()
        ) {
            st.execute("SET search_path TO " + SCHEMA);
            for (int an = annee - 1; an <= annee + 1; an++) {
                for (String table : tables) {
                    st.execute(
                        "CREATE TABLE IF NOT EXISTS %s_%d PARTITION OF %s FOR VALUES FROM ('%d-01-01') TO ('%d-01-01')".formatted(
                                table,
                                an,
                                table,
                                an,
                                an + 1
                            )
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("création des partitions impossible", e);
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.kobe.warehouse.repository")
    @EnableTransactionManagement
    static class JpaTestConfiguration {

        @Bean
        DataSource dataSource() {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(Driver.class);
            String url = postgres.getJdbcUrl();
            // L'URL de Testcontainers porte déjà des paramètres : un second « ? » la rendrait invalide.
            dataSource.setUrl(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
            dataSource.setUsername(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
            return dataSource;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.kobe.warehouse.domain");
            factory.setPersistenceUnitName("pharmaSmartIntegrationTest");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

            Properties properties = new Properties();
            properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
            properties.put(AvailableSettings.DEFAULT_SCHEMA, SCHEMA);
            properties.put(AvailableSettings.HBM2DDL_AUTO, "none");
            properties.put(AvailableSettings.JDBC_TIME_ZONE, "UTC");
            properties.put(AvailableSettings.TIMEZONE_DEFAULT_STORAGE, "NORMALIZE");
            properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "50");
            properties.put(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "100");
            // Le cache de second niveau de la production masquerait ici les écritures réellement
            // envoyées à Postgres : c'est précisément ce qu'on veut observer.
            properties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, "false");
            properties.put(AvailableSettings.USE_QUERY_CACHE, "false");
            properties.put(
                AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
            );
            properties.put(
                AvailableSettings.IMPLICIT_NAMING_STRATEGY,
                "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl"
            );
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }
    }
}
