package com.kobe.warehouse.config;

import java.time.Year;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FlywayConfig {

    private final ObjectProvider<DatabasePartitionService> partitionServiceProvider;
    private final Environment environment;

    public FlywayConfig(ObjectProvider<DatabasePartitionService> partitionServiceProvider, Environment environment) {
        this.partitionServiceProvider = partitionServiceProvider;
        this.environment = environment;
    }

    @Bean
    @ConditionalOnProperty(name = "flyway.enabled", havingValue = "true")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .schemas(environment.getProperty("flyway.schemas"))
            // schema à utiliser
            .table(environment.getProperty("flyway.table")) // table de versionnement
            .baselineOnMigrate(true) // utile si la DB existe déjà
            .baselineDescription("initialisation de la base de données")
            .locations("classpath:db/migration") // emplacement des scripts
            .encoding("UTF-8")
            .load();

        // Lancer la migration au démarrage
        flyway.migrate();
        DatabasePartitionService databasePartitionService = partitionServiceProvider.getIfAvailable();
        if (databasePartitionService != null) {
            Year current = Year.now();
            Year next = current.plusYears(1L);
            List.of(
                "sales",
                "sales_line",
                "third_party_sale_line",
                "commande",
                "order_line",
                "facture_tiers_payant",
                "inventory_transaction",
                "payment_transaction",
                "invoice_payment_item"
            ).forEach(table -> {
                databasePartitionService.createMonthlyPartition(table, current);
                databasePartitionService.createMonthlyPartition(table, next);
            });
        }
        return flyway;
    }
}
