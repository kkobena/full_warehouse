package com.kobe.warehouse.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {
    private Environment environment;
    public FlywayConfig(Environment environment) {
        this.environment = environment;
    }
    @Bean
    @ConditionalOnProperty(
        name = "flyway.enabled",
        havingValue = "true"
    )
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .schemas(environment.getProperty("flyway.schemas"))
            // schema à utiliser
            .table(environment.getProperty("flyway.table"))            // table de versionnement
            .baselineOnMigrate(true)               // utile si la DB existe déjà
            .baselineDescription("initialize database")
            .locations("classpath:db/migration")  // emplacement des scripts
            .encoding("UTF-8")
            .load();

        // Lancer la migration au démarrage
        flyway.migrate();
        return flyway;
    }
}
