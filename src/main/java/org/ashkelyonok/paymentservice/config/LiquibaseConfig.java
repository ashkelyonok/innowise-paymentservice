package org.ashkelyonok.paymentservice.config;

import jakarta.annotation.PostConstruct;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.paymentservice.exception.LiquibaseMigrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Slf4j
@Configuration
public class LiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.liquibase.change-log}")
    private String changeLogPath;

    @PostConstruct
    public void migrate() {
        log.info("Starting Liquibase MongoDB migrations using changelog: {}", changeLogPath);

        String cleanPath = changeLogPath.replace("classpath:", "");

        try (ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor()) {

            runUpdate(cleanPath, resourceAccessor);

            log.info("Liquibase MongoDB migrations completed successfully.");

        } catch (Exception e) {
            log.error("Liquibase migration failed critical check: {}", e.getMessage());
            throw new LiquibaseMigrationException("Could not initialize Liquibase resources", e);
        }
    }

    private void runUpdate(String cleanPath, ClassLoaderResourceAccessor resourceAccessor) throws LiquibaseException, SQLException {
        try (Database database = DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, resourceAccessor)) {

            try (Liquibase liquibase = new Liquibase(cleanPath, resourceAccessor, database)) {
                liquibase.update("");
            } catch (LiquibaseException e) {
                log.error("Error during execution of Liquibase update: {}", e.getMessage());
                throw new LiquibaseMigrationException("Failed to update MongoDB schema", e);
            }
        }
    }
}