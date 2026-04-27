package com.medsync.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(60000);
            config.setMaxLifetime(1200000);
            config.setAutoCommit(true);
            config.setLeakDetectionThreshold(60000);
            config.setConnectionTestQuery("SELECT 1");

            // Add PostgreSQL-specific properties
            config.addDataSourceProperty("sslmode", "require");
            config.addDataSourceProperty("ApplicationName", "medsync-backend");
            config.addDataSourceProperty("tcpKeepAlives", "true");

            HikariDataSource hikariDataSource = new HikariDataSource(config);

            // Test the connection
            testConnection(hikariDataSource);

            System.out.println("✓ Database connection pool initialized successfully");
            logger.info("Database connection pool initialized successfully");
            return hikariDataSource;

        } catch (Exception e) {
            System.err.println("✗ Failed to initialize database connection pool");
            System.err.println("Error: " + e.getMessage());
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database connection failed. Please verify your database credentials and network connectivity.", e);
        }
    }

    private void testConnection(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
            System.out.println("✓ Database connection test successful");
            logger.info("Database connection test successful");
        } catch (Exception e) {
            System.err.println("✗ Database connection test failed");
            logger.error("Database connection test failed", e);
            throw new RuntimeException("Cannot establish connection to database. Check your credentials and Supabase database availability.", e);
        }
    }
}

