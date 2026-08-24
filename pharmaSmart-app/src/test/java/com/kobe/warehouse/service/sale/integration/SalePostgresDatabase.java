package com.kobe.warehouse.service.sale.integration;

import java.sql.Connection;
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
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Le PostgreSQL des tests d'intégration des ventes : un conteneur, migré par Flyway, sur lequel on
 * monte les vrais repositories Spring Data.
 *
 * <p>Le conteneur et le contexte JPA sont construits une seule fois pour toutes les classes de test
 * du paquet. Migrer le schéma coûte quatre secondes ; le refaire par classe multiplierait la note
 * sans rien prouver de plus, puisque chaque test s'exécute de toute façon dans une transaction
 * annulée à la fin (voir {@link AbstractSaleIntegrationTest}).
 *
 * <p>On ne démarre <em>pas</em> le contexte Spring Boot complet : sécurité, licence, Firebase et
 * imprimante n'ont rien à faire ici, et l'application n'a aucun test qui les bootstrappe. Ce qu'on
 * veut éprouver — le mapping Hibernate, les requêtes des repositories, le SQL natif, les
 * contraintes et le partitionnement — tient dans un {@code EntityManagerFactory} et les
 * repositories qui vont avec.
 */
final class SalePostgresDatabase {

    static final String SCHEMA = "pharma_smart";

    private static PostgreSQLContainer<?> postgres;
    private static AnnotationConfigApplicationContext context;

    private SalePostgresDatabase() {}

    static synchronized AnnotationConfigApplicationContext context() {
        if (context == null) {
            demarrer();
        }
        return context;
    }

    static <T> T bean(Class<T> type) {
        return context().getBean(type);
    }

    static PlatformTransactionManager transactionManager() {
        return bean(PlatformTransactionManager.class);
    }

    @SuppressWarnings("resource")
    private static void demarrer() {
        postgres = new PostgreSQLContainer<>("postgres:18-alpine").withDatabaseName("sale_integration_test");
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
        Runtime.getRuntime().addShutdownHook(new Thread(SalePostgresDatabase::fermer));
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
     * {@code sales}, {@code sales_line} et consorts sont partitionnées par date : sans partition
     * couvrant la date visée, l'insertion échoue au lieu de créer la ligne. Les services testés
     * datent leurs écritures de {@code LocalDate.now()} — et les copies d'annulation aussi — donc
     * on couvre l'année précédente, l'année en cours et la suivante.
     */
    private static void creerLesPartitions() {
        record Partitionnee(String table, String colonne) {}
        var tables = new Partitionnee[] {
            new Partitionnee("sales", "sale_date"),
            new Partitionnee("sales_line", "sale_date"),
            new Partitionnee("third_party_sale_line", "sale_date"),
            new Partitionnee("payment_transaction", "transaction_date"),
            new Partitionnee("inventory_transaction", "transaction_date"),
            new Partitionnee("facture_tiers_payant", "invoice_date"),
            new Partitionnee("invoice_payment_item", "transaction_date"),
            new Partitionnee("commande", "order_date"),
            new Partitionnee("order_line", "order_date"),
        };
        int annee = LocalDate.now().getYear();
        try (
            Connection connection = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
            );
            Statement st = connection.createStatement()
        ) {
            st.execute("SET search_path TO " + SCHEMA);
            for (int an = annee - 1; an <= annee + 1; an++) {
                for (Partitionnee t : tables) {
                    st.execute(
                        "CREATE TABLE IF NOT EXISTS %s_%d PARTITION OF %s FOR VALUES FROM ('%d-01-01') TO ('%d-01-01')".formatted(
                                t.table(),
                                an,
                                t.table(),
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
            factory.setPersistenceUnitName("pharmaSmartTest");
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
