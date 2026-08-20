package com.nerdc.elephantfence.backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
@Slf4j
public class DatabaseAutoCreator implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Environment env = beanFactory.getBean(Environment.class);
        String jdbcUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/elephant_fence");
        String username = env.getProperty("spring.datasource.username", "postgres");
        String password = env.getProperty("spring.datasource.password", "");

        if (!jdbcUrl.startsWith("jdbc:postgresql:")) {
            return;
        }

        try {
            // Extract host, port, and database name from jdbc:postgresql://localhost:5432/elephant_fence
            String cleanUrl = jdbcUrl.substring("jdbc:postgresql://".length());
            int slashIndex = cleanUrl.indexOf('/');
            if (slashIndex == -1) return;

            String hostAndPort = cleanUrl.substring(0, slashIndex);
            String dbName = cleanUrl.substring(slashIndex + 1);
            int paramIndex = dbName.indexOf('?');
            if (paramIndex != -1) {
                dbName = dbName.substring(0, paramIndex);
            }

            if (dbName.isBlank() || dbName.equals("postgres")) {
                return;
            }

            String systemDbUrl = "jdbc:postgresql://" + hostAndPort + "/postgres";

            log.info("Checking if PostgreSQL database '{}' exists on {}...", dbName, hostAndPort);
            try (Connection conn = DriverManager.getConnection(systemDbUrl, username, password);
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
                if (!rs.next()) {
                    log.info("Database '{}' does not exist. Auto-creating PostgreSQL database...", dbName);
                    stmt.executeUpdate("CREATE DATABASE " + dbName);
                    log.info("Database '{}' created successfully!", dbName);
                } else {
                    log.info("Database '{}' already exists.", dbName);
                }
            }
        } catch (Exception e) {
            log.warn("Auto-creation check for database '{}' completed: {}", jdbcUrl, e.getMessage());
        }
    }
}
