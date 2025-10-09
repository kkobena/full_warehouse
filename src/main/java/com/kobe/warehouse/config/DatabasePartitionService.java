package com.kobe.warehouse.config;

import java.time.LocalDate;
import java.time.Year;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabasePartitionService {

    private static final Logger LOG = LoggerFactory.getLogger(DatabasePartitionService.class);
    private final JdbcClient jdbcClient;

    public DatabasePartitionService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public void createMonthlyPartition(String parentTable, Year year) {
        String partitionName = parentTable + "_" + year;

        LocalDate from = year.atDay(1);
        LocalDate to = year.plusYears(1L).atDay(1);

        // Vérifie si la partition existe
        String existsSql =
            """
            SELECT EXISTS (
                SELECT 1
                FROM pg_inherits i
                JOIN pg_class c ON c.oid = i.inhrelid
                JOIN pg_class p ON p.oid = i.inhparent
                WHERE p.relname = :parentTable
                  AND c.relname = :partitionName
            )
            """;

        boolean partitioExist = jdbcClient
            .sql(existsSql)
            .param("parentTable", parentTable)
            .param("partitionName", partitionName)
            .query(Boolean.class)
            .single();

        if (!partitioExist) {
            String ddl = String.format(
                "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s');",
                partitionName,
                parentTable,
                from,
                to
            );
            jdbcClient.sql(ddl).update();

            LOG.info(String.format("Partition créée : %s [%s -> %s)%n", partitionName, from, to));
        } else {
            LOG.info(String.format("Partition déjà existante : %s%n", partitionName));
        }
    }
}
